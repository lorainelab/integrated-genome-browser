package com.affymetrix.genometry.comparator;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import java.io.Serializable;
import java.util.Comparator;

public final class GenomeVersionDateComparator implements Comparator<AnnotatedSeqGroup>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Comparator<String> stringComp = new StringVersionDateComparator();

    public int compare(AnnotatedSeqGroup group1, AnnotatedSeqGroup group2) {
        return stringComp.compare(group1.getID(), group2.getID());
    }
}
