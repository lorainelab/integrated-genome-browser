package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import java.util.List;
import java.util.regex.Pattern;

public abstract class RootSeqSymmetry extends SimpleSymWithProps {
	public abstract FileTypeCategory getCategory();
	public abstract void searchHints(List<String> results, Pattern regex, int limit);
	public abstract void search(List<SeqSymmetry> results, Pattern regex, int limit);
	public abstract void searchProperties(List<SeqSymmetry> results, Pattern regex, int limit);
}
