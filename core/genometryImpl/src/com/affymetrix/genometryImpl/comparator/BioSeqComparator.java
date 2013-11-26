package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.BioSeq;
import java.util.Comparator;

/**
 *
 * @author hiralv
 */
public final class BioSeqComparator implements Comparator<BioSeq>{

	static final AlphanumComparator alphaNumComparator = new AlphanumComparator();
	
	public int compare(BioSeq o1, BioSeq o2) {
		return compareStrings(o1.getID(), o2.getID());
	}

	private static int compareStrings(String id1, String id2) {
		if (id1 == null || id2 == null) {
			return compareNullIDs(id1, id2);
		}
		return alphaNumComparator.compare(id1, id2);
	}
	
	static int compareNullIDs(String id1, String id2) {
		if (id1 == null) {
			if (id2 == null) {
				return 0;
			}
			return 1;
		}
		return -1;
	}
}
