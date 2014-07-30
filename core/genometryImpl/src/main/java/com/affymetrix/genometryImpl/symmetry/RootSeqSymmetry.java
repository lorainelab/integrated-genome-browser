package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithProps;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class RootSeqSymmetry extends SimpleSymWithProps {

    public abstract FileTypeCategory getCategory();

    public abstract void search(Set<SeqSymmetry> results, String id);

    public abstract void searchHints(Set<String> results, Pattern regex, int limit);

    public abstract void search(Set<SeqSymmetry> results, Pattern regex, int limit);

    public abstract void searchProperties(Set<SeqSymmetry> results, Pattern regex, int limit);
}
