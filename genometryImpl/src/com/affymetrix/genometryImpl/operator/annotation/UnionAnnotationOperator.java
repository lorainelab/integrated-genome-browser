package com.affymetrix.genometryImpl.operator.annotation;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

public class UnionAnnotationOperator implements AnnotationOperator {

	@Override
	public String getName() {
		return "union";
	}

	@Override
	public SeqSymmetry operate(List<SeqSymmetry> symList) {
		return null;
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<List<SeqSymmetry>> symList) {
		if (symList.size() < getOperandCountMin() || symList.size() > getOperandCountMax()) {
			return null;
		}
		//    MutableSeqSymmetry psym = new SimpleSymWithProps();
		// first get the landscape as a GraphSym
		List<SeqSymmetry> allSyms = new ArrayList<SeqSymmetry>();
		for (List<SeqSymmetry> syms : symList) {
			allSyms.addAll(syms);
		}
		GraphSym landscape = SeqSymSummarizer.getSymmetrySummary(allSyms, seq, true, "");
		// now just flatten it
		if (landscape != null) {
			return projectLandscape(landscape);
		} else {
			return null;
		}
	}

	private static SymWithProps projectLandscape(GraphSym landscape) {
		BioSeq seq = landscape.getGraphSeq();
		SimpleSymWithProps psym = new SimpleSymWithProps();
		int num_points = landscape.getPointCount();

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = landscape.getGraphXCoord(i);
			float ypos = landscape.getGraphYCoord(i);
			if (in_region) {
				if (ypos <= 0) { // reached end of region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
			}
			else {  // not already in_region
				if (ypos > 0) {
					in_region = true;
					current_region_start = xpos;
				}
			}
		}
		if (in_region) {  // last point was still in_region, so make a span to end?
			// pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
			System.err.println("still in a covered region at end of projectLandscape() loop!");
		}

		if (psym.getChildCount() <= 0) {
			psym = null;
		}
		else {
			// landscape is already sorted, so should be able to derive parent min and max
			int pmin = psym.getChild(0).getSpan(seq).getMin();
			int pmax = psym.getChild(psym.getChildCount()-1).getSpan(seq).getMax();
			SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
			psym.addSpan(pspan);
		}
		return psym;
	}

	@Override
	public int getOperandCountMin() {
		return 2;
	}

	@Override
	public int getOperandCountMax() {
		return Integer.MAX_VALUE;
	}
}
