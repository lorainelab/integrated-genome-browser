package com.affymetrix.genometryImpl.operator;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;

/**
 *
 * @author lfrohman
 */
public abstract class ExclusiveOperator extends XorOperator implements Operator {
	
	public ExclusiveOperator(FileTypeCategory fileTypeCategory) {
		super(fileTypeCategory);
	}
	
	protected SeqSymmetry operate(BioSeq aseq, SeqSymmetry symsA, SeqSymmetry symB){
		return exclusive(aseq, findChildSyms(symsA), findChildSyms(symB));
	}
	
	protected static SeqSymmetry exclusive(BioSeq seq, List<SeqSymmetry> symsA, List<SeqSymmetry> symsB) {
		SeqSymmetry xorSym = getXor(seq, symsA, symsB);
		//  if no spans for xor, then won't be any for one-sided xor either, so return null;
		if (xorSym == null)  { return null; }
		List<SeqSymmetry> xorList = new ArrayList<SeqSymmetry>();
		xorList.add(xorSym);
		SeqSymmetry a_not_b = SeqSymSummarizer.getIntersection(symsA, xorList, seq);
		return a_not_b;
	}
}
