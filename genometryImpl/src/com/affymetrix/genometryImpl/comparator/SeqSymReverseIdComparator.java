package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.util.Comparator;

/**
 *
 * @author jnicol
 */
/**
 *  Sorts SeqSymmetries based on lexicographic ordering of reversed IDs
 */
public final class SeqSymReverseIdComparator implements Comparator<SeqSymmetry> {
	public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
		String id1 = sym1.getID();
		String id2 = sym2.getID();

		return compareReverseStrings(id1, id2);
	}

	private static int compareReverseStrings(String id1, String id2) {
		if (id1 == null || id2 == null) {
			if (id1 == null) {
				if (id2 == null) {
					return 0;
				}
				return 1;
			}
			return -1;
		}
		// reverse id1.
		StringBuffer IDbuffer = new StringBuffer(id1);
		IDbuffer = IDbuffer.reverse();
		String tempID1 = IDbuffer.toString();
		// reverse id2.
		IDbuffer = new StringBuffer(id2);
		IDbuffer = IDbuffer.reverse();
		String tempID2 = IDbuffer.toString();
		
		return tempID1.compareTo(tempID2);
	}
}

