package com.affymetrix.genometry.comparator;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.Comparator;

/**
 * Sorts SeqSymmetries based first on {@link SeqSpan#getMin()},
 * then on {@link SeqSpan#getMax()}.
 *
 * @see SeqSymStartComparator
 */
public final class SeqSymMinComparator implements Comparator<SeqSymmetry> {

    private final BioSeq seq;

    /**
     * Constructor.
     *
     * @param s sequence to base the sorting on
     */
    public SeqSymMinComparator(BioSeq s) {
        this.seq = s;
    }

    public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
        SeqSpan span1 = sym1.getSpan(seq);
        SeqSpan span2 = sym2.getSpan(seq);
        return SeqSpanComparator.compareSpans(span1, span2);
    }
}
