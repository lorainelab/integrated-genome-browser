/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author auser
 *//*
public class ExclusiveAnnotationOperator implements Operator {
	@Override
	public String getName() {
		return "Exclusive Annotation";
	}
	@Override
	public SeqSymmetry operate(BioSeq aseq, SeqSymmetry symList) {
		SeqSymmetry xorSym = getXor(symList, symsB, aseq);
		//  if no spans for xor, then won't be any for one-sided xor either, so return null;
		if (xorSym == null)  { return null; }
		List<SeqSymmetry> xorList = new ArrayList<SeqSymmetry>();
		xorList.add(xorSym);
		SeqSymmetry a_not_b = SeqSymSummarizer.getIntersection(symsA, xorList, seq);
		return a_not_b;
	}
	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 2 : 0;
	}
	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 2 : 0;
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
		return FileTypeCategory.Annotation;
	}
	protected SeqSymmetry getXor(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq) {
		MutableSeqSymmetry psym = new SimpleSymWithProps();
		SeqSymmetry unionA = SeqSymSummarizer.getUnion(symsA, seq);
		SeqSymmetry unionB = SeqSymSummarizer.getUnion(symsB, seq);
		List<SeqSymmetry> symsAB = new ArrayList<SeqSymmetry>();
		symsAB.add(unionA);
		symsAB.add(unionB);
		GraphSym combo_graph = SeqSymSummarizer.getSymmetrySummary(symsAB, seq, false, "");
		// combo_graph should now be landscape where:
		//    no coverage ==> depth = 0;
		//    A not B     ==> depth = 1;
		//    B not A     ==> depth = 1;
		//    A && B      ==> depth = 2;

		// so any regions with depth == 1 are XOR regions
		int num_points = combo_graph.getPointCount();

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = combo_graph.getGraphXCoord(i);
			float ypos = combo_graph.getGraphYCoord(i);
			if (in_region) {
				if (ypos < 1 || ypos > 1) { // reached end of xor region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
			}
			else {  // not already in_region
				if (ypos == 1) {
					in_region = true;
					current_region_start = xpos;
				}
			}
		}
		if (in_region) {  // last point was still in_region, so make a span to end?
			// pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
			System.err.println("still in a covered region at end of getUnion() loop!");
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
}
*/