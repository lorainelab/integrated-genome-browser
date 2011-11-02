package com.affymetrix.genometryImpl.operator.annotation;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class XorAnnotationOperator extends ExclusiveAnnotationOperator {

	@Override
	public String getName() {
		return "xor";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<List<SeqSymmetry>> symList) {
		if (symList.size() < getOperandCountMin() || symList.size() > getOperandCountMax()) {
			return null;
		}
		return getXor(symList.get(0), symList.get(1), seq);
	}

	@Override
	public int getOperandCountMin() {
		return 2;
	}

	@Override
	public int getOperandCountMax() {
		return 2;
	}
}
