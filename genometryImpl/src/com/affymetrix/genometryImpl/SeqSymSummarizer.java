/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;

public final class SeqSymSummarizer {

	/**
	 *  Makes a summary graph of a set the spans of some SeqSymmetries on a given BioSeq.
	 *  Descends into parent's descendants, collecting all leaf symmetries and
	 *    creating a summary over the leafs.
	 *  Currently assumes that spans are integral.
	 *<pre>
	 *  Performance: ~ n log(n) ?   where n is number of spans in the syms
	 *      a.) collect leaf spans, ~linear scan (n)
	 *      b.) sort span starts and ends, ~(n)(log(n))
	 *      c.) get transitions, linear scan (n)
	 *</pre>
	 *  @param syms a List of SeqSymmetry's
	 *  @param seq the sequence you want the summary computed for
	 *  @param binary_depth passed through to {@link #getSpanSummary(List, boolean, String)}
	 */
	public static GraphIntervalSym getSymmetrySummary(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id)  {
		int symcount = syms.size();
		List<SeqSpan> leaf_spans = new ArrayList<SeqSpan>(symcount);
		for (int i=0; i<symcount; i++) {
			SeqSymmetry sym = syms.get(i);
			SeqUtils.collectLeafSpans(sym, seq, leaf_spans);
		}
		if (leaf_spans.isEmpty()) {
			return null;
		} else {
			return getSpanSummary(leaf_spans, binary_depth, id);
		}
	}


