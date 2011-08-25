package com.affymetrix.common;

import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * utilities used by both the main, starting class, and the
 * bundles. Singleton pattern.
 *
 */
public class CommonUtils {
	private static final CommonUtils instance = new CommonUtils();
	private String app_dir = null;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("common");

	private static final String APP_NAME         = BUNDLE.getString("appName");
	private static final String APP_NAME_SHORT   = BUNDLE.getString("appNameShort");
	private static final String APP_VERSION      = BUNDLE.getString("appVersion");
	private static final String APP_VERSION_FULL = MessageFormat.format(
			BUNDLE.getString("appVersionFull"),
			APP_VERSION);
	private BundleContext bundleContext;

	private CommonUtils() {
		super();
	}

	public final static CommonUtils getInstance() {
		return instance;
	}

	void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public String getAppName() {
		return APP_NAME;
	}

	public String getAppNameShort() {
		return APP_NAME_SHORT;
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
	 * Returns the value of the argument indicated by label.
	 * If arguments are
	 *   "-flag_2 -foo bar", then get_arg("foo", args)
	 * returns "bar", get_arg("flag_2") returns a non-null string,
	 * and get_arg("flag_5") returns null.
	 */
	public String getArg(String label, String[] args) {
		String to_return = null;
		boolean got_it = false;
		if (label != null && args != null) {
			for (String item : args) {
				if (got_it) {
					to_return = item;
					break;
				}
				if (item.equals(label)) {
					got_it = true;
				}
			}
		}
		if (got_it && to_return == null) {
			to_return = "true";
		}
		return to_return;
	}

	public boolean installBundle(String filePath) {
		Bundle bundle = null;
		if (filePath != null) {
			try {
				bundle = bundleContext.installBundle(filePath);
			}
			catch(Exception x) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "error installing bundle", x);
				bundle = null;
			}
			if (bundle != null) {
				Logger.getLogger(getClass().getName()).log(Level.INFO, "installed bundle: {0}", filePath);
			}
		}
		return bundle != null;
	}

	public boolean uninstallBundle(String symbolicName) {
		boolean found = false;
		if (symbolicName != null) {
			for (Bundle bundle : bundleContext.getBundles()) {
				if (symbolicName.equals(bundle.getSymbolicName())) {
					try {
						bundle.uninstall();
						Logger.getLogger(getClass().getName()).log(Level.INFO, "uninstalled bundle: {0}", symbolicName);
						found = true;
					}
					catch(Exception x) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "error uninstalling bundle", x);
						found = false;
					}
				}
			}
		}
		return found;
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
