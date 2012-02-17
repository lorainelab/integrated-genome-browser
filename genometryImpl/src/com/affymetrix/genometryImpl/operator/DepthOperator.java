package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class DepthOperator implements Operator {
	private static FileTypeCategory input[] = new FileTypeCategory[]{FileTypeCategory.Annotation};
	private static FileTypeCategory output = FileTypeCategory.Graph;
	
	@Override
	public String getName() {
		return "depth";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {		
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
	public boolean supportsTwoTrack() {
		return true;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return output;
	}

	@Override
	public FileTypeCategory[] getInputCategory() {
		return input; 
	}
}
