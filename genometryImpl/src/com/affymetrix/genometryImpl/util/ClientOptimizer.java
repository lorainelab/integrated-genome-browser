package com.affymetrix.genometryImpl.util;

import java.util.*;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.comparator.SeqSpanComparator;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.das2.Das2FeatureRequestSym;
import com.affymetrix.genometryImpl.das2.Das2Region;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.general.FeatureRequestSym;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientOptimizer {
	private static boolean DEBUG = false;
    public static void OptimizeQuery(
			BioSeq aseq, URI typeid,
			Das2Type type, String typeName, List<FeatureRequestSym> output_requests, FeatureRequestSym request_sym) {
		if (aseq == null) {
			Logger.getLogger(ClientOptimizer.class.getName()).severe("Chromosome was not selected -- cannot load data");
			return;
		}
        MutableSeqSymmetry cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(typeid.toString());
        // this should work even for graphs, now that graphs are added to BioSeq's type hash (with id as type)

		// little hack for GraphSyms, need to resolve when to use id vs. name vs. type
		if (cont_sym == null && typeid.toString().endsWith(".bar")) {
			cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(typeName);
			Logger.getLogger(ClientOptimizer.class.getName()).log(
					Level.FINE, "trying to use type name for bar type, name: {0}, id: {1}, cont_sym: {2}", new Object[]{typeName, typeid, cont_sym});
        }
        if (cont_sym == null || cont_sym.getChildCount() == 0) {
            Logger.getLogger(ClientOptimizer.class.getName()).log(
					Level.INFO, "Can''t optimize query, no previous annotations of type: {0}", typeid);
            output_requests.add(request_sym);
        } else {
			List<SeqSymmetry> prev_overlaps = determinePreviousRequests(cont_sym);
			if (prev_overlaps.isEmpty()) {
				Logger.getLogger(ClientOptimizer.class.getName()).log(
						Level.INFO, "Can''t optimize query, no previous requests found: {0}", typeid);
				output_requests.add(request_sym);
			} else {
				optimizeRequestVsPreviousRequests(request_sym, prev_overlaps, aseq, typeid, type, output_requests);
			}
		}
    }

	private static List<SeqSymmetry> determinePreviousRequests(MutableSeqSymmetry cont_sym) {
		int prevcount = cont_sym.getChildCount();
		List<SeqSymmetry> prev_overlaps = new ArrayList<SeqSymmetry>(prevcount);
		for (int i = 0; i < prevcount; i++) {
			SeqSymmetry prev_request = cont_sym.getChild(i);
			if (prev_request instanceof FeatureRequestSym) {
				prev_overlaps.add(((FeatureRequestSym) prev_request).getOverlapSym());
			} else if (prev_request instanceof GraphSym) {
				prev_overlaps.add(prev_request);
			}
		}
		return prev_overlaps;
	}

	private static void optimizeRequestVsPreviousRequests(FeatureRequestSym request_sym, List<SeqSymmetry> prev_overlaps, BioSeq aseq, URI typeid, Das2Type type, List<FeatureRequestSym> output_requests) {
		// overlap_span and overlap_sym should actually be the same object, a LeafSeqSymmetry
		SeqSymmetry overlap_sym = request_sym.getOverlapSym();
		Das2Region region = null;
		if (request_sym instanceof Das2FeatureRequestSym) {
			region = ((Das2FeatureRequestSym) request_sym).getRegion();
		}
		SeqSymmetry prev_union = SeqSymSummarizer.getUnion(prev_overlaps, aseq);
		List<SeqSymmetry> qnewlist = new ArrayList<SeqSymmetry>();
		qnewlist.add(overlap_sym);
		List<SeqSymmetry> qoldlist = new ArrayList<SeqSymmetry>();
		qoldlist.add(prev_union);
		SeqSymmetry split_query = SeqSymSummarizer.getExclusive(qnewlist, qoldlist, aseq);
		SplitQuery(split_query, aseq, typeid, prev_union, type, region, output_requests);
	}


    private static void SplitQuery(
			SeqSymmetry split_query, BioSeq aseq,
			URI typeid, SeqSymmetry prev_union, Das2Type type, Das2Region region, List<FeatureRequestSym> output_requests) {

        if (split_query == null || split_query.getChildCount() == 0) {
            // all of current query overlap range covered by previous queries, so return empty list
            Logger.getLogger(ClientOptimizer.class.getName()).log(
					Level.INFO, "All of new query covered by previous queries for type: {0}", typeid);
            return;
        }

        SeqSpan split_query_span = split_query.getSpan(aseq);
		if (DEBUG) {
			Logger.getLogger(ClientOptimizer.class.getName()).log(
					Level.FINE, "split query: {0}", SeqUtils.spanToString(split_query_span));
		}
        // figure out min/max within bounds based on location of previous queries relative to new query
        int first_within_min;
        int last_within_max;
        List<SeqSpan> union_spans = SeqUtils.getLeafSpans(prev_union, aseq);
        SeqSpanComparator spancomp = new SeqSpanComparator();
        // since prev_union was created via SeqSymSummarizer, spans should come out already
        //   sorted by ascending min (and with no overlaps)
        //          Collections.sort(union_spans, spancomp);
        int insert = Collections.binarySearch(union_spans, split_query_span, spancomp);
        if (insert < 0) {
            insert = -insert - 1;
        }
        if (insert == 0) {
            first_within_min = 0;
        } else {
            first_within_min = (union_spans.get(insert - 1)).getMax();
        }
        // since sorted by min, need to make sure that we are at the insert index
        //   at which get(index).min >= exclusive_span.max,
        //   so increment till this (or end) is reached
        while ((insert < union_spans.size()) && ((union_spans.get(insert)).getMin() < split_query_span.getMax())) {
            insert++;
        }
        if (insert == union_spans.size()) {
            last_within_max = aseq.getLength();
        } else {
            last_within_max = (union_spans.get(insert)).getMin();
        }
        // done determining first_within_min and last_within_max
        splitIntoSubSpans(split_query, aseq, first_within_min, last_within_max, type, region, output_requests);
    }

	 private static void splitIntoSubSpans(
            SeqSymmetry split_query, BioSeq aseq, int first_within_min, int last_within_max, Das2Type type,
			Das2Region region, List<FeatureRequestSym> output_requests) {
        int split_count = split_query.getChildCount();
        int cur_within_min;
        int cur_within_max;
        for (int k = 0; k < split_count; k++) {
            SeqSymmetry csym = split_query.getChild(k);
            SeqSpan ospan = csym.getSpan(aseq);
            if (k == 0) {
                cur_within_min = first_within_min;
            } else {
                cur_within_min = ospan.getMin();
            }
            if (k == (split_count - 1)) {
                cur_within_max = last_within_max;
            } else {
                cur_within_max = ospan.getMax();
            }
            SeqSpan ispan = new SimpleSeqSpan(cur_within_min, cur_within_max, aseq);
			if (DEBUG) {
				Logger.getLogger(ClientOptimizer.class.getName()).log(
						Level.FINE, "new inside span: {0}", SeqUtils.spanToString(ispan));
			}
			FeatureRequestSym new_request = null;
			if (region == null) {
				new_request = new FeatureRequestSym(ospan, ispan);
			} else {
				new_request = new Das2FeatureRequestSym(type, region, ospan, ispan);
			}
            output_requests.add(new_request);
        }
    }

}
