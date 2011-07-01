package com.affymetrix.genometryImpl.operator.annotation;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.SeqSymmetry;

public abstract class ExclusiveAnnotationOperator implements AnnotationOperator {

	protected SeqSymmetry operate(BioSeq seq, List<SeqSymmetry> symsA, List<SeqSymmetry> symsB) {
		SeqSymmetry xorSym = SeqSymSummarizer.getXor(symsA, symsB, seq);
		//  if no spans for xor, then won't be any for one-sided xor either, so return null;
		if (xorSym == null)  { return null; }
		List<SeqSymmetry> xorList = new ArrayList<SeqSymmetry>();
		xorList.add(xorSym);
		SeqSymmetry a_not_b = SeqSymSummarizer.getIntersection(symsA, xorList, seq);
		return a_not_b;
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
