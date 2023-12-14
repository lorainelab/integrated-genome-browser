package org.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.symloader.SymLoader;

import java.net.URI;
import java.util.Optional;

public class UCSCRestSymLoader extends SymLoader {
    private String trackType;

    public UCSCRestSymLoader(URI uri, Optional<URI> indexUri, String type, String trackType, GenomeVersion genomeVersion) {
        super(uri, indexUri, type, genomeVersion);
        this.trackType = trackType;
    }
}
