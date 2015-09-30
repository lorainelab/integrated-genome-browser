package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.AnnotationWriter;
import java.net.URI;
import java.util.Optional;

public class BedGraph extends Wiggle implements AnnotationWriter {

    private static final String TRACK_TYPE = "bedgraph";

    public BedGraph() {
        this(null, Optional.empty(), null, null);
    }

    public BedGraph(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion seq_group) {
        super(uri, indexUri, featureName, seq_group);
    }

    @Override
    protected String getTrackType() {
        return TRACK_TYPE;
    }

    @Override
    public String getMimeType() {
        return "text/bedgraph";
    }
}
