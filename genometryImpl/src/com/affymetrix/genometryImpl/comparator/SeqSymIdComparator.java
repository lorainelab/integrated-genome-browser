package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.util.Comparator;

/**
 *  Sorts SeqSymmetries based on lexicographic ordering of IDs 
 */
public final class SeqSymIdComparator implements Comparator<SeqSymmetry> {
	public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
		String id1 = sym1.getID();
		String id2 = sym2.getID();
		return compareStrings(id1, id2);
	}

	private static int compareStrings(String id1, String id2) {
		if (id1 == null || id2 == null) {
			if (id1 == null) {
				if (id2 == null) {
					return 0;
				}
				return 1;
			}
			return -1;
		}
		return id1.compareTo(id2);
	}
}
