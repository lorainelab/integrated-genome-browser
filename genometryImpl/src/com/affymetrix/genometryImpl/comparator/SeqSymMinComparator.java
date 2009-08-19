package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.util.Comparator;

/**
 *  Sorts SeqSymmetries based first on {@link SeqSpan#getMin()},
 *   then on {@link SeqSpan#getMax()}.
 *
 *  @see SeqSymStartComparator
 */
public final class SeqSymMinComparator implements Comparator<SeqSymmetry> {
	private BioSeq seq;

	/** Constructor.
	 *  @param s  sequence to base the sorting on
	 *  @param b  true to sort ascending, false for descending
	 */
	public SeqSymMinComparator(BioSeq s) {
		this.seq = s;
	}

	public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
		SeqSpan span1 = sym1.getSpan(seq);
		SeqSpan span2 = sym2.getSpan(seq);
		final int min1 = span1.getMin();
		final int min2 = span2.getMin();
		if (min1 != min2) {
			return ((Integer) min1).compareTo(min2);
		}
		return ((Integer) span1.getMax()).compareTo(span2.getMax());
	}
}
