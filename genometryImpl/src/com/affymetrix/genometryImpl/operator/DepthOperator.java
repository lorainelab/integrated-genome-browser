package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class DepthOperator implements Operator {

	@Override
	public String getName() {
		return "depth";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		if (symList.size() != 1 || !(symList.get(0) instanceof TypeContainerAnnot)) {
			return null;
		}
		
		return SeqSymSummarizer.getSymmetrySummary(symList, aseq, false, null);
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 1 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 1 : 0;
	}

	@Override
	public Map<String, Class<?>> getParameters() {
		return null;
	}

	@Override
	public boolean setParameters(Map<String, Object> obj) {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Graph;
	}
}
