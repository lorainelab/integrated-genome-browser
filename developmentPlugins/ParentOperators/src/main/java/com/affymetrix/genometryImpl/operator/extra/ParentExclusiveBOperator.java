package com.affymetrix.genometryImpl.operator.extra;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class ParentExclusiveBOperator extends ParentExclusiveOperator implements Operator {

    public ParentExclusiveBOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return category.toString().toLowerCase() + "_parent_b_not_a";
    }

    @Override
    public String getDisplay() {
        return ParentOperatorConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry operate(BioSeq seq, java.util.List<SeqSymmetry> symList) {
        return operate(seq, symList.get(1), symList.get(0));
    }
}
