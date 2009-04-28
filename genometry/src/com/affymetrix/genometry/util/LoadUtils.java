package com.affymetrix.genometry.util;

public final class LoadUtils {
	public static enum LoadStrategy {

		NO_LOAD, VISIBLE, WHOLE
	};

	public static enum LoadStatus {

		UNLOADED, LOADING, LOADED
	};

	public static enum ServerType {

		DAS, DAS2, QuickLoad, Unknown
	};

	/**
	 * Used to give a friendly name for QuickLoad features.
	 * @param name
	 * @return
	 */
	public static String stripFilenameExtensions(final String name) {
		// Remove ending .gz or .zip extension.
		if (name.endsWith(".gz")) {
			return stripFilenameExtensions(name.substring(0, name.length() -3));
		}
		if (name.endsWith(".zip")) {
			return stripFilenameExtensions(name.substring(0, name.length() -4));
		}

		if (name.indexOf('.') > 0) {
			return name.substring(0, name.lastIndexOf('.'));
		}
		return name;
	}
}
