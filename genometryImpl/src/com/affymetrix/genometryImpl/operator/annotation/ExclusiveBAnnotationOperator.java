package com.affymetrix.genometryImpl.operator.annotation;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;

public class ExclusiveBAnnotationOperator extends ExclusiveAnnotationOperator {

	@Override
	public String getName() {
		return "B not A:";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<List<SeqSymmetry>> symList) {
		if (symList.size() < getOperandCountMin() || symList.size() > getOperandCountMax()) {
			return null;
		}
		return operate(seq, symList.get(1), symList.get(0));
	}
}
