package com.affymetrix.genometryImpl.operator.annotation;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;

public class NotAnnotationOperator implements AnnotationOperator {

	@Override
	public String getName() {
		return "not";
	}

	@Override
	public SeqSymmetry operate(List<SeqSymmetry> symList) {
		return null;
	}

	private static SeqSymmetry getNot(List<SeqSymmetry> syms, BioSeq seq, boolean include_ends) {
		SeqSymmetry union = SeqSymSummarizer.getUnion(syms, seq);
		int spanCount = union.getChildCount();

		// rest of this is pretty much pulled directly from SeqUtils.inverse()
		if (! include_ends )  {
			if (spanCount <= 1) {  return null; }  // no gaps, no resulting inversion
		}
		MutableSeqSymmetry invertedSym = new SimpleSymWithProps();
		if (include_ends) {
			if (spanCount < 1) {
				// no spans, so just return sym of whole range of seq
				invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
				return invertedSym;
			}
			else {
				SeqSpan firstSpan = union.getChild(0).getSpan(seq);
				if (firstSpan.getMin() > 0) {
					SeqSymmetry beforeSym = new SingletonSeqSymmetry(0, firstSpan.getMin(), seq);
					invertedSym.addChild(beforeSym);
				}
			}
		}
		for (int i=0; i<spanCount-1; i++) {
			SeqSpan preSpan = union.getChild(i).getSpan(seq);
			SeqSpan postSpan = union.getChild(i+1).getSpan(seq);
			SeqSymmetry gapSym =
				new SingletonSeqSymmetry(preSpan.getMax(), postSpan.getMin(), seq);
			invertedSym.addChild(gapSym);
		}
		if (include_ends) {
			SeqSpan lastSpan = union.getChild(spanCount-1).getSpan(seq);
			if (lastSpan.getMax() < seq.getLength()) {
				SeqSymmetry afterSym = new SingletonSeqSymmetry(lastSpan.getMax(), seq.getLength(), seq);
				invertedSym.addChild(afterSym);
			}
		}
		if (include_ends) {
			invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
		}
		else {
			int min = union.getChild(0).getSpan(seq).getMax();
			int max = union.getChild(spanCount-1).getSpan(seq).getMin();
			invertedSym.addSpan(new SimpleSeqSpan(min, max, seq));
		}
		return invertedSym;
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<List<SeqSymmetry>> symList) {
		if (symList.size() < getOperandCountMin() || symList.size() > getOperandCountMax()) {
			return null;
		}
		return getNot(symList.get(0), seq, true);
	}

	@Override
	public int getOperandCountMin() {
		return 1;
	}

	@Override
	public int getOperandCountMax() {
		return 1;
	}
}
