package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsParser.AnnotMapElt;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasSource;
import com.affymetrix.igb.das.DasType;
import com.affymetrix.igb.das2.Das2ClientOptimizer;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.das2.Das2Type;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.menuitem.OpenGraphAction;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
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
	private static final SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

	/**
	 * Load annotation names for the given version name (across multiple servers).
	 * The internal call is threaded to keep from locking up the GUI.
	 * @param versionSet
	 * @return
	 */
	public static boolean loadFeatureNames(Set<GenericVersion> versionSet) {
		for (final GenericVersion gVersion : versionSet) {
			loadFeatureNames(gVersion);
		}
		return true;
	}

	/**
	 * Load the annotations for the given version.  This is specific to one server.
	 * @param gVersion
	 */
	private static synchronized void loadFeatureNames(final GenericVersion gVersion) {
		if (!gVersion.features.isEmpty()) {
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
				gVersion.features.add(new GenericFeature(type_name, type_props, gVersion));
			}
			return;
		}
		if (gVersion.gServer.serverType == ServerType.DAS) {
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
				gVersion.features.add(new GenericFeature(type_name, null, gVersion));
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

				QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
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
					gVersion.features.add(new GenericFeature(type_name, type_props, gVersion));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return;
		}
		if (gVersion.gServer.serverType == ServerType.Unknown) {
			// no features.  This was an unknown type.
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
					final boolean update_display,
					final SingletonGenometryModel gmodel,
					final SeqMapView gviewer) {
		if ((requests == null) || (requests.size() == 0)) {
			return;
		}
		final List<Das2FeatureRequestSym> result_syms = new ArrayList<Das2FeatureRequestSym>();

		Map<Das2VersionedSource, Set<Das2FeatureRequestSym>> requests_by_version = splitDAS2RequestsByVersion(requests);

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
					if (update_display && gviewer != null) {
						MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
						gviewer.setAnnotatedSeq(aseq, true, true);
					}
					Application.getSingleton().setStatus("", false);
				}
			};

			vexec.execute(worker);
		}
		//for some reason this doesn't always get called
		Application.getSingleton().setStatus("", false);
	}


	/**
	 * split into entries by DAS/2 versioned source
	 * @param requests
	 * @return
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

	private static final void createDAS2ResultSyms(final Set<Das2FeatureRequestSym> request_set, final List<Das2FeatureRequestSym> result_syms) {
		for (Das2FeatureRequestSym request_sym : request_set) {
			// Create an AnnotStyle so that we can automatically set the
			// human-readable name to the DAS2 name, rather than the ID, which is a URI
			Das2Type type = request_sym.getDas2Type();
			if (DEBUG) {
				System.out.println("$$$$$ in processFeatureRequests(), getting style for: " + type.getName());
			}
			IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type.getID());
			style.setHumanName(type.getName());
			Application.getSingleton().setNotLockedUpStatus("Loading " + type.getShortName());
			List<Das2FeatureRequestSym> feature_list = Das2ClientOptimizer.loadFeatures(request_sym);
			result_syms.addAll(feature_list);
		}
	}

	public static boolean loadQuickLoadAnnotations(final GenericFeature gFeature) throws OutOfMemoryError {
		final String fileName = determineQuickLoadFileName(gFeature);
		if (fileName.length() == 0) {
			return false;
		}

		Executor vexec = ThreadUtils.getPrimaryExecutor(gFeature.gVersion.gServer);

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				try {
				loadQuickLoadFeature(fileName, gFeature);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
			@Override
			public void done() {
				Application.getSingleton().setStatus("",false);
			}
		};

		vexec.execute(worker);
		return true;

	}

	private static String determineQuickLoadFileName(final GenericFeature gFeature) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) gFeature.gVersion.gServer.serverObj);
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return "";
		}

		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
		List<AnnotMapElt> annotsList = quickloadServer.getAnnotsMap(gFeature.gVersion.versionID);
		
		// Linear search, but over a very small list.
		for (AnnotMapElt annotMapElt : annotsList) {
			if (annotMapElt.title.equals(gFeature.featureName)) {
				return annotMapElt.fileName;
			}
		}
		return "";
	}

	private static boolean loadQuickLoadFeature(final String fileName, GenericFeature gFeature) throws OutOfMemoryError {
		InputStream istr = null;
		BufferedInputStream bis = null;
		final String annot_url = gFeature.gVersion.gServer.URL + gFeature.gVersion.versionID + "/" + fileName;

		if (DEBUG) {
			System.out.println("need to load: " + annot_url);
		}
		try {
			istr = LocalUrlCacher.getInputStream(annot_url, true);
			if (istr != null) {
				IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(fileName);
				style.setHumanName(gFeature.featureName);
				if (GraphSymUtils.isAGraphFilename(fileName)) {
					URL url = new URL(annot_url);
					List<GraphSym> graphs = OpenGraphAction.loadGraphFile(url, gmodel.getSelectedSeqGroup(), gmodel.getSelectedSeq());
					if (graphs != null) {
						// Reset the selected Seq Group to make sure that the DataLoadView knows
						// about any new chromosomes that were added.
						gmodel.setSelectedSeqGroup(gmodel.getSelectedSeqGroup());
					}
				} else {
					bis = new BufferedInputStream(istr);
					LoadFileAction.load(Application.getSingleton().getFrame(), bis, fileName, gmodel, gmodel.getSelectedSeqGroup(), gmodel.getSelectedSeq());
				}
				return true;
			}
		} catch (Exception ex) {
			System.out.println("Problem loading requested url:" + annot_url);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(istr);
		}
		return false;
	}


}
