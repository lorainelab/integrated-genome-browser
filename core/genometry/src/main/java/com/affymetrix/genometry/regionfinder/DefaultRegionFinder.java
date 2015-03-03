package com.affymetrix.genometry.regionfinder;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class DefaultRegionFinder implements RegionFinder {

    public SeqSpan findInterestingRegion(BioSeq aseq, List<SeqSymmetry> syms) {
        List<SeqSymmetry> less_syms = new ArrayList<>();
        if (syms.size() > 100) {
            int size = syms.size();
            int inc = size / 100;
            for (int i = 0; i < size; i += inc) {
                less_syms.add(syms.get(i));
            }
        } else {
            less_syms = syms;
        }

        MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
        SeqUtils.union(less_syms, resultSym, aseq);
        return resultSym.getSpan(aseq);
    }

}
