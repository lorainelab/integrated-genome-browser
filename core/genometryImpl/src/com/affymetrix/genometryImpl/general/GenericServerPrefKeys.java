package com.affymetrix.genometryImpl.general;

/**
 * This class contains constant values that are used to stored preferences for
 * the generic server.
 *
 */
public class GenericServerPrefKeys {

    private GenericServerPrefKeys() {
        //private construtor to prevent instantiation
    }
    /**
     * Key to retrieve the value for the server name
     */
    public static final String SERVER_NAME = "name";
    public static final String IS_SERVER_ENABLED = "enabled";
    public static final String ENABLE_IF_AVAILABLE = "enableIfAvailable";
    public static final String SERVER_LOGIN = "login";
    public static final String SERVER_PASSWORD = "password";
    public static final String SERVER_URL = "url";
    /**
     * Key to retrieve the server type (DAS2, DAS, Quickload or other)
     */
    public static final String SERVER_TYPE = "type";
    public static final String SERVER_ORDER = "order";
    public static final String IS_DEFAULT_SERVER = "default";
}