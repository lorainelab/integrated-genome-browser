package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class DepthOperator implements Operator {
	private final FileTypeCategory fileTypeCategory;

	public DepthOperator(FileTypeCategory fileTypeCategory) {
		super();
		this.fileTypeCategory = fileTypeCategory;
	}

	@Override
	public String getName() {
		return fileTypeCategory.toString() + " depth";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {		
		return SeqSymSummarizer.getSymmetrySummary(symList, aseq, false, null);
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == this.fileTypeCategory ? 1 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == this.fileTypeCategory ? 1 : 0;
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
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Graph;
	}
}
