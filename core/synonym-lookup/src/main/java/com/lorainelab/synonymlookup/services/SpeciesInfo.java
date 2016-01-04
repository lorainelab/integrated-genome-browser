package org.lorainelab.igb.synonymlookup.services;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @author dcnorris
 */
public class SpeciesInfo {

    private final String name;
    private final String commonName;
    private final String genomeVersionNamePrefix;

    public SpeciesInfo(String name, String commonName, String genomeVersionNamePrefix) {
        this.name = checkNotNull(name, "name is a required field.");
        this.commonName = commonName;
        this.genomeVersionNamePrefix = checkNotNull(genomeVersionNamePrefix, "genomeVersionNamePrefix is a required field.");
    }

    public String getName() {
        return name;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getGenomeVersionNamePrefix() {
        return genomeVersionNamePrefix;
    }

}
