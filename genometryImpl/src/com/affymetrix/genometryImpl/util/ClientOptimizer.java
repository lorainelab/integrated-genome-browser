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
import java.util.logging.Logger;

public class ClientOptimizer {
    public static void OptimizeQuery(
			BioSeq aseq, String typeid,
			Das2Type type, String typeName, List<FeatureRequestSym> output_requests, FeatureRequestSym request_sym) {
		// overlap_span and overlap_sym should actually be the same object, a LeafSeqSymmetry
		SeqSymmetry overlap_sym = request_sym.getOverlapSym();
		Das2Region region = null;
		if (request_sym instanceof Das2FeatureRequestSym) {
			region = ((Das2FeatureRequestSym)request_sym).getRegion();
		}
        MutableSeqSymmetry cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(typeid);
        // this should work even for graphs, now that graphs are added to BioSeq's type hash (with id as type)

		// little hack for GraphSyms, need to resolve when to use id vs. name vs. type
		if (cont_sym == null && typeid.endsWith(".bar")) {
			cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(typeName);
			Logger.getLogger(ClientOptimizer.class.getName()).fine(
					"trying to use type name for bar type, name: " + typeName + ", id: " + typeid +
				", cont_sym: " + cont_sym);
        }
        if (cont_sym == null || cont_sym.getChildCount() == 0) {
            Logger.getLogger(ClientOptimizer.class.getName()).info("Can't optimize query, no previous annotations of type: " + typeid);
            output_requests.add(request_sym);
        } else {
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
			if (prev_overlaps.isEmpty()) {
				Logger.getLogger(ClientOptimizer.class.getName()).info("Can't optimize query, no previous requests found: " + typeid);
				output_requests.add(request_sym);
				return;
			}
            SeqSymmetry prev_union = SeqSymSummarizer.getUnion(prev_overlaps, aseq);
            List<SeqSymmetry> qnewlist = new ArrayList<SeqSymmetry>();
            qnewlist.add(overlap_sym);
            List<SeqSymmetry> qoldlist = new ArrayList<SeqSymmetry>();
            qoldlist.add(prev_union);
			SeqSymmetry split_query = SeqSymSummarizer.getExclusive(qnewlist, qoldlist, aseq);
            SplitQuery(split_query, aseq, typeid, prev_union, type, region, output_requests);
        }
    }


    private static void SplitQuery(
			SeqSymmetry split_query, BioSeq aseq,
			String typeid, SeqSymmetry prev_union, Das2Type type, Das2Region region, List<FeatureRequestSym> output_requests) {

        if (split_query == null || split_query.getChildCount() == 0) {
            // all of current query overlap range covered by previous queries, so return empty list
            Logger.getLogger(ClientOptimizer.class.getName()).info("All of new query covered by previous queries for type: " + typeid);
            return;
        }

        SeqSpan split_query_span = split_query.getSpan(aseq);
        Logger.getLogger(ClientOptimizer.class.getName()).fine("split query: " + SeqUtils.symToString(split_query));
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
            Logger.getLogger(ClientOptimizer.class.getName()).fine("new request: " + SeqUtils.spanToString(ispan));
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
