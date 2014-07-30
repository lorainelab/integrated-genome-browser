/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometryImpl.symmetry.impl;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.ResiduesChars;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.SeqUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SeqSymSummarizer {

    public static MisMatchGraphSym getMismatchGraph(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int start, int end, boolean pileup) {

        if (syms.isEmpty()) {
            return null;
        }

        int range = end - start;
        int[] y = new int[range];
        int[][] yR = new int[5][range];
        SeqSymmetry sym = syms.get(0);
        SeqSpan span;

        byte[] seq_residues = seq.getResidues(start, end).toLowerCase().getBytes();
        byte[] cur_residues;
        byte ch, intron = "-".getBytes()[0];
        int k, offset, cur_start, cur_end, length, y_offset = 0;

        for (int i = 0; i < sym.getChildCount(); i++) {
            SeqSymmetry childSeqSym = sym.getChild(i);

            if (!(childSeqSym instanceof SymWithResidues)) {
                continue;
            }

            span = childSeqSym.getSpan(seq);
            offset = span.getMin() > start ? span.getMin() - start : 0;

            // Boundary Check
            cur_start = Math.max(start, span.getMin());
            cur_end = Math.min(end, span.getMax());
            length = cur_end - cur_start;

            cur_residues = ((SymWithResidues) childSeqSym).getResidues(cur_start, cur_end).toLowerCase().getBytes();

            for (int j = 0; j < length; j++) {
                ch = cur_residues[j];
                if (seq_residues[offset + j] != ch && ch != intron) {
                    y[offset - y_offset + j] += 1;
                }

                k = ResiduesChars.getValue((char) ch);
                if (k > -1) {
                    yR[k][offset - y_offset + j] += 1;
                }
            }

        }

        MisMatchGraphSym summary = createMisMatchGraph(range, yR, start, y, id, seq, pileup);
		//summary.getGraphState().setGraphStyle(GraphType.FILL_BAR_GRAPH);

        //Request Garbage Collection
        System.gc();

        return summary;
    }

    private static MisMatchGraphSym createMisMatchGraph(int range, int[][] yR, int start, int[] y, String id, BioSeq seq, boolean pileup) {
        MisMatchGraphSym summary;
        IntArrayList _x = new IntArrayList(range);
        FloatArrayList _y = new FloatArrayList(range);
//		IntArrayList _w = new IntArrayList(range);
        IntArrayList _yA = new IntArrayList(range);
        IntArrayList _yT = new IntArrayList(range);
        IntArrayList _yG = new IntArrayList(range);
        IntArrayList _yC = new IntArrayList(range);
        IntArrayList _yN = new IntArrayList(range);

        for (int i = 0; i < range; i++) {
            if (yR[0][i] > 0 || yR[1][i] > 0 || yR[2][i] > 0 || yR[3][i] > 0 || yR[4][i] > 0) {
                _x.add(start + i);
                _y.add(y[i]);
//				_w.add(1);

                _yA.add(yR[0][i]);
                _yT.add(yR[1][i]);
                _yG.add(yR[2][i]);
                _yC.add(yR[3][i]);
                _yN.add(yR[4][i]);
            }
        }
        _x.trimToSize();
        _y.trimToSize();
//		_w.trimToSize();
        _yA.trimToSize();
        _yT.trimToSize();
        _yG.trimToSize();
        _yC.trimToSize();
        _yN.trimToSize();

        summary = pileup
                ? new MisMatchPileupGraphSym(_x.elements(), null/*_w.elements()*/, _y.elements(),
                        _yA.elements(), _yT.elements(), _yG.elements(), _yC.elements(), _yN.elements(),
                        id, seq)
                : new MisMatchGraphSym(_x.elements(), null/*_w.elements()*/, _y.elements(),
                        _yA.elements(), _yT.elements(), _yG.elements(), _yC.elements(), _yN.elements(),
                        id, seq);
        return summary;
    }

    public static GraphIntervalSym getSymmetrySummary(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, Boolean isForward) {
        int symcount = syms.size();
        List<SeqSpan> leaf_spans = new ArrayList<SeqSpan>(symcount);
        for (SeqSymmetry sym : syms) {
            SeqUtils.collectLeafSpans(sym, seq, isForward, leaf_spans);
        }
        if (leaf_spans.isEmpty()) {
            return null;
        } else {
            return getSpanSummary(leaf_spans, binary_depth, id);
        }
    }

    /**
     * Makes a summary graph of a set the spans of some SeqSymmetries on a given
     * BioSeq. Descends into parent's descendants, collecting all leaf
     * symmetries and creating a summary over the leafs. Currently assumes that
     * spans are integral.
     * <pre>
     *  Performance: ~ n log(n) ?   where n is number of spans in the syms
     *      a.) collect leaf spans, ~linear scan (n)
     *      b.) sort span starts and ends, ~(n)(log(n))
     *      c.) get transitions, linear scan (n)
     * </pre>
     *
     * @param syms a List of SeqSymmetry's
     * @param seq the sequence you want the summary computed for
     * @param binary_depth passed through to
     * {@link #getSpanSummary(List, boolean, String)}
     */
    public static GraphIntervalSym getSymmetrySummary(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id) {
        int symcount = syms.size();
        List<SeqSpan> leaf_spans = new ArrayList<SeqSpan>(symcount);
        for (SeqSymmetry sym : syms) {
            SeqUtils.collectLeafSpans(sym, seq, leaf_spans);
        }
        if (leaf_spans.isEmpty()) {
            return null;
        } else {
            return getSpanSummary(leaf_spans, binary_depth, id);
        }
    }

    public static GraphIntervalSym getSymmetrySummary(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int desired_leaf_depth) {
        int symcount = syms.size();
        List<SeqSpan> leaf_spans = new ArrayList<SeqSpan>(symcount);
        for (SeqSymmetry sym : syms) {
            SeqUtils.collectSpans(sym, seq, leaf_spans, desired_leaf_depth);
        }
        if (leaf_spans.isEmpty()) {
            return null;
        } else {
            return getSpanSummary(leaf_spans, binary_depth, id);
        }
    }

    /**
     * GetSpanSummary. General idea is that this will make getUnion(),
     * getIntersection(), etc. easier and more efficient.
     *
     * @param spans a List of SeqSpan's all defined on the same BioSeq
     * @param binary_depth if false, then return a graph with full depth
     * information if true, then return a graph with flattened / binary depth
     * information, 1 for covered, 0 for not covered
     */
    private static GraphIntervalSym getSpanSummary(List<SeqSpan> spans, boolean binary_depth, String gid) {
        BioSeq seq = spans.get(0).getBioSeq();
        int span_num = spans.size();
        int[] starts = new int[span_num];
        int[] stops = new int[span_num];
        for (int i = 0; i < span_num; i++) {
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
        IntArrayList transition_xpos = new IntArrayList(span_num * 2);
        FloatArrayList transition_ypos = new FloatArrayList(span_num * 2);

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
                    prev_depth = 1;
                } else if ((prev_depth > 0) && (depth <= 0)) {
                    transition_xpos.add(next_transition);
                    transition_ypos.add(0);
                    prev_depth = 0;
                }
            } else {
                transition_xpos.add(next_transition);
                transition_ypos.add(depth);
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
                    prev_depth = 1;
                } else if ((prev_depth > 0) && (depth <= 0)) {
                    transition_xpos.add(next_transition);
                    transition_ypos.add(0);
                    prev_depth = 0;
                }
            } else {
                transition_xpos.add(next_transition);
                transition_ypos.add(depth);
                max_depth = Math.max(depth, max_depth);
            }
        }
        transition_xpos.trimToSize();
        transition_ypos.trimToSize();
        int[] x_positions = transition_xpos.elements();
        int[] widths = new int[x_positions.length];
        for (int i = 0; i < widths.length - 1; i++) {
            widths[i] = x_positions[i + 1] - x_positions[i];
        }
        widths[widths.length - 1] = 1;

		// Originally, this returned a GraphSym with just x and y, but now has widths.
        // Since the x and y values are not changed, all old code that relies on them
        // does not need to change.
        String uid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq);
        GraphIntervalSym gsym
                = new GraphIntervalSym(x_positions, widths, transition_ypos.elements(), uid, seq);
        return gsym;
    }

    /**
     * Assumes all spans refer to same BioSeq
     */
    public static List<SeqSpan> getMergedSpans(List<SeqSpan> spans) {
        GraphSym landscape = getSpanSummary(spans, true, "");
        return projectLandscapeSpans(landscape);
    }

    private static List<SeqSpan> projectLandscapeSpans(GraphSym landscape) {
        List<SeqSpan> spanlist = new ArrayList<SeqSpan>();
        BioSeq seq = landscape.getGraphSeq();
        int num_points = landscape.getPointCount();

        int current_region_start = 0;
        int current_region_end = 0;
        boolean in_region = false;
        for (int i = 0; i < num_points; i++) {
            int xpos = landscape.getGraphXCoord(i);
            float ypos = landscape.getGraphYCoord(i);
            if (in_region) {
                if (ypos <= 0) { // reached end of region, make SeqSpan
                    in_region = false;
                    current_region_end = xpos;
                    SeqSpan newspan = new SimpleSeqSpan(current_region_start, current_region_end, seq);
                    spanlist.add(newspan);
                }
            } else {  // not already in_region
                if (ypos > 0) {
                    in_region = true;
                    current_region_start = xpos;
                }
            }
        }
        if (in_region) {  // last point was still in_region, so make a span to end?
            // pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
            System.err.println("still in a covered region at end of projectLandscapeSpans() loop!");
        }
        return spanlist;
    }

    private static SymWithProps projectLandscape(GraphSym landscape) {
        BioSeq seq = landscape.getGraphSeq();
        SimpleSymWithProps psym = new SimpleSymWithProps();
        int num_points = landscape.getPointCount();

        int current_region_start = 0;
        int current_region_end = 0;
        boolean in_region = false;
        for (int i = 0; i < num_points; i++) {
            int xpos = landscape.getGraphXCoord(i);
            float ypos = landscape.getGraphYCoord(i);
            if (in_region) {
                if (ypos <= 0) { // reached end of region, make SeqSpan
                    in_region = false;
                    current_region_end = xpos;
                    SeqSymmetry newsym
                            = new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
                    psym.addChild(newsym);
                }
            } else {  // not already in_region
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
        } else {
            // landscape is already sorted, so should be able to derive parent min and max
            int pmin = psym.getChild(0).getSpan(seq).getMin();
            int pmax = psym.getChild(psym.getChildCount() - 1).getSpan(seq).getMax();
            SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
            psym.addSpan(pspan);
        }
        return psym;
    }

    /**
     * Finds the Union of a List of SeqSymmetries. This will merge not only
     * overlapping syms but also abutting syms (where symA.getMax() ==
     * symB.getMin())
     */
    public static SeqSymmetry getUnion(List<SeqSymmetry> syms, BioSeq seq) {
		//    MutableSeqSymmetry psym = new SimpleSymWithProps();
        // first get the landscape as a GraphSym
        GraphSym landscape = getSymmetrySummary(syms, seq, true, "");
        // now just flatten it
        if (landscape != null) {
            return projectLandscape(landscape);
        } else {
            return null;
        }
    }

    public static SeqSymmetry getUnion(List<SeqSymmetry> syms, BioSeq seq, int depth) {
		//    MutableSeqSymmetry psym = new SimpleSymWithProps();
        // first get the landscape as a GraphSym
        GraphSym landscape = getSymmetrySummary(syms, seq, true, "", depth);
        // now just flatten it
        if (landscape != null) {
            return projectLandscape(landscape);
        } else {
            return null;
        }
    }

    /**
     * Finds the Intersection of a List of SeqSymmetries.
     */
    public static SeqSymmetry getIntersection(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq) {
        MutableSeqSymmetry psym = new SimpleSymWithProps();
        SeqSymmetry unionA = getUnion(symsA, seq);
        SeqSymmetry unionB = getUnion(symsB, seq);
        List<SeqSymmetry> symsAB = new ArrayList<SeqSymmetry>();
        symsAB.add(unionA);
        symsAB.add(unionB);
        GraphSym combo_graph = getSymmetrySummary(symsAB, seq, false, "");
		// combo_graph should now be landscape where:
        //    no coverage ==> depth = 0;
        //    A not B     ==> depth = 1;
        //    B not A     ==> depth = 1;
        //    A && B      ==> depth = 2;

        // so any regions with depth == 2 are intersection
        int num_points = combo_graph.getPointCount();

        int current_region_start = 0;
        int current_region_end = 0;
        boolean in_region = false;
        for (int i = 0; i < num_points; i++) {
            int xpos = combo_graph.getGraphXCoord(i);
            float ypos = combo_graph.getGraphYCoord(i);
            if (in_region) {
                if (ypos < 2) { // reached end of intersection region, make SeqSpan
                    in_region = false;
                    current_region_end = xpos;
                    SeqSymmetry newsym
                            = new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
                    psym.addChild(newsym);
                }
            } else {  // not already in_region
                if (ypos >= 2) {
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
        } else {
            // landscape is already sorted, so should be able to derive parent min and max
            int pmin = psym.getChild(0).getSpan(seq).getMin();
            int pmax = psym.getChild(psym.getChildCount() - 1).getSpan(seq).getMax();
            SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
            psym.addSpan(pspan);
        }
        return psym;
    }

    /**
     * Finds the Intersection of a List of SeqSymmetries.
     */
    public static SeqSymmetry getIntersection(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq, int depth) {
        MutableSeqSymmetry psym = new SimpleSymWithProps();
        SeqSymmetry unionA = getUnion(symsA, seq, depth);
        SeqSymmetry unionB = getUnion(symsB, seq, depth);
        List<SeqSymmetry> symsAB = new ArrayList<SeqSymmetry>();
        symsAB.add(unionA);
        symsAB.add(unionB);
        GraphSym combo_graph = getSymmetrySummary(symsAB, seq, false, "", depth);
		// combo_graph should now be landscape where:
        //    no coverage ==> depth = 0;
        //    A not B     ==> depth = 1;
        //    B not A     ==> depth = 1;
        //    A && B      ==> depth = 2;

        // so any regions with depth == 2 are intersection
        int num_points = combo_graph.getPointCount();

        int current_region_start = 0;
        int current_region_end = 0;
        boolean in_region = false;
        for (int i = 0; i < num_points; i++) {
            int xpos = combo_graph.getGraphXCoord(i);
            float ypos = combo_graph.getGraphYCoord(i);
            if (in_region) {
                if (ypos < 2) { // reached end of intersection region, make SeqSpan
                    in_region = false;
                    current_region_end = xpos;
                    SeqSymmetry newsym
                            = new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
                    psym.addChild(newsym);
                }
            } else {  // not already in_region
                if (ypos >= 2) {
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
        } else {
            // landscape is already sorted, so should be able to derive parent min and max
            int pmin = psym.getChild(0).getSpan(seq).getMin();
            int pmax = psym.getChild(psym.getChildCount() - 1).getSpan(seq).getMax();
            SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
            psym.addSpan(pspan);
        }
        return psym;
    }

    public static GraphSym getSymmetryStartSummary(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int desired_leaf_depth) {
        int symcount = syms.size();
        List<SeqSpan> leaf_spans = new ArrayList<SeqSpan>(symcount);
        for (SeqSymmetry sym : syms) {
            SeqUtils.collectSpans(sym, seq, leaf_spans, desired_leaf_depth);
        }
        if (leaf_spans.isEmpty()) {
            return null;
        } else {
            return getSpanStartSummary(leaf_spans, binary_depth, id);
        }
    }

    /**
     * GetSpanSummary. General idea is that this will make getUnion(),
     * getIntersection(), etc. easier and more efficient.
     *
     * @param spans a List of SeqSpan's all defined on the same BioSeq
     * @param binary_depth if false, then return a graph with full depth
     * information if true, then return a graph with flattened / binary depth
     * information, 1 for covered, 0 for not covered
     */
    private static GraphSym getSpanStartSummary(List<SeqSpan> spans, boolean binary_depth, String gid) {
        BioSeq seq = spans.get(0).getBioSeq();
        int span_num = spans.size();
        int[] starts = new int[span_num];
        for (int i = 0; i < span_num; i++) {
            SeqSpan span = spans.get(i);
            starts[i] = span.isForward() ? span.getStart() : span.getStart() - 1;
        }
        Arrays.sort(starts);
        int starts_index = 0;
        int depth = 0;

		// initializing capacity of sum_starts and sum_stops to max that could theoretically be
        //   needed, though likely won't fill it
        IntArrayList transition_xpos = new IntArrayList(span_num * 2);
        FloatArrayList transition_ypos = new FloatArrayList(span_num * 2);

        int prev_depth = 0;
        while (starts_index < span_num) {
            // figure out whether next position is a start, stop, or both
            int next_start = starts[starts_index];
            depth = 0;
			// note that by design, if (next_start == next_stop), then both of the following
            //    conditionals will execute:
            while ((starts_index < span_num) && (starts[starts_index] == next_start)) {
                depth++;
                starts_index++;
            }

            if (binary_depth) {
                if ((prev_depth <= 0) && (depth > 0)) {
                    transition_xpos.add(next_start);
                    transition_ypos.add(1);
                    prev_depth = 1;
                } else if ((prev_depth > 0) && (depth <= 0)) {
                    transition_xpos.add(next_start);
                    transition_ypos.add(0);
                    prev_depth = 0;
                }
            } else {
                transition_xpos.add(next_start);
                transition_ypos.add(depth);
            }
        }
        transition_xpos.trimToSize();
        transition_ypos.trimToSize();

        String uid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq);

        return new GraphSym(transition_xpos.elements(), transition_ypos.elements(), uid, seq);
    }
}
