package com.affymetrix.igb.general;

import com.affymetrix.igb.view.GeneralLoadUtils.LoadStatus;
import com.affymetrix.igb.view.GeneralLoadUtils.LoadStrategy;

/**
 * A class that's useful for visualizing a generic feature.
 * A feature is unique to a genome version/species/server.
 * (Even if the feature names and version names match, but the servers don't,
 * we can't guarantee that they would contain the same information.)
 */
public final class GenericFeature {

    public final String featureName;      // friendly name of the feature.
    public final GenericVersion gVersion;        // Points to the version that uses this feature.
    public LoadStrategy loadStrategy;  // range chosen by the user, defaults to NO_LOAD.
    public LoadStatus loadStatus;   // status of feature loading, visible to the user.

    /**
     * @param featureName
     * @param gVersion
     */
    public GenericFeature(String featureName, GenericVersion gVersion) {
        this.featureName = featureName;
        this.gVersion = gVersion;
        this.loadStrategy = LoadStrategy.NO_LOAD;
        this.loadStatus = LoadStatus.UNLOADED;
    }
}
