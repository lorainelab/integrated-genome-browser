package com.affymetrix.common;

import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * utilities used by both the main, starting class, and the
 * bundles. Singleton pattern.
 *
 */
public class CommonUtils {
	private static final CommonUtils instance = new CommonUtils();
	private String app_dir = null;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("common");

	private static final String APP_VERSION      = BUNDLE.getString("appVersion");
	private static final String APP_VERSION_FULL = MessageFormat.format(
			BUNDLE.getString("appVersionFull"),
			APP_VERSION);

	private CommonUtils() {
		super();
	}

	public final static CommonUtils getInstance() {
		return instance;
	}

	/**
	 * get the current version of IGB
	 * @return the IGB version
	 */
	public String getAppVersion() {
		return APP_VERSION;
	}

	/**
	 * get the current full (detailed) version of IGB
	 * @return the full IGB version
	 */
	public String getAppVersionFull() {
		return APP_VERSION_FULL;
	}

	/**
	 * Returns the location of the application data directory.
	 * The String will always end with "/".
	 *
	 * @return the application directory
	 */
	public String getAppDataDirectory() {
		if (app_dir == null) {
			String home = System.getProperty("user.home");
			String app_data = home + "/Application Data";
			File app_data_dir = new File(app_data);
			if (app_data_dir.exists() && app_data_dir.isDirectory()) {
				app_dir = app_data + "/IGB/";
			} else {
				app_dir = home + "/.igb/";
			}
		}
		if (!app_dir.endsWith("/")) {
			app_dir = app_dir + "/";
		}
		return app_dir;
	}
}
