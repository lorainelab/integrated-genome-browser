package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymSummarizer;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

import java.util.List;

public class SoftClipDepthOperator extends AbstractAnnotationTransformer implements Operator {

    public SoftClipDepthOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return fileTypeCategory.toString().toLowerCase() + "_softclip_depth";
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        return SeqSymSummarizer.getSymmetrySoftclipSummary(symList, aseq, false, null);
    }

    @Override
    public FileTypeCategory getOutputCategory() {
        return FileTypeCategory.Graph;
    }
}
