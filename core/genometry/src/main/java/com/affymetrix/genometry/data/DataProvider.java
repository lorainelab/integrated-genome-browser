package com.affymetrix.genometry.data;

import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface DataProvider {

    /**
     * @return name of the data provider which will populate name column of "Data Source Provider" panel
     */
    public String getName();

    /**
     * @param name which will populate name column of "Data Sources" panel
     */
    public void setName(String name);

    /**
     * @return Returns the default load priority
     * this priority will be used to determine the order
     * in which to query DataProvider instances. Users will be able to override the value returned here.
     */
    public int getLoadPriority();

    /**
     * sets the load priority
     *
     * @param loadPriority
     */
    public void setLoadPriority(int loadPriority);

    /**
     * @return required globally unique url
     */
    public String getUrl();

    /**
     * @return Optional mirror url for automatic failover
     */
    public default Optional<String> getMirrorUrl() {
        return Optional.empty();
    }

    /**
     * If a DataProvider is providing data for a species wich does not already exist,
     * providing SpeciesInfo is required, otherwise this is not a required file.
     */
    public default Optional<Set<SpeciesInfo>> getSpeciesInfo() {
        return Optional.empty();
    }

    public void setMirrorUrl(String mirrorUrl);

    /**
     * @return Checks and returns current server status,
     * this status drives
     */
    public ResourceStatus getStatus();

    public void setStatus(ResourceStatus serverStatus);

    /**
     * @return set of genome versions for which data is available
     */
    public Set<String> getSupportedGenomeVersionNames();

    public default Optional<String> getGenomeVersionDescription(String genomeVersionName) {
        return Optional.empty();
    }

}
