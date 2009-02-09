package com.affymetrix.igb.general;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that's useful for visualizing a generic version.
 */
public final class GenericVersion {

    public final String versionName;          // name of the version.
    public final genericServer gServer; // generic Server object.
    public final Object versionSourceObj;     // Das2VersionedSource, DasVersionedSource, ..., QuickLoad?
    public final List<GenericFeature> features;

    /**
     * @param versionName
     * @param gServer
     * @param versionSourceObj
     */
    public GenericVersion(String versionName, genericServer gServer, Object versionSourceObj) {
        this.versionName = versionName;
        this.gServer = gServer;
        this.versionSourceObj = versionSourceObj;
        this.features = new ArrayList<GenericFeature>();
    }
}
