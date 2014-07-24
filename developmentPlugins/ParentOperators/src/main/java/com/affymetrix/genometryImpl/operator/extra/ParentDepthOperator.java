package com.affymetrix.genometryImpl.operator.extra;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.AbstractAnnotationTransformer;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;

public class ParentDepthOperator extends AbstractAnnotationTransformer implements Operator {

    public ParentDepthOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return fileTypeCategory.toString().toLowerCase() + "_parent_depth";
    }

    @Override
    public String getDisplay() {
        return ParentOperatorConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        return SeqSymSummarizer.getSymmetrySummary(symList, aseq, false, null, 2);
    }

    @Override
    public FileTypeCategory getOutputCategory() {
        return FileTypeCategory.Graph;
    }

}
