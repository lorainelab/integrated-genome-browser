package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.igb.Application;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das.DasType;
import com.affymetrix.genometryImpl.das2.Das2ClientOptimizer;
import com.affymetrix.genometryImpl.das2.Das2FeatureRequestSym;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.general.FeatureRequestSym;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.symloader.QuickLoad;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.swing.SwingWorker;

public final class FeatureLoading {

	private static final boolean DEBUG = false;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();


	/**
	 * Load the annotations for the given version.  This is specific to one server.
	 * @param gVersion
	 */
	public static void loadFeatureNames(final GenericVersion gVersion) {
		boolean autoload = PreferenceUtils.getBooleanParam(
						PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
		if (!gVersion.getFeatures().isEmpty()) {
			if (DEBUG) {
				System.out.println("Feature names are already loaded.");
			}
			return;
		}

		if (gVersion.gServer.serverType == ServerType.DAS2) {
			if (DEBUG) {
				System.out.println("Discovering DAS2 features for " + gVersion.versionName);
			}
			// Discover features from DAS/2
			Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;
			for (Das2Type type : version.getTypes().values()) {
				String type_name = type.getName();
				if (type_name == null || type_name.length() == 0) {
					System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
					continue;
				}
				Map<String, String> type_props = type.getProps();
				gVersion.addFeature(new GenericFeature(type_name, type_props, gVersion, null, type, autoload));
			}
			return;
		}
		if (gVersion.gServer.serverType == ServerType.DAS) {
			// Discover features from DAS
			if (DEBUG) {
				System.out.println("Discovering DAS1 features for " + gVersion.versionName);
			}
			DasSource version = (DasSource) gVersion.versionSourceObj;
			for (DasType type : version.getTypes()) {
				String type_name = type.getName();
				if (type_name == null || type_name.length() == 0) {
					System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
					continue;
				}
				gVersion.addFeature(new GenericFeature(type_name, null, gVersion, null, type, autoload));
			}
			return;
		}
		if (gVersion.gServer.serverType == ServerType.QuickLoad) {
			// Discover feature names from QuickLoad

			try {
				URL quickloadURL = new URL((String) gVersion.gServer.serverObj);
				if (DEBUG) {
					System.out.println("Discovering Quickload features for " + gVersion.versionName + ". URL:" + (String) gVersion.gServer.serverObj);
				}

				QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
				List<String> typeNames = quickloadServer.getTypes(gVersion.versionName);
				for (String type_name : typeNames) {
					if (type_name == null || type_name.length() == 0) {
						System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
						continue;
					}
					if (DEBUG) {
						System.out.println("Adding feature " + type_name);
					}
					Map<String, String> type_props = quickloadServer.getProps(gVersion.versionName, type_name);
					gVersion.addFeature(
							new GenericFeature(
							type_name, type_props, gVersion, new QuickLoad(gVersion, type_name), null, autoload));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return;
		}
		if (gVersion.gServer.serverType == ServerType.LocalFiles) {
			// no features.
			return;
		}

		System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverType);
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
	 * @param requests
	 * @param update_display
	 * @param gmodel
	 * @param gviewer
	 */
	public static void processDas2FeatureRequests(
					List<Das2FeatureRequestSym> requests,
					final String feature_name,
					final boolean update_display,
					final GenometryModel gmodel) {
		if ((requests == null) || (requests.size() == 0)) {
			Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature_name);
			return;
		}
		final List<FeatureRequestSym> result_syms = new ArrayList<FeatureRequestSym>();

		Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> requests_by_version = splitDAS2RequestsByVersion(requests);
		final SeqMapView gviewer = Application.getSingleton().getMapView();

		for (Map.Entry<Das2VersionedSource, Set<Das2FeatureRequestSym>> entry : requests_by_version.entrySet()) {
			Das2VersionedSource version = entry.getKey();
			Executor vexec = ThreadUtils.getPrimaryExecutor(version);
			final Set<Das2FeatureRequestSym> request_set = entry.getValue();

			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				public Void doInBackground() {
					try {
					createDAS2ResultSyms(request_set, result_syms);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					return null;
				}

				@Override
				public void done() {
					if (update_display && gviewer != null && !result_syms.isEmpty()) {
						BioSeq aseq = gmodel.getSelectedSeq();
						Application.getSingleton().getMapView().updateDependentData();
						gviewer.setAnnotatedSeq(aseq, true, true);
					}
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature_name);
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

	private static final void createDAS2ResultSyms(final Set<Das2FeatureRequestSym> request_set, final List<FeatureRequestSym> result_syms) {
		for (Das2FeatureRequestSym request_sym : request_set) {
			// Create an AnnotStyle so that we can automatically set the
			// human-readable name to the DAS2 name, rather than the ID, which is a URI
			Das2Type type = request_sym.getDas2Type();
			if (DEBUG) {
				System.out.println("$$$$$ in processFeatureRequests(), getting style for: " + type.getName());
			}
			IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type.getID());
			style.setHumanName(type.getName());
			Application.getSingleton().addNotLockedUpMsg("Loading " + type.getShortName());
			List<? extends FeatureRequestSym> feature_list = Das2ClientOptimizer.loadFeatures(request_sym);
			result_syms.addAll(feature_list);
			Application.getSingleton().removeNotLockedUpMsg("Loading " + type.getShortName());
		}
	}

}
