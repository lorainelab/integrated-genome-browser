/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
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
package com.affymetrix.igb.das2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.CompositeGraphSym;
import com.affymetrix.genometryImpl.SeqSpanComparator;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.parsers.*;
import com.affymetrix.genometryImpl.das2.Das2RequestLog;
import com.affymetrix.genoviz.util.GeneralUtils;

/*
 * Desired optimizations:
 *
 *      Split up by range // not really an optmization, but necessary for other optimizations
 *   0. Split up by type  // not really an optmization, but necessary for other optimizations
 *   1. Format selection
 *   2. overlap with prior query filter
 *   3. whole-sequence-based persistent caching???
 *   4. addition of containment constraints to ensure uniqueness
 *   4. full persistent caching based on (2)
 *
// Das2MultiRangeSplitter
// Das2MultiTypeSplitter // can be applied to any Das2 feature query?
// Das2FormatOptimizer
// Das2OverlapOptimizer
// etc.
 *  Chain together with a CompositeOptimizer?
 *
 */
import com.affymetrix.igb.util.LocalUrlCacher;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class Das2ClientOptimizer {

    //static boolean USE_SEGMENT = Das2Region.USE_SEGMENT;
    //static boolean USE_TYPE_URI = Das2Region.USE_TYPE_URI;
    //static boolean USE_SEGMENT_URI = Das2Region.USE_SEGMENT_URI;
    //static boolean URL_ENCODE_QUERY = Das2Region.URL_ENCODE_QUERY;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_HEADERS = false;
    private static final boolean OPTIMIZE_FORMAT = true;
    private static final boolean SHOW_DAS_QUERY_GENOMETRY = false;
    private static final String UTF8 = "UTF-8";
    /**
     *  For DAS/2 version >= 300, the segment part of location-based feature filters is split
     *  out into a separate query field, "segment", that applies to all location-based filters in the query
     *  (overlaps, inside, ??)
     */
    //static boolean SEPARATE_SEGMENT_FILTER = false;
    private static final String default_format = "das2feature";
    
		/*private static final String PREF_SHOW_DAS_QUERY_GENOMETRY = "SHOW_DAS_QUERY_GENOMETRY";
		private static final boolean default_show_das_query_genometry = false;
    static {
        SHOW_DAS_QUERY_GENOMETRY =
                UnibrowPrefsUtil.getTopNode().getBoolean(PREF_SHOW_DAS_QUERY_GENOMETRY,
                default_show_das_query_genometry);
    }*/

    // input is a single Das2FeatureRequestSym
    // output is List of _optimized_ Das2FeatureRequestSyms that are equivalent to input request,
    //    based on current state of SingletonGenometryModel/AnnotatedSeqGroup/SmartAnnotBioSeq
    //    annotations --
    //    Could this strategy handle persistent caching??   Would need to redirect to
    //      file:// URLs on disk, but how to deal with proper setting of headers???
    //      would need to have additional info persisted on disk with header info...
    //      but for general case, need more info on disk anyway, to quickly find bounds
    //
    // also attaches
    // assume for now one type, one overlap span
    public static List<Das2FeatureRequestSym> loadFeatures(Das2FeatureRequestSym request_sym) {
        Das2RequestLog request_log = request_sym.getLog();

        //    request_log.addLogMessage("called Das2ClientOptimizer.loadFeatures()");
        //  public static List optimizeFeatureRequests(List input_requests) {
        List<Das2FeatureRequestSym> output_requests = new ArrayList<Das2FeatureRequestSym>();
        // overlap_span and overlap_sym should actually be the same object, a LeafSeqSymmetry
        SeqSymmetry overlap_sym = request_sym.getOverlapSym();
        SeqSpan overlap_span = request_sym.getOverlapSpan();
        MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq) overlap_span.getBioSeq();
        Das2Region region = request_sym.getRegion();
        Das2Type type = request_sym.getDas2Type();
        String typeid = type.getID();
        //int omin = overlap_span.getMin();
        //int omax = overlap_span.getMax();
        SeqSymmetry split_query = null;

        if (!(seq instanceof SmartAnnotBioSeq)) {
            request_log.addLogMessage("Can't optimize DAS/2 query for type: " + typeid + ", seq is NOT a SmartAnnotBioSeq!");
            output_requests.add(request_sym);
        } else {
            split_query = OptimizeDas2Query(seq, typeid, request_log, type, output_requests, request_sym, overlap_sym, region);
        }

        for (Das2FeatureRequestSym request : output_requests) {
            optimizedLoadFeatures(request);
        }

        if (SHOW_DAS_QUERY_GENOMETRY) {
            SimpleSymWithProps query_sym = new SimpleSymWithProps();
            query_sym.setProperty("method", ("das_raw_query: " + typeid));
            //      query_sym.addSpan(overlap_span);
            SeqUtils.copyToMutable(overlap_sym, query_sym);
            synchronized (seq) {
                seq.addAnnotation(query_sym);
            }
            if (split_query == null) {
                split_query = query_sym;
            }
            SimpleSymWithProps split_sym = new SimpleSymWithProps();
            SeqUtils.copyToMutable(split_query, split_sym);
            split_sym.setProperty("method", ("das_optimized_query:" + typeid));
            synchronized (seq) {
                seq.addAnnotation(split_sym);
            }
        }

        return output_requests;
    }


    private static SeqSymmetry OptimizeDas2Query(MutableAnnotatedBioSeq seq, String typeid, Das2RequestLog request_log, Das2Type type, List<Das2FeatureRequestSym> output_requests, Das2FeatureRequestSym request_sym, SeqSymmetry overlap_sym, Das2Region region) {
        SeqSymmetry split_query = null;
        SmartAnnotBioSeq aseq = (SmartAnnotBioSeq) seq;
        MutableSeqSymmetry cont_sym;
        // this should work even for graphs, now that graphs are added to SmartAnnotBioSeq's type hash (with id as type)
        cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(typeid);
        // little hack for GraphSyms, need to resolve when to use id vs. name vs. type
        if (cont_sym == null && typeid.endsWith(".bar")) {
            if (DEBUG) {
                request_log.addLogMessage("trying to use type name for bar type, name: " + type.getName() + ", id: " + typeid);
            }
            cont_sym = (MutableSeqSymmetry) aseq.getAnnotation(type.getName());
            if (DEBUG) {
                request_log.addLogMessage("cont_sym: " + cont_sym);
            }
        }
        if ((cont_sym == null) || (cont_sym.getChildCount() == 0)) {
            if (DEBUG) {
                request_log.addLogMessage("Can't optimize DAS/2 query, no previous annotations of type: " + typeid);
            }
            output_requests.add(request_sym);
        } else {
            int prevcount = cont_sym.getChildCount();
            if (DEBUG) {
                request_log.addLogMessage("  child count: " + prevcount);
            }
            ArrayList<SeqSymmetry> prev_overlaps = new ArrayList<SeqSymmetry>(prevcount);
            for (int i = 0; i < prevcount; i++) {
                SeqSymmetry prev_request = cont_sym.getChild(i);
                if (prev_request instanceof Das2FeatureRequestSym) {
                    prev_overlaps.add(((Das2FeatureRequestSym) prev_request).getOverlapSym());
                } else if (prev_request instanceof GraphSym) {
                    prev_overlaps.add(prev_request);
                }
            }
            SeqSymmetry prev_union = SeqSymSummarizer.getUnion(prev_overlaps, aseq);
            ArrayList<SeqSymmetry> qnewlist = new ArrayList<SeqSymmetry>();
            qnewlist.add(overlap_sym);
            ArrayList<SeqSymmetry> qoldlist = new ArrayList<SeqSymmetry>();
            qoldlist.add(prev_union);
            split_query = SplitQuery(qnewlist, qoldlist, aseq, split_query, request_log, typeid, prev_union, type, region, output_requests);
            //	output_requests.add(request_sym);
        }
        return split_query;
    }


    private static SeqSymmetry SplitQuery(ArrayList<SeqSymmetry> qnewlist, ArrayList<SeqSymmetry> qoldlist, SmartAnnotBioSeq aseq, SeqSymmetry split_query, Das2RequestLog request_log, String typeid, SeqSymmetry prev_union, Das2Type type, Das2Region region, List<Das2FeatureRequestSym> output_requests) {
        split_query = SeqSymSummarizer.getExclusive(qnewlist, qoldlist, aseq);
        if (split_query == null || split_query.getChildCount() == 0) {
            // all of current query overlap range covered by previous queries, so return empty list
            if (DEBUG) {
                request_log.addLogMessage("ALL OF NEW QUERY COVERED BY PREVIOUS QUERIES FOR TYPE: " + typeid);

            }
            return split_query;
        }

        SeqSpan split_query_span = split_query.getSpan(aseq);
        if (DEBUG) {
            request_log.addLogMessage("DAS/2 optimizer, split query: " + SeqUtils.symToString(split_query));
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
        splitIntoSubSpans(split_query, aseq, first_within_min, last_within_max, request_log, type, region, output_requests, typeid);
        
        //	output_requests.add(request_sym);
        return split_query;
    }


    private static void splitIntoSubSpans(
            SeqSymmetry split_query, SmartAnnotBioSeq aseq, int first_within_min, int last_within_max, Das2RequestLog request_log, Das2Type type, Das2Region region, List<Das2FeatureRequestSym> output_requests, String typeid) {
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
                request_log.addLogMessage("   new request: " + SeqUtils.spanToString(ispan));
            }
            Das2FeatureRequestSym new_request = new Das2FeatureRequestSym(type, region, ospan, ispan);
            output_requests.add(new_request);
            if (SHOW_DAS_QUERY_GENOMETRY) {
                SimpleSymWithProps within_swp = new SimpleSymWithProps();
                within_swp.addSpan(new SimpleSeqSpan(cur_within_min, cur_within_max, aseq));
                within_swp.addChild(new SingletonSeqSymmetry(cur_within_min, cur_within_min, aseq));
                within_swp.addChild(csym);
                within_swp.addChild(new SingletonSeqSymmetry(cur_within_max, cur_within_max, aseq));
                within_swp.setProperty("method", "das_within_query:" + typeid);
                synchronized (aseq) {
                    aseq.addAnnotation(within_swp);
                }
            }
        }
    }



    static Das2RequestLog optimizedLoadFeatures(Das2FeatureRequestSym request_sym) {
        SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

        Das2RequestLog request_log = request_sym.getLog();
        request_log.setSuccess(true);

        Das2Region region = request_sym.getRegion();
        SeqSpan overlap_span = request_sym.getOverlapSpan();
        SeqSpan inside_span = request_sym.getInsideSpan();
        String overlap_filter = null;
        String inside_filter = null;
        if (Das2Region.USE_SEGMENT) {
            overlap_filter = Das2FeatureSaxParser.getRangeString(overlap_span, false);
            if (inside_span != null) {
                inside_filter = Das2FeatureSaxParser.getRangeString(inside_span, false);
            }
        } else {
            overlap_filter = region.getPositionString(overlap_span, false);
            if (inside_span != null) {
                inside_filter = region.getPositionString(inside_span, false);
            }
        }
        if (DEBUG) {
            request_log.addLogMessage("^^^^^^^  in Das2ClientOptimizer.optimizedLoadFeatures(), overlap = " + overlap_filter +
                    ", inside = " + inside_filter);
        }
        Das2Type type = request_sym.getDas2Type();
        String format = request_sym.getFormat();
        // if format already specified in Das2FeatureRequestSym, don't optimize
        if (OPTIMIZE_FORMAT && (format == null)) {
            format = FormatPriorities.getFormat(type);
            request_sym.setFormat(format);
        }

        MutableAnnotatedBioSeq aseq = region.getAnnotatedSeq();
        Das2VersionedSource versioned_source = region.getVersionedSource();
        AnnotatedSeqGroup seq_group = versioned_source.getGenome();
        Das2Source source = versioned_source.getSource();
        //    String version_url = source.getServerInfo().getRootUrl() + "/" +
        //      versioned_source.getID();

        Das2Capability featcap = versioned_source.getCapability(Das2VersionedSource.FEATURES_CAP_QUERY);
        String request_root = featcap.getRootURI().toString();

        if (DEBUG) {
            request_log.addLogMessage("   request root: " + request_root);
            request_log.addLogMessage("   preferred format: " + format);
        }

        try {
            String query_part = DetermineQueryPart(region, overlap_filter, inside_filter, type, format);

            if (format == null) {
                format = default_format;
            }


            String feature_query = request_root + "?" + query_part;
            if (DEBUG) {
                request_log.addLogMessage("feature query URL:  " + feature_query);
                request_log.addLogMessage("url-encoded query URL:  " + URLEncoder.encode(feature_query, UTF8));
                request_log.addLogMessage("url-decoded query:  " + URLDecoder.decode(feature_query, UTF8));

            }
            boolean success = LoadFeaturesFromQuery(overlap_span, aseq, feature_query, format, request_log, seq_group, type, gmodel, request_sym);
            request_log.setSuccess(success);
        } catch (Exception ex) {
//			ex.printStackTrace();
            request_log.setSuccess(false);
            request_log.setException(ex);
        }
        return request_log;
    }

    private static String DetermineQueryPart(Das2Region region, String overlap_filter, String inside_filter, Das2Type type, String format) throws UnsupportedEncodingException {
        StringBuffer buf = new StringBuffer(200);
        if (Das2Region.USE_SEGMENT) {
            buf.append("segment=");
            if (Das2Region.USE_SEGMENT_URI) {
                buf.append(URLEncoder.encode(region.getID(), UTF8));
            } else {
                buf.append(URLEncoder.encode(region.getName(), UTF8));
            }
            buf.append(";");
        }

        buf.append("overlaps=");
        buf.append(URLEncoder.encode(overlap_filter, UTF8));
        buf.append(";");
        if (inside_filter != null) {
            buf.append("inside=");
            buf.append(URLEncoder.encode(inside_filter, UTF8));
            buf.append(";");
        }
        buf.append("type=");
        if (Das2Region.USE_TYPE_URI) {
            buf.append(URLEncoder.encode(type.getID(), UTF8));
        } else {
            buf.append(type.getName());
        }
        if (OPTIMIZE_FORMAT && format != null) {
            buf.append(";");
            buf.append("format=");
            buf.append(URLEncoder.encode(format, UTF8));
        }
        String query_part = buf.toString();

        return query_part;
    }

    private static boolean LoadFeaturesFromQuery(
            SeqSpan overlap_span, MutableAnnotatedBioSeq aseq, String feature_query, String format, Das2RequestLog request_log,
            AnnotatedSeqGroup seq_group, Das2Type type, SingletonGenometryModel gmodel, Das2FeatureRequestSym request_sym)
            throws SAXException, IOException, IOException {

        /**
         *  Need to look at content-type of server response
         */
        BufferedInputStream bis = null;
        InputStream istr = null;
        String content_subtype = null;
        
        try {
            // if overlap_span is entire length of sequence, then check for caching
            if ((overlap_span.getMin() == 0) && (overlap_span.getMax() == aseq.getLength())) {
                //	LocalUrlCacher.getInputStream(feature_query, cache_usage, cache_annots);
                istr = LocalUrlCacher.getInputStream(feature_query);
                if (istr == null) {
                    request_log.addLogMessage("Server couldn't be accessed with query " + feature_query);
                    request_log.setSuccess(false);
                    return false;
                }
                bis = new BufferedInputStream(istr);
                // for now, assume that when caching, content type returned is same as content type requested
                content_subtype = format;
            } else {
                URL query_url = new URL(feature_query);
                if (DEBUG) {
                    request_log.addLogMessage("    opening connection " + feature_query);
                }
                // casting to HttpURLConnection, since Das2 servers should be either accessed via either HTTP or HTTPS
                HttpURLConnection query_con = (HttpURLConnection) query_url.openConnection();
                int response_code = query_con.getResponseCode();
                String response_message = query_con.getResponseMessage();

                request_log.setHttpResponse(response_code, response_message);

                if (DEBUG) {
                    request_log.addLogMessage("http response code: " + response_code + ", " + response_message);
                }

                //      Map headers = query_con.getHeaderFields();
                if (DEBUG_HEADERS) {
                    int hindex = 0;
                    while (true) {
                        String val = query_con.getHeaderField(hindex);
                        String key = query_con.getHeaderFieldKey(hindex);
                        if (val == null && key == null) {
                            break;
                        }
                        request_log.addLogMessage("header:   key = " + key + ", val = " + val);
                        hindex++;
                    }
                }

                if (response_code != 200) {
                    request_log.addLogMessage("WARNING, HTTP response code not 200/OK: " + response_code + ", " + response_message);
                }

                if (response_code >= 400 && response_code < 600) {
                    request_log.addLogMessage("Server returned error code, aborting response parsing!");
                    request_log.setSuccess(false);
                    return false;
                } else {
                    // request_log.addLogMessage("    getting content type");
                    String content_type = query_con.getContentType();
                    // request_log.addLogMessage("    getting input stream");
                    istr = query_con.getInputStream();
                    bis = new BufferedInputStream(istr);
                    if (DEBUG) {
                        request_log.addLogMessage("content type: " + content_type);
                    }
                    content_subtype = content_type.substring(content_type.indexOf("/") + 1);
                    int sindex = content_subtype.indexOf(';');
                    if (sindex >= 0) {
                        content_subtype = content_subtype.substring(0, sindex);
                        content_subtype = content_subtype.trim();
                    }
										if (DEBUG) {
											request_log.addLogMessage("content subtype: " + content_subtype);
										}
                    if (content_subtype == null || content_type.equals("unknown") || content_subtype.equals("unknown") || content_subtype.equals("xml") || content_subtype.equals("plain")) {
                        // if content type is not descriptive enough, go by what was requested
                        content_subtype = format;
                    }
                }
            }

            if (request_log.getSuccess()) {
                List feats = DetermineFormatAndParse(content_subtype, request_log, bis, feature_query, seq_group, type, gmodel);
                addSymmetriesAndAnnotations(feats, request_sym, request_log, aseq);
            } // end if (success) conditional
            return request_log.getSuccess();
        } finally {
            GeneralUtils.safeClose(bis);
            GeneralUtils.safeClose(istr);
        }
    }

    private static List DetermineFormatAndParse(
            String content_subtype, Das2RequestLog request_log, BufferedInputStream bis, String feature_query, AnnotatedSeqGroup seq_group,
            Das2Type type, SingletonGenometryModel gmodel)
            throws IOException, SAXException {
        List feats = null;
        if (content_subtype.equals(Das2FeatureSaxParser.FEATURES_CONTENT_SUBTYPE)
                || content_subtype.equals("das2feature")
                || content_subtype.equals("das2xml")
                || content_subtype.startsWith("x-das-feature")) {
            AddParsingLogMessage(request_log, content_subtype);
            Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
            InputSource isrc = new InputSource(bis);
            feats = parser.parse(isrc, feature_query, seq_group, false);
        } else if (content_subtype.equals("bed")) {
			AddParsingLogMessage(request_log, content_subtype);
            BedParser parser = new BedParser();
			feats = parser.parse(bis, gmodel, seq_group, false, type.getID(), false);
        } else if (content_subtype.equals("bgn")) {
            AddParsingLogMessage(request_log, content_subtype);
            BgnParser parser = new BgnParser();
            feats = parser.parse(bis, type.getID(), seq_group, -1, false);
        } else if (content_subtype.equals("bps")) {
            AddParsingLogMessage(request_log, content_subtype);
            DataInputStream dis = new DataInputStream(bis);
            feats = BpsParser.parse(dis, type.getID(), null, seq_group, false, false);
        } else if (content_subtype.equals("brs")) {
            AddParsingLogMessage(request_log, content_subtype);
            BrsParser parser = new BrsParser();
            DataInputStream dis = new DataInputStream(bis);
            feats = parser.parse(dis, type.getID(), seq_group, false);
        } else if (content_subtype.equals("bar")) {
            AddParsingLogMessage(request_log, content_subtype);
            feats = BarParser.parse(bis, gmodel, seq_group, type.getName(), false);
        } else if (content_subtype.equals("bp2")) {
            AddParsingLogMessage(request_log, content_subtype);
            Bprobe1Parser bp1_reader = new Bprobe1Parser();
            // parsing probesets in bp2 format, also adding probeset ids
            feats = bp1_reader.parse(bis, seq_group, false, type.getName(), false);
        } else if (content_subtype.equals("ead")) {
            AddParsingLogMessage(request_log, content_subtype);
            ExonArrayDesignParser parser = new ExonArrayDesignParser();
            feats = parser.parse(bis, seq_group, false, type.getName());
        } else if (content_subtype.equals("gff")) {
            AddParsingLogMessage(request_log, content_subtype);
            GFFParser parser = new GFFParser();
            feats = parser.parse(bis, ".", seq_group, false, false);
        } else if (content_subtype.equals("link.psl")) {
            AddParsingLogMessage(request_log, content_subtype);
            // reference to LoadFileAction.ParsePSL
            PSLParser parser = new PSLParser();
            parser.setIsLinkPsl(true);
            parser.enableSharedQueryTarget(true);
            // annotate _target_ (which is chromosome for consensus annots, and consensus seq for probeset annots
            // why is annotate_target parameter below set to false?
            feats = parser.parse(bis, type.getName(), null, seq_group, null, false, false, false); // do not annotate_other (not applicable since not PSL3)
        } else if (content_subtype.equals("cyt")) {
            AddParsingLogMessage(request_log, content_subtype);
            CytobandParser parser = new CytobandParser();
            feats = parser.parse(bis, seq_group, false);
        } else if (content_subtype.equals("psl")) {
            AddParsingLogMessage(request_log, content_subtype);
            // reference to LoadFileAction.ParsePSL
            PSLParser parser = new PSLParser();
            parser.enableSharedQueryTarget(true);
            // annotate target sequence

            DataInputStream dis = new DataInputStream(bis);
            parser.parse(dis, type.getName(), null, seq_group, null, false, true, false);
            // Note that here we specifically ignore the output of parser.parse, because it's already been added to the seq_group.
            // Otherwise we double-count and add multiple tiers.
        } else {
            request_log.addLogMessage("ABORTING DAS2 FEATURE LOADING, FORMAT NOT RECOGNIZED: " + content_subtype);
            request_log.setSuccess(false);
        }
        return feats;
    }

     private static void AddParsingLogMessage(Das2RequestLog request_log, String content_subtype) {
        request_log.addLogMessage("PARSING " + content_subtype.toUpperCase() + " FORMAT FOR DAS2 FEATURE RESPONSE");
    }

     private static void addSymmetriesAndAnnotations(List feats, Das2FeatureRequestSym request_sym, Das2RequestLog request_log, MutableAnnotatedBioSeq aseq) {
        boolean no_graphs = true;
        if (feats == null || feats.size() == 0) {
            // because many operations will treat empty Das2FeatureRequestSym as a leaf sym, want to
            //    populate with empty sym child/grandchild
            //    [ though a better way might be to have request sym's span on aseq be dependent on children, so
            //       if no children then no span on aseq (though still an overlap_span and inside_span) ]
            SimpleSymWithProps child = new SimpleSymWithProps();
            SimpleSymWithProps grandchild = new SimpleSymWithProps();
            child.addChild(grandchild);
            request_sym.addChild(child);
        } else if (request_log.getSuccess()) {
            // checking success again, could have changed before getting to this point...
            int feat_count = feats.size();
            request_log.addLogMessage("parsed query results, annot count = " + feat_count);
            for (int k = 0; k < feat_count; k++) {
                SeqSymmetry feat = (SeqSymmetry) feats.get(k);
                if (feat instanceof GraphSym) {
                    addChildGraph((GraphSym) feat, request_sym);
                    no_graphs = false; // should either be all graphs or no graphs
                } else {
                    request_sym.addChild(feat);
                }
            }
        }
        // probably want to synchronize on annotated seq, since don't want to add annotations to aseq
        // on one thread when might be rendering based on aseq in event thread...
        // or maybe should just make addAnnotation() a synchronized method
        if (no_graphs) {
            // if graphs, then adding to annotation bioseq is already handled by addChildGraph() method
            synchronized (aseq) {
                aseq.addAnnotation(request_sym);
            }
        }
    }


    /**
     *  Given a child GraphSym, find the appropriate parent [Composite]GraphSym and add child to it
     *
     *  Assumes ids of parent graphs are unique among annotations on seq
     *  Also use Das2FeatureRequestSym overlap span as span for child GraphSym
     *  Uses type URI as graph ID, type name as graph name
     */
    public static void addChildGraph(GraphSym cgraf, Das2FeatureRequestSym request_sym) {
        Das2RequestLog request_log = request_sym.getLog();

        request_log.addLogMessage("adding a child GraphSym to parent graph");
        SmartAnnotBioSeq aseq = (SmartAnnotBioSeq) cgraf.getGraphSeq();
        // check and see if parent graph already exists
        //    String id = cgraf.getGraphName();  // grafs can be retrieved from SmartAnnotBioSeq by treating their ID as type
        //    String id = cgraf.getID();  // grafs can be retrieved from SmartAnnotBioSeq by treating their ID as type
        Das2Type type = request_sym.getDas2Type();
        String id = type.getID();
        String name = type.getName();
        request_log.addLogMessage("   child graph id: " + id);
        request_log.addLogMessage("   child graph name: " + name);
        request_log.addLogMessage("   seq: " + aseq.getID());
        GraphSym pgraf = (GraphSym) aseq.getAnnotation(id);
        if (pgraf == null) {
            request_log.addLogMessage("$$$$ creating new parent composite graph sym");
            //      pgraf = new CompositeGraphSym(new int[0], new float[0], id, aseq);
            //      String compid = GraphSymUtils.getUniqueGraphID(id, aseq);
            // don't need to uniquify ID, since already know it's null (since no sym retrieved from aseq)
            pgraf = new CompositeGraphSym(id, aseq);
            //      pgraf.setGraphName(id);
            pgraf.setGraphName(name);
            aseq.addAnnotation(pgraf);
        }
        // since GraphSyms get a span automatically set to the whole seq when constructed, need to first
        //    remove that span, then add overlap span from Das2FeatureRequestSym
        //    could instead create new span based on start and end xcoord, but for better integration with
        //    rest of Das2ClientOptimizer span of request is preferred
        cgraf.removeSpan(cgraf.getSpan(aseq));
        cgraf.addSpan(request_sym.getOverlapSpan());
        request_log.addLogMessage("   span of child graf: " + SeqUtils.spanToString(cgraf.getSpan(aseq)));
        pgraf.addChild(cgraf);
        //add properties of child to parent		
        pgraf.setProperties(cgraf.getProperties());
    }
}
