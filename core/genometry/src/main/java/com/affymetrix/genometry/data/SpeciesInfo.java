package com.affymetrix.genometry.data;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Optional;

/**
 *
 * @author dcnorris
 */
public class SpeciesInfo {

    private final String name;
    private final Optional<String> commonName;
    private final String genomeVersionNamePrefix;

    public SpeciesInfo(String name, Optional<String> commonName, String genomeVersionNamePrefix) {
        this.name = checkNotNull(name, "name is a required field.");
        this.commonName = commonName == null ? Optional.empty() : commonName;
        this.genomeVersionNamePrefix = checkNotNull(genomeVersionNamePrefix, "genomeVersionNamePrefix is a required field.");
    }

    public String getName() {
        return name;
    }

    public Optional<String> getCommonName() {
        return commonName;
    }

    public String getGenomeVersionNamePrefix() {
        return genomeVersionNamePrefix;
    }

}
