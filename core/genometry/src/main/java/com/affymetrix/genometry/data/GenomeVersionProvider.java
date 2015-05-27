package com.affymetrix.genometry.data;

import com.google.common.collect.SetMultimap;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface GenomeVersionProvider {

    /**
     * @return set of genome versions for which data is available
     */
    public Set<String> getSupportedGenomeVersionNames();

    /**
     * If a DataProvider is providing data for a species which does not already exist,
     * providing SpeciesInfo is required, otherwise this is not a required file.
     */
    public default Optional<Set<SpeciesInfo>> getSpeciesInfo() {
        return Optional.empty();
    }

    public default Optional<SetMultimap<String, String>> getGenomeVersionSynonyms() {
        return Optional.empty();
    }

    public default Optional<String> getGenomeVersionDescription(String genomeVersionName) {
        return Optional.empty();
    }
}
