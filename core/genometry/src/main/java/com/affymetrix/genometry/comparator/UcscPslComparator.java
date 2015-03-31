package com.affymetrix.genometry.comparator;

import com.affymetrix.genometry.symmetry.impl.UcscPslSym;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Sorts based on UcscPslSym.getTargetMin().
 */
public final class UcscPslComparator implements Comparator<UcscPslSym>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Sorts two instances of UcscPslSym based on UcscPslSym.getTargetMin(),
     * and in second case, by UscsPslSym.getTargetMax().
     *
     * @param sym1
     * @param sym2
     * @return comparison integer
     */
    public int compare(UcscPslSym sym1, UcscPslSym sym2) {
        final int min1 = sym1.getTargetMin();
        final int min2 = sym2.getTargetMin();
        if (min1 != min2) {
            return ((Integer) min1).compareTo(min2);
        }
        return ((Integer) sym1.getTargetMax()).compareTo(sym2.getTargetMax());
    }
}
