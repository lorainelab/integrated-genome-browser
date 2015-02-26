package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

/**
 * This is a filter which is used to filter out the symmetries with children
 *
 * @author hiralv
 */
public class NoIntronFilter extends SymmetryFilter {

    @Override
    public String getName() {
        return "no_intron";
    }

    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
        if (ss.getChildCount() > 1) {
            return false;
        }
        return true;
    }
}
