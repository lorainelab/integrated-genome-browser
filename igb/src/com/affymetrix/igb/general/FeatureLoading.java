package com.affymetrix.igb.general;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das.DasSource;
import com.affymetrix.igb.das.DasType;
import com.affymetrix.igb.das2.Das2ClientOptimizer;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2Type;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.swing.threads.SwingWorker;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public final class FeatureLoading {

	private static final boolean DEBUG = false;
	private static final SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

	/**
	 * Load annotation names for the given version name (across multiple servers).
	 * The internal call is threaded to keep from locking up the GUI.
	 * @param versionName
	 * @return
	 */
	public static boolean loadFeatureNames(Set<GenericVersion> versionSet) {
		for (final GenericVersion gVersion : versionSet) {
			// We use a thread to get the servers.  (Otherwise the user may see a lockup of their UI.)
			try {
				Runnable r = new Runnable() {
					public void run() {
						loadFeatureNames(gVersion);
					}
				};
				Thread thr1 = new Thread(r);
				thr1.start();
				while (thr1.isAlive()) {
					Thread.sleep(200);
				}
			} catch (InterruptedException ie) {
				System.out.println("Interruption while getting feature list.");
			}
		}
		return true;
	}

	/**
	 * Load the annotations for the given version.  This is specific to one server.
	 * @param gVersion
	 */
	private static synchronized void loadFeatureNames(final GenericVersion gVersion) {
		if (gVersion.features.size() > 0) {
			System.out.println("Feature names are already loaded.");
			return;
		}

		if (gVersion.gServer.serverClass == Das2ServerInfo.class) {
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
				gVersion.features.add(new GenericFeature(type_name, gVersion));
			}
			return;
		}
		if (gVersion.gServer.serverClass == DasServerInfo.class) {
			// Discover features from DAS
			if (DEBUG) {
				System.out.println("Discovering DAS1 features for " + gVersion.versionName);
			}
			DasSource version = (DasSource) gVersion.versionSourceObj;
			for (DasType type : version.getTypes().values()) {
				String type_name = type.getID();
				if (type_name == null || type_name.length() == 0) {
					System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
					continue;
				}
				gVersion.features.add(new GenericFeature(type_name, gVersion));
			}
			return;
		}
		if (gVersion.gServer.serverClass == QuickLoadServerModel.class) {
			// Discover feature names from QuickLoad

			try {
				URL quickloadURL = new URL((String) gVersion.gServer.serverObj);
				if (DEBUG) {
				System.out.println("Discovering Quickload features for " + gVersion.versionName + ". URL:" + (String)gVersion.gServer.serverObj);
			}

				QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
				List<String> featureNames = quickloadServer.getFilenames(gVersion.versionName);
				for (String featureName : featureNames) {
					if (featureName == null || featureName.length() == 0) {
						System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
						continue;
					}
					if (DEBUG) {
						System.out.println("Adding feature " + featureName);
					}
					gVersion.features.add(new GenericFeature(featureName, gVersion));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return;
		}

		System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverClass);
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
	 */
	public static void processDas2FeatureRequests(
					List<Das2FeatureRequestSym> requests,
					final boolean update_display,
					boolean thread_requests,
					final SingletonGenometryModel gmodel,
					final SeqMapView gviewer) {
		if ((requests == null) || (requests.size() == 0)) {
			return;
		}
		final List<Das2FeatureRequestSym> result_syms = new ArrayList<Das2FeatureRequestSym>();

		Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> requests_by_version = splitRequestsByVersion(requests);

		for (Map.Entry<Das2VersionedSource, Set<Das2FeatureRequestSym>> entry : requests_by_version.entrySet()) {
			Das2VersionedSource version = entry.getKey();
			Executor vexec = ThreadUtils.getPrimaryExecutor(version);
			final Set<Das2FeatureRequestSym> request_set = entry.getValue();

			SwingWorker worker = new SwingWorker() {

				public Object construct() {
					createResultSyms(request_set, result_syms);
					return null;
				}

				@Override
				public void finished() {
					if (update_display && gviewer != null) {
						MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
						gviewer.setAnnotatedSeq(aseq, true, true);
					}
					Application.getSingleton().setStatus("", false);
				}
			};

			if (thread_requests) {
				//	worker.start();
				vexec.execute(worker);
			} else {
				// if not threaded, then want to execute code in above subclass of SwingWorker, but within this thread
				//   so just ignore the thread features of SwingWorker and call construct() and finished() directly to
				//   to execute in this thread
				try {
					worker.construct();
					worker.finished();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		//for some reason this doesn't always get called
		Application.getSingleton().setStatus("", false);
	}

	private static Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> splitRequestsByVersion(List<Das2FeatureRequestSym> requests) {
		Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> requests_by_version =
						new LinkedHashMap<Das2VersionedSource, Set<Das2FeatureRequestSym>>();
		// split into entries by DAS/2 versioned source

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

	private static final void createResultSyms(final Set<Das2FeatureRequestSym> request_set, final List<Das2FeatureRequestSym> result_syms) {
		for (Das2FeatureRequestSym request_sym : request_set) {
			// Create an AnnotStyle so that we can automatically set the
			// human-readable name to the DAS2 name, rather than the ID, which is a URI
			Das2Type type = request_sym.getDas2Type();
			if (DEBUG) {
				System.out.println("$$$$$ in processFeatureRequests(), getting style for: " + type.getName());
			}
			IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type.getID());
			style.setHumanName(type.getName());
			Application.getSingleton().setStatus("Loading " + type.getShortName(), false);
			List<Das2FeatureRequestSym> feature_list = Das2ClientOptimizer.loadFeatures(request_sym);
			result_syms.addAll(feature_list);
		}
	}
}
