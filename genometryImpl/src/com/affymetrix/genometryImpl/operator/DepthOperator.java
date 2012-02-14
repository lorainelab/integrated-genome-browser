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
	public SeqSymmetry operate(List<SeqSymmetry> symList) {
		if (symList.size() != 1 || !(symList.get(0) instanceof TypeContainerAnnot)) {
			return null;
		}
		String meth = BioSeq.determineMethod(symList.get(0));
		
		if (meth == null) {
			return null;
		}
		BioSeq seq = symList.get(0).getSpanSeq(0);
		return SeqSymSummarizer.getSymmetrySummary(symList, seq, false, meth);
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
}
