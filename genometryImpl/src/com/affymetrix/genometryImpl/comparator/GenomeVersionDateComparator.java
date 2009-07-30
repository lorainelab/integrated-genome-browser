package com.affymetrix.genometryImpl.comparator;

import java.util.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

public final class GenomeVersionDateComparator implements Comparator<AnnotatedSeqGroup> {
	public int compare(AnnotatedSeqGroup group1, AnnotatedSeqGroup group2) {
		String name1 = group1.getID();
		String name2 = group2.getID();
		return new StringVersionDateComparator().compare(name1, name2);
	}
}
