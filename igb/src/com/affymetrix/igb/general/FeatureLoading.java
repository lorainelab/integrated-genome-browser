package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.OKAction;
import com.affymetrix.igb.action.ReportBugAction;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;

/**
 *
 * @version $Id$
 */
public final class FeatureLoading {

	private static final boolean DEBUG = false;

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
			for (Entry<String,String> type : version.getTypes().entrySet()) {
				String type_name = type.getKey();
				if (type_name == null || type_name.length() == 0) {
					System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
					continue;
				}
				gVersion.addFeature(new GenericFeature(type_name, null, gVersion, null, type.getValue(), autoload));
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
				if (typeNames == null) {
					String errorText = MessageFormat.format(IGBConstants.BUNDLE.getString("quickloadGenomeError"), gVersion.gServer.serverName, gVersion.group.getOrganism(), gVersion.versionName);
					List<GenericAction> actions = new ArrayList<GenericAction>();
					actions.add(OKAction.getAction());
					actions.add(ReportBugAction.getAction());
					ErrorHandler.errorPanel((JFrame) null, gVersion.gServer.serverName, errorText, new ArrayList<Throwable>(), actions);
					return;
				}
				String organism_dir = quickloadServer.getOrganismDir(gVersion.versionName);
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
							type_name, type_props, gVersion, new QuickLoad(gVersion, type_name, organism_dir), null, autoload));
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

}
