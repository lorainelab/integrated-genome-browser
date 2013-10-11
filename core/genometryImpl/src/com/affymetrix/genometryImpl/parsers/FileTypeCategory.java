package com.affymetrix.genometryImpl.parsers;

public enum FileTypeCategory {
	Axis(false),
	Annotation(true),
	Alignment(true),
/*	Cytoband(true), */
	Graph(false),
	Sequence(false),
	Mismatch(false),
	ProbeSet(true),
	ScoredContainer(false);

	private final boolean container;
	private FileTypeCategory(boolean container) {
		this.container = container;
	}
	public boolean isContainer() {
		return container;
	}
}
