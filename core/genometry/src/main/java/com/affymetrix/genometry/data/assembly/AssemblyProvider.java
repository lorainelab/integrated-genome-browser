package com.affymetrix.genometry.data.assembly;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.general.DataContainer;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author dcnorris
 */
public interface AssemblyProvider {

    /**
     * @return the name of the AssemblyProvider
     */
    public String getName();

    /**
     * @param name which will be displayed to users to identify this AssemblyProvider
     */
    public void setName(String name);

    /**
     * @return Returns the default load priority
     * this priority will be used to determine the order
     * in which to query AssemblyProvider instances. Users will be able to override the value returned here.
     */
    public int getLoadPriority();

    /**
     * sets the load priority
     *
     * @param loadPriority
     */
    public void setLoadPriority(int loadPriority);

    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion);

    public default Optional<Multimap<String, String>> getChromosomeSynonyms(DataContainer dataContainer) {
        return Optional.empty();
    }

}
