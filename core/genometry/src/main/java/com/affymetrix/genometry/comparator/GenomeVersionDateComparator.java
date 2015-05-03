package com.affymetrix.genometry.comparator;

import com.affymetrix.genometry.GenomeVersion;
import java.io.Serializable;
import java.util.Comparator;

public final class GenomeVersionDateComparator implements Comparator<GenomeVersion>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Comparator<String> stringComp = new StringVersionDateComparator();

    public int compare(GenomeVersion group1, GenomeVersion group2) {
        return stringComp.compare(group1.getName(), group2.getName());
    }
}
