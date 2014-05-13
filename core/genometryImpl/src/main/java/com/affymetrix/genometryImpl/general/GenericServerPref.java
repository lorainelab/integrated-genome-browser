package com.affymetrix.genometryImpl.general;

/**
 * This class contains constant values that are used to stored preferences
 * for the generic server.
 *
 * 
 * @author jfvillal
 */
interface GenericServerPref {
	/**
	 * Key to retrieve the value for the server name
	 */
	public static final String NAME = "name";
	/**
	 * Key to retrieve  ??
	 */
	public static final String ENABLED = "enabled";
	/**
	 * Key to retrieve the login name
	 */
	public static final String LOGIN = "login";
	/**
	 * Key to retrieve the password
	 */
	public static final String PASSWORD = "password";
	/**
	 * Key to retrieve the url for the server
	 */
	public static final String SERVER_URL = "url";
	/**
	 * Key to retrieve the server type (DAS2, DAS, Quickload or other)
	 */
	public static final String TYPE = "type";
	/**
	 * Key to retrieve ??
	 */
	public static final String ORDER = "order";
	
	public static final String DEFAULT = "default";
}
