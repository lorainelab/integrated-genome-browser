package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;

public class DummyRootSeqSymmetry extends RootSeqSymmetry {
	private final FileTypeCategory category;
	public DummyRootSeqSymmetry(FileTypeCategory category) {
		this.category = category;
	}
	@Override public FileTypeCategory getCategory() { return category; }
}
