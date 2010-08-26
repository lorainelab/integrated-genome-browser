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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
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
		Das2VersionedSource version = (Das2VersionedSource)gFeature.gVersion.versionSourceObj;
		List<Das2Type> type_list = version.getTypesByName(gFeature.featureName);
		Das2Region region = version.getSegment(overlap.getBioSeq());

		List<Das2FeatureRequestSym> requests = new ArrayList<Das2FeatureRequestSym>();
		for (Das2Type dtype : type_list) {
			if (dtype != null && region != null) {
				Das2FeatureRequestSym request_sym = new Das2FeatureRequestSym(dtype, region, overlap);
				requests.add(request_sym);
			}
		}

		Das2.processFeatureRequests(requests, gFeature, true);
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
	 * @param requests - FeatureRequestSyms on this GenericFeature
 	 * @param feature
	 * @param update_display - whether to update the display or not
	 */
	public static void processFeatureRequests(
					List<Das2FeatureRequestSym> requests,
					final GenericFeature feature,
					final boolean update_display) {
		if ((requests == null) || (requests.isEmpty())) {
			Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName);
			return;
		}
		final List<FeatureRequestSym> result_syms = new ArrayList<FeatureRequestSym>();

		Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> requests_by_version = splitDAS2RequestsByVersion(requests);

		for (Map.Entry<Das2VersionedSource, Set<Das2FeatureRequestSym>> entry : requests_by_version.entrySet()) {
			Das2VersionedSource version = entry.getKey();
			Executor vexec = ThreadUtils.getPrimaryExecutor(version);
			final Set<Das2FeatureRequestSym> request_set = entry.getValue();

			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				public Void doInBackground() {
					try {
						createDAS2ResultSyms(feature, request_set, result_syms);
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

			vexec.execute(worker);
		}
	}

	/**
	 * split into entries by DAS/2 versioned source
	 * @param requests
	 * @return requests_by_version
	 */
	private static Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> splitDAS2RequestsByVersion(List<Das2FeatureRequestSym> requests) {
		Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> requests_by_version =
						new LinkedHashMap<Das2VersionedSource, Set<Das2FeatureRequestSym>>();
		for (Das2FeatureRequestSym request : requests) {
			Das2Type dtype = request.getDas2Type();
			Das2VersionedSource version = dtype.getVersionedSource();
			Set<Das2FeatureRequestSym> rset = requests_by_version.get(version);
			if (rset == null) {
				// Using Set instead of List here guarantees only one request per type, even if version (and therefore type) shows up
				//    in multiple branches of DAS/2 server/source/version/type tree.
				rset = new LinkedHashSet<Das2FeatureRequestSym>();
				requests_by_version.put(version, rset);
			}
			rset.add(request);
		}
		return requests_by_version;
	}

	private static void createDAS2ResultSyms(
			final GenericFeature feature, final Set<Das2FeatureRequestSym> request_set, final List<FeatureRequestSym> result_syms) {
		for (Das2FeatureRequestSym request_sym : request_set) {
			// Create an AnnotStyle so that we can automatically set the
			// human-readable name to the DAS2 name, rather than the ID, which is a URI
			Das2Type type = request_sym.getDas2Type();
			ITrackStyle ts = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type.getID(), type.getName());
			ts.setFeature(feature);

			try {
				Application.getSingleton().addNotLockedUpMsg("Loading " + type.getShortName());
				List<? extends FeatureRequestSym> feature_list = Das2ClientOptimizer.loadFeatures(request_sym);
				result_syms.addAll(feature_list);
			} finally {
				Application.getSingleton().removeNotLockedUpMsg("Loading " + type.getShortName());
			}
		}
	}

}
