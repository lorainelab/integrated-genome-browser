package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
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
		else {
			gVersion.gServer.serverType.discoverFeatures(gVersion, autoload);
		}
	}

}
