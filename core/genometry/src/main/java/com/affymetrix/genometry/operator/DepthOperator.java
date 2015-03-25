package com.affymetrix.genometry.operator;

import java.util.List;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymSummarizer;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

public class DepthOperator extends AbstractAnnotationTransformer implements Operator {
	
	public DepthOperator(FileTypeCategory fileTypeCategory) {
		super(fileTypeCategory);
	}

	@Override
	public String getName() {
		return fileTypeCategory.toString().toLowerCase() + "_depth";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {		
		return SeqSymSummarizer.getSymmetrySummary(symList, aseq, false, null);
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Graph;
	}
}
