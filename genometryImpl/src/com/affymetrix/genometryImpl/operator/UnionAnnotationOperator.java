/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/*package com.affymetrix.genometryImpl.operator;

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
public class UnionAnnotationOperator implements Operator{

	@Override
	public String getName() {
		return "union";
	}
	
	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		if (symList.size() < getOperandCountMin(FileTypeCategory.Annotation) || symList.size() > getOperandCountMax(FileTypeCategory.Annotation)) {
			return null;
		}
		//    MutableSeqSymmetry psym = new SimpleSymWithProps();
		// first get the landscape as a GraphSym
		List<SeqSymmetry> allSyms = new ArrayList<SeqSymmetry>();
		for (List<SeqSymmetry> syms : symList) {
			allSyms.addAll(syms);
		}
		GraphSym landscape = SeqSymSummarizer.getSymmetrySummary(allSyms, aseq, true, "");
		// now just flatten it
		if (landscape != null) {
			return projectLandscape(landscape);
		} else {
			return null;
		}
	}
	
	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 2 : 0;
	}
	
	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? Integer.MAX_VALUE : 0;
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
	
} */
