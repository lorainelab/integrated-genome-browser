package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.util.Comparator;

public final class GenomeVersionDateComparator implements Comparator<AnnotatedSeqGroup> {
	private static final Comparator<String> stringComp = new StringVersionDateComparator();
	public int compare(AnnotatedSeqGroup group1, AnnotatedSeqGroup group2) {
		return stringComp.compare(group1.getID(), group2.getID());
	}
}
