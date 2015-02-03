package com.affymetrix.igb.view;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.symmetry.impl.SeqSymSummarizer;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.IGBConstants;
import java.util.List;

/**
 *
 * @author hiralv
 */
@Component(name = MismatchOperator.COMPONENT_NAME, provide = Operator.class, immediate = true)
public class MismatchOperator extends AbstractMismatchOperator implements Operator {

    public static final String COMPONENT_NAME = "MismatchOperator";

    @Override
    public String getName() {
        return "mismatch";
    }

    @Override
    public String getDisplay() {
        return IGBConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry getMismatch(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int start, int end) {
        return SeqSymSummarizer.getMismatchGraph(syms, seq, false, id, start, end, false);
    }

}
