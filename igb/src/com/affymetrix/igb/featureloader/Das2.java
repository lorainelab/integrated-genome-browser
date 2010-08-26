package com.affymetrix.igb.featureloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.das2.Das2ClientOptimizer;
import com.affymetrix.genometryImpl.das2.Das2FeatureRequestSym;
import com.affymetrix.genometryImpl.das2.Das2Region;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.general.FeatureRequestSym;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;

/**
 *
 * @author jnicol
 */
public class Das2 {

	/**
	 * Loads (and displays) DAS/2 annotations.
	 * This is done in a multi-threaded fashion so that the UI doesn't lock up.
	 * @param selected_seq
	 * @param gFeature
	 * @param gviewer
	 * @param overlap
	 * @return true or false
	 */
	public static boolean loadFeatures(SeqSpan overlap, GenericFeature gFeature) {
		Das2VersionedSource version = (Das2VersionedSource) gFeature.gVersion.versionSourceObj;
		Das2Type dType = (Das2Type) gFeature.typeObj;

		Das2Region region = version.getSegment(overlap.getBioSeq());

		if (dType != null && region != null) {
			Das2FeatureRequestSym request_sym = new Das2FeatureRequestSym(dType, region, overlap);
			processFeatureRequest(request_sym, gFeature, true);
		}
		return true;
	}

	/**
	 *  Want to put loading of DAS/2 annotations on separate thread(s) (since processFeatureRequests() call is most
	 *     likely being run on event thread)
	 *  Also don't want to overwhelm a DAS/2 server with nearly simultaneous calls from separate threads
	 *  But also don't want to slow down display of annotations from faster DAS/2 servers due to another slower server
	 *  Compromise is to have separate threads for each Das2VersionedSource
	 *  split requests into sets of requests, one set per Das2VersionedSource the request is being made to
	 *  Then for each set of requests spawn a SwingWorker thread, with serial processing of each request in the set
	 *     and finishing with a gviewer.setAnnotatedSeq() call on the event thread to revise main view to show new annotations
	 *
	 *
	 * @param request - FeatureRequestSym on this GenericFeature
 	 * @param feature
	 * @param update_display - whether to update the display or not
	 */
	public static void processFeatureRequest(
					final Das2FeatureRequestSym request,
					final GenericFeature feature,
					final boolean update_display) {
		if (request == null) {
			Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName);
			return;
		}
		final List<FeatureRequestSym> result_syms = new ArrayList<FeatureRequestSym>();

		Das2Type dtype = request.getDas2Type();
		Das2VersionedSource version = dtype.getVersionedSource();

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				try {
					createDAS2ResultSyms(feature, request, result_syms);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}

			@Override
			public void done() {
				final SeqMapView gviewer = Application.getSingleton().getMapView();
				if (update_display && gviewer != null && !result_syms.isEmpty()) {
					BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
					TrackView.updateDependentData();
					gviewer.setAnnotatedSeq(aseq, true, true);
				}
				Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName);
			}
		};

		ThreadUtils.getPrimaryExecutor(version).execute(worker);
	}

	private static void createDAS2ResultSyms(
			final GenericFeature feature, final Das2FeatureRequestSym request_sym, final List<FeatureRequestSym> result_syms) {
		// Create an AnnotStyle so that we can automatically set the
		// human-readable name to the DAS2 name, rather than the ID, which is a URI
		Das2Type type = request_sym.getDas2Type();
		ITrackStyle ts = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type.getID(), type.getName());
		ts.setFeature(feature);

		try {
			Application.getSingleton().addNotLockedUpMsg("Loading " + type.getName());
			List<? extends FeatureRequestSym> feature_list = Das2ClientOptimizer.loadFeatures(request_sym);
			result_syms.addAll(feature_list);
		} finally {
			Application.getSingleton().removeNotLockedUpMsg("Loading " + type.getName());
		}
	}

}
