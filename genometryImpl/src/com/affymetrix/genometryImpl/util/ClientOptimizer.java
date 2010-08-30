package com.affymetrix.genometryImpl.util;

import java.util.*;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.GraphSym;
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

	public static final Map<URI,String> uri2type = new HashMap<URI,String>();
	// TODO: HACK for 6.3


    public static void OptimizeQuery(
			BioSeq aseq, URI typeid,
			Das2Type type, String typeName, List<FeatureRequestSym> output_requests, FeatureRequestSym request_sym) {
		if (aseq == null) {
			Logger.getLogger(ClientOptimizer.class.getName()).severe("Chromosome was not selected -- cannot load data");
			return;
		}
		MutableSeqSymmetry cont_sym = null;
		if (!(request_sym instanceof Das2FeatureRequestSym)) {
			// TODO: HACK for 6.3
			cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(uri2type.get(typeid));
		} else {
			cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(typeid.toString());
		}
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
				optimizeRequestVsPreviousRequests(request_sym, prev_overlaps, aseq, type, output_requests);
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

	private static void optimizeRequestVsPreviousRequests(FeatureRequestSym request_sym, List<SeqSymmetry> prev_overlaps, BioSeq aseq, Das2Type type, List<FeatureRequestSym> output_requests) {
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
		SplitQuery(split_query, aseq, type, region, output_requests);
	}


    private static void SplitQuery(
			SeqSymmetry split_query, BioSeq aseq,
			Das2Type type, Das2Region region, List<FeatureRequestSym> output_requests) {

		if (split_query == null || split_query.getChildCount() == 0) {
			// all of current query overlap range covered by previous queries, so return empty list
			Logger.getLogger(ClientOptimizer.class.getName()).log(
					Level.INFO, "All of new query covered by previous queries");
			return;
		}

        SeqSpan split_query_span = split_query.getSpan(aseq);
		if (DEBUG) {
			Logger.getLogger(ClientOptimizer.class.getName()).log(
					Level.FINE, "split query: {0}", SeqUtils.spanToString(split_query_span));
		}

        int split_count = split_query.getChildCount();
        for (int k = 0; k < split_count; k++) {
            SeqSymmetry csym = split_query.getChild(k);
            SeqSpan ospan = csym.getSpan(aseq);
			FeatureRequestSym new_request = null;
			if (region == null) {
				new_request = new FeatureRequestSym(ospan);
			} else {
				new_request = new Das2FeatureRequestSym(type, region, ospan);
			}
            output_requests.add(new_request);
        }
    }

}
