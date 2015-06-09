package com.affymetrix.genometry.general;

/**
 * This class contains constant values that are used to stored preferences for
 * the generic server.
 *
 */
public class DataProviderPrefKeys {

    private DataProviderPrefKeys() {
        //private construtor to prevent instantiation
    }
    public static final String PROVIDER_NAME = "name";
    public static final String FACTORY_NAME = "factoryName";
    public static final String LOGIN = "login";
    public static final String PASSWORD = "password";
    public static final String PRIMARY_URL = "url";
    public static final String MIRROR_URL = "mirrorUrl";
    public static final String STATUS = "status";
    public static final String LOAD_PRIORITY = "loadPriority";
    public static final String IS_DEFAULT_SERVER = "default";
    public static final String REMEMBER_CREDENTIALS = "rememberCredentials";
    public static final String IS_EDITABLE = "isEditable";
}
