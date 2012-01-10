package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

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

		if (gVersion.gServer.serverType == null) {
			System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverType);
		}
		else if (gVersion.gServer.serverType == ServerTypeI.QuickLoad) {
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
					ErrorHandler.errorPanelWithReportBug(gVersion.gServer.serverName, errorText);
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
		}
		else {
			gVersion.gServer.serverType.discoverFeatures(gVersion, autoload);
		}
	}

}
