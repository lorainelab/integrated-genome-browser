package com.affymetrix.genometryImpl.operator.annotation;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class ExclusiveAAnnotationOperator extends ExclusiveAnnotationOperator {

	@Override
	public String getName() {
		return "A not B:";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<List<SeqSymmetry>> symList) {
		if (symList.size() < getOperandCountMin() || symList.size() > getOperandCountMax()) {
			return null;
		}
		return operate(seq, symList.get(0), symList.get(1));
	}
}
