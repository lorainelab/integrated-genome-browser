package com.lorainelab.main;

/**
 * This class is used to clear the OSGi bundle cache
 */
public class ClearBundleCache {

    public static void main(String[] args) {
        OSGiHandler.getInstance().clearCache();
    }
}
