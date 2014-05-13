package com.affymetrix.genometryImpl.symloader;

import java.net.URI;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.AnnotationWriter;

public class BedGraph extends Wiggle implements AnnotationWriter {
	private static final String TRACK_TYPE = "bedgraph";

	public BedGraph() {
		this(null, null, null);
	}

	public BedGraph(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri, featureName, seq_group);
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
