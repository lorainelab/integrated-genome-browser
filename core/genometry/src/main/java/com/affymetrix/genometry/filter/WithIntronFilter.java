package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

/**
 * This is a filter which is used to filter out the symmetries with no children
 *
 * @author Anuj
 */
public class WithIntronFilter extends SymmetryFilter {

    @Override
    public String getName() {
        return "intron";
    }

    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
        return ss.getChildCount() > 1;
    }

}
