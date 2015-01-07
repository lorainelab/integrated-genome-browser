package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.google.common.collect.SetMultimap;
import java.util.Set;

public interface VersionDiscoverer {

    public GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName);

    public String versionName2Species(String versionName);

    public SetMultimap<String, GenericVersion> getSpecies2Generic();

    /**
     * Get list of versions for given species. Create it if it doesn't exist.
     *
     * @param speciesName
     * @return list of versions for the given species.
     */
    public Set<GenericVersion> getSpecies2Generic(String speciesName);
}
