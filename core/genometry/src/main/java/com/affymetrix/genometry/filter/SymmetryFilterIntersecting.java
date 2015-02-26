package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;

/**
 * Filters out symmetry that intersects with provided symmetry as parameter.
 *
 * @author hiralv
 */
public class SymmetryFilterIntersecting extends SymmetryFilter {

    private final MutableSeqSymmetry dummySym = new SimpleMutableSeqSymmetry();

    private final static String SEQSYMMETRY = "seqsymmetry";
    private final static SeqSymmetry DEFAULT_SEQSYMMETRY = null;

    private Parameter<SeqSymmetry> original_sym = new Parameter<>(DEFAULT_SEQSYMMETRY);

    public SymmetryFilterIntersecting() {
        super();
        parameters.addParameter(SEQSYMMETRY, SeqSymmetry.class, original_sym);
    }

    public String getName() {
        return "existing";
    }

    @Override
    public String getDisplay() {
        return getName();
    }

    public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym) {
        /**
         * Since GraphSym is only SeqSymmetry containing all points.
         * The intersection may find some points intersecting and
         * thus not add whole GraphSym at all. So if GraphSym is encountered
         * the it's not checked if it is intersecting.
         */
        if (sym instanceof GraphSym) {
            return true;
        }

        dummySym.clear();

        return !SeqUtils.intersection(sym, original_sym.get(), dummySym, seq);
    }

}
