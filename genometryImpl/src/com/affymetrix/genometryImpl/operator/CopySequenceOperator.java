package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;

public final class CopySequenceOperator implements Operator {

	@Override
	public String getName() {
		return "copysequence";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		SimpleSymWithResidues residueSym = (SimpleSymWithResidues)symList.get(0).getChild(0);
		return new SimpleSymWithResidues(residueSym.getType(), residueSym.getBioSeq(), residueSym.getMin(),
				residueSym.getMax(), residueSym.getName(), residueSym.getScore(), residueSym.isForward(),
				residueSym.hasCdsSpan() ? residueSym.getCdsSpan().getMin() : Integer.MIN_VALUE,
				residueSym.hasCdsSpan() ? residueSym.getCdsSpan().getMax() : Integer.MAX_VALUE,
				ArrayUtils.clone(residueSym.getBlockMins()), ArrayUtils.clone(residueSym.getBlockMaxs()), new String(residueSym.getResidues()));
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Sequence ? 1 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Sequence ? 1 : 0;
	}

	@Override
	public Map<String, Class<?>> getParameters() {
		return null;
	}

	@Override
	public boolean setParameters(Map<String, Object> parms) {
		return false;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Sequence;
	}
}
