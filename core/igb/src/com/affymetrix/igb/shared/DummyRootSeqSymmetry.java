package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;
import java.util.regex.Pattern;

public class DummyRootSeqSymmetry extends RootSeqSymmetry {
	private final FileTypeCategory category;
	public DummyRootSeqSymmetry(FileTypeCategory category) {
		this.category = category;
	}
	@Override public FileTypeCategory getCategory() { return category; }
	@Override public void searchHints(List<String> results, Pattern regex, int limit) { }
	@Override public void search(List<SeqSymmetry> result, Pattern regex, int limit) { }
	@Override public void searchProperties(List<SeqSymmetry> results, Pattern regex, int limit) { }
}
