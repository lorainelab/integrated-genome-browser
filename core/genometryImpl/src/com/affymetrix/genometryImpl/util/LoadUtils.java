package com.affymetrix.genometryImpl.util;

public final class LoadUtils {
	public static enum LoadStrategy {
		NO_LOAD ("Don't Load"),
		AUTOLOAD ("Auto"),
		VISIBLE ("Manual"),
		//CHROMOSOME ("Chromosome"),
		GENOME ("Genome");
		
		private String name;

		LoadStrategy(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	public static enum LoadStatus {
		UNLOADED ("Unloaded"),
		LOADING ("Loading"),
		LOADED ("Loaded");

		private String name;

		LoadStatus(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	public static enum ServerStatus {
		NotInitialized ("Not initialized"),
		Initialized ("Initialized"),
		NotResponding ("Not responding");

		private String name;

		ServerStatus(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	public static enum RefreshStatus {
		NOT_REFRESHED ("Feature not refeshed yet."),
		NO_DATA_LOADED ("No data found in visible region."),
		NO_SEQ_PRESENT ("Current sequence is not present on feature."),
		NO_NEW_DATA_LOADED ("All data in visible region is already loaded."),
		DATA_LOADED ("Data Loaded");
	
		private String message;
		
		RefreshStatus(String message){
			this.message = message;
		}
		
		@Override
		public String toString(){
			return message;
		}
	};
	
	/**
	 * Used to give a friendly name for QuickLoad features.
	 * @param name
	 * @return name
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
