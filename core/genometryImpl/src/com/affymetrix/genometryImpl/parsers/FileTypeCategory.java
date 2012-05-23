package com.affymetrix.genometryImpl.parsers;

public enum FileTypeCategory {
	Annotation(true),
	Alignment(true),
	Graph(false),
	Sequence(false),
	Variant(true),
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