	/**
	 *  GetSpanSummary.
	 *  General idea is that this will make getUnion(), getIntersection(), etc. easier and
	 *       more efficient.
	 *  @param spans a List of SeqSpan's all defined on the same BioSeq
	 *  @param binary_depth if false, then return a graph with full depth information
	 *                  if true, then return a graph with flattened / binary depth information,
	 *                  1 for covered, 0 for not covered
	 */
	public static GraphIntervalSym getSpanSummary(List<SeqSpan> spans, boolean binary_depth, String gid) {
		//    System.out.println("SeqSymSummarizer: starting to summarize syms");
		//    System.out.println("binary depth: " + binary_depth);
		BioSeq seq = spans.get(0).getBioSeq();
		//int spancount = spans.size();
		int span_num = spans.size();
		int[] starts = new int[span_num];
		int[] stops = new int[span_num];
		for (int i=0; i<span_num; i++) {
			SeqSpan span = spans.get(i);
			starts[i] = span.getMin();
			stops[i] = span.getMax();
		}
		Arrays.sort(starts);
		Arrays.sort(stops);
		int starts_index = 0;
		int stops_index = 0;
		int depth = 0;
		int max_depth = 0;
		// initializing capacity of sum_starts and sum_stops to max that could theoretically be
		//   needed, though likely won't fill it
		IntList transition_xpos = new IntList(span_num * 2);
		FloatList transition_ypos = new FloatList(span_num * 2);
		int transitions = 0; // the value of this variable is never used for anything
		int prev_depth = 0;
		while ((starts_index < span_num) && (stops_index < span_num)) {
			// figure out whether next position is a start, stop, or both
			int next_start = starts[starts_index];
			int next_stop = stops[stops_index];
			int next_transition = Math.min(next_start, next_stop);
			// note that by design, if (next_start == next_stop), then both of the following
			//    conditionals will execute:
			if (next_start <= next_stop) {
				while ((starts_index < span_num) && (starts[starts_index] == next_start)) {
					depth++;
					starts_index++;
				}
			}
			if (next_start >= next_stop) {
				while ((stops_index < span_num) && (stops[stops_index] == next_stop)) {
					depth--;
					stops_index++;
				}
			}
			if (binary_depth) {
				if ((prev_depth <= 0) && (depth > 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(1);
					transitions++;
					prev_depth = 1;
				}
				else if ((prev_depth > 0) && (depth <= 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(0);
					transitions++;
					prev_depth = 0;
				}
			}
			else {
				transition_xpos.add(next_transition);
				transition_ypos.add(depth);
				transitions++;
				max_depth = Math.max(depth, max_depth);
			}
		}
		// clean up last stops...
		//    don't need to worry about "last starts", all starts will be done before last stop...
		while (stops_index < span_num) {
			int next_stop = stops[stops_index];
			int next_transition = next_stop;
			while ((stops_index < span_num) && (stops[stops_index] == next_stop)) {
				depth--;
				stops_index++;
			}
			if (binary_depth) {
				if ((prev_depth <= 0) && (depth > 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(1);
					transitions++;
					prev_depth = 1;
				}
				else if ((prev_depth > 0) && (depth <= 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(0);
					transitions++;
					prev_depth = 0;
				}
			}
			else {
				transition_xpos.add(next_transition);
				transition_ypos.add(depth);
				transitions++;
				max_depth = Math.max(depth, max_depth);
			}
		}

		int[] x_positions = transition_xpos.copyToArray();
		int[] widths = new int[x_positions.length];
		for (int i=0; i<widths.length-1; i++) {
			widths[i] = x_positions[i+1] - x_positions[i];
		}
		widths[widths.length-1] = 1;

		// Originally, this returned a GraphSym with just x and y, but now has widths.
		// Since the x and y values are not changed, all old code that relies on them
		// does not need to change.
		String uid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq);
		GraphIntervalSym gsym =
			new GraphIntervalSym(x_positions, widths, transition_ypos.copyToArray(), uid, seq);
		return gsym;
	}


	/**
	 *  Assumes all spans refer to same BioSeq
	 */
	public static List getMergedSpans(List<SeqSpan> spans) {
		GraphSym landscape = getSpanSummary(spans, true, null);
		List merged_spans = projectLandscapeSpans(landscape);
		return merged_spans;
	}

	public static List<SeqSpan> projectLandscapeSpans(GraphSym landscape) {
		List<SeqSpan> spanlist = new ArrayList<SeqSpan>();
		BioSeq seq = landscape.getGraphSeq();
		int xcoords[] = landscape.getGraphXCoords();
		//float ycoords[] = (float[]) landscape.getGraphYCoords();
		int num_points = xcoords.length;

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = xcoords[i];
			float ypos = landscape.getGraphYCoord(i);
			if (in_region) {
				if (ypos <= 0) { // reached end of region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSpan newspan = new SimpleSeqSpan(current_region_start, current_region_end, seq);
					spanlist.add(newspan);
				} else {  // still in region, do nothing
				}
			} else {  // not already in_region
				if (ypos > 0) {
					in_region = true;
					current_region_start = xpos;
				} else {  // still not in region, so do nothing
				}
			}
		}
		if (in_region) {  // last point was still in_region, so make a span to end?
			// pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
			System.err.println("still in a covered region at end of projectLandscapeSpans() loop!");
		}
		return spanlist;
	}


	public static SymWithProps projectLandscape(GraphSym landscape) {
		BioSeq seq = landscape.getGraphSeq();
		SimpleSymWithProps psym = new SimpleSymWithProps();
		int xcoords[] = landscape.getGraphXCoords();
		//float ycoords[] = (float[]) landscape.getGraphYCoords();
		int num_points = xcoords.length;

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = xcoords[i];
			float ypos = landscape.getGraphYCoord(i);
			if (in_region) {
				if (ypos <= 0) { // reached end of region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
				else {  // still in region, do nothing
				}
			}
			else {  // not already in_region
				if (ypos > 0) {
					in_region = true;
					current_region_start = xpos;
				}
				else {  // still not in region, so do nothing
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

	/**
	 *  Finds the Union of a List of SeqSymmetries.
	 *  This will merge not only overlapping syms but also abutting syms (where symA.getMax() == symB.getMin())
	 */
	public static SeqSymmetry getUnion(List<SeqSymmetry> syms, BioSeq seq)  {
		//    MutableSeqSymmetry psym = new SimpleSymWithProps();
		// first get the landscape as a GraphSym
		GraphSym landscape = getSymmetrySummary(syms, seq, true, null);
		// now just flatten it
		if (landscape != null) {
			return projectLandscape(landscape);
		} else {
			return null;
		}
	}



	/**
	 *  Finds the Intersection of a List of SeqSymmetries.
	 */
	public static SeqSymmetry getIntersection(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq)  {
		MutableSeqSymmetry psym = new SimpleSymWithProps();
		SeqSymmetry unionA = getUnion(symsA, seq);
		SeqSymmetry unionB = getUnion(symsB, seq);
		List<SeqSymmetry> symsAB = new ArrayList<SeqSymmetry>();
		symsAB.add(unionA);
		symsAB.add(unionB);
		GraphSym combo_graph = getSymmetrySummary(symsAB, seq, false, null);
		// combo_graph should now be landscape where:
		//    no coverage ==> depth = 0;
		//    A not B     ==> depth = 1;
		//    B not A     ==> depth = 1;
		//    A && B      ==> depth = 2;

		// so any regions with depth == 2 are intersection
		int xcoords[] = combo_graph.getGraphXCoords();
		//float ycoords[] = (float[]) combo_graph.getGraphYCoords();
		int num_points = xcoords.length;

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = xcoords[i];
			float ypos = combo_graph.getGraphYCoord(i);
			if (in_region) {
				if (ypos < 2) { // reached end of intersection region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
				else {  // still in region, do nothing
				}
			}
			else {  // not already in_region
				if (ypos >= 2) {
					in_region = true;
					current_region_start = xpos;
				}
				else {  // still not in region, so do nothing
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

		//Where does this comment belong?
		/*
		 *  Alternative way to get to combo_graph (should be more efficient, as it avoid
		 *     intermediary creation of symsA union syms, and symsB union syms)
		 *  GraphSym scapeA = getSymmetrySummary(symsA, seq, true);
		 *  GraphSym scapeB = getSymmetrySummary(symsB, seq, true);
		 *  List scapes = new ArrayList();
		 *  scapes.add(scapeA); scapes.add(scapeB);
		 *  GraphSym combo_graph = landscapeSummer(scapes, seq);
		 */
	}

	public static SeqSymmetry getXor(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq) {
		MutableSeqSymmetry psym = new SimpleSymWithProps();
		SeqSymmetry unionA = getUnion(symsA, seq);
		SeqSymmetry unionB = getUnion(symsB, seq);
		List<SeqSymmetry> symsAB = new ArrayList<SeqSymmetry>();
		symsAB.add(unionA);
		symsAB.add(unionB);
		GraphSym combo_graph = getSymmetrySummary(symsAB, seq, false, null);
		// combo_graph should now be landscape where:
		//    no coverage ==> depth = 0;
		//    A not B     ==> depth = 1;
		//    B not A     ==> depth = 1;
		//    A && B      ==> depth = 2;

		// so any regions with depth == 1 are XOR regions
		int xcoords[] = combo_graph.getGraphXCoords();
		//float ycoords[] = (float[]) combo_graph.getGraphYCoords();
		int num_points = xcoords.length;

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = xcoords[i];
			float ypos = combo_graph.getGraphYCoord(i);
			if (in_region) {
				if (ypos < 1 || ypos > 1) { // reached end of xor region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
				else {  // still in region, do nothing
				}
			}
			else {  // not already in_region
				if (ypos == 1) {
					in_region = true;
					current_region_start = xpos;
				}
				else {  // still not in region, so do nothing
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

	/**
	 *  Like a one-sided xor,
	 *  creates a SeqSymmetry that contains children for regions covered by syms in symsA that
	 *     are not covered by syms in symsB.
	 */
	public static SeqSymmetry getExclusive(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq) {
		SeqSymmetry xorSym = getXor(symsA, symsB, seq);
		//  if no spans for xor, then won't be any for one-sided xor either, so return null;
		if (xorSym == null)  { return null; }
		List<SeqSymmetry> xorList = new ArrayList<SeqSymmetry>();
		xorList.add(xorSym);
		SeqSymmetry a_not_b = getIntersection(symsA, xorList, seq);
		return a_not_b;
	}

	public static SeqSymmetry getNot(List<SeqSymmetry> syms, BioSeq seq) {
		return getNot(syms, seq, true);
	}

	public static SeqSymmetry getNot(List<SeqSymmetry> syms, BioSeq seq, boolean include_ends) {
		SeqSymmetry union = getUnion(syms, seq);
		int spanCount = union.getChildCount();

		// rest of this is pretty much pulled directly from SeqUtils.inverse()
		if (! include_ends )  {
			if (spanCount <= 1) {  return null; }  // no gaps, no resulting inversion
		}
		MutableSeqSymmetry invertedSym = new SimpleSymWithProps();
		//    invertedSym.addSpan(new SimpleSeqSpan(pSpan.getStart(), pSpan.getEnd(), seq));
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

}
