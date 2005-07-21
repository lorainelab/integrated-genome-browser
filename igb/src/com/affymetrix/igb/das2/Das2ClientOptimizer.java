package com.affymetrix.igb.das2;

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.InputSource;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.parsers.*;

import com.affymetrix.igb.util.UnibrowPrefsUtil;  // just need for diagnostics
import com.affymetrix.igb.menuitem.DasFeaturesAction2;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SingletonSeqSymmetry;  // just need for diagnostics

/**
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
public class Das2ClientOptimizer {
  static boolean DEBUG_HEADERS = false;
  static boolean OPTIMIZE_FORMAT = true;
  static boolean SHOW_DAS_QUERY_GENOMETRY = false;

  static String default_format = "das2feature";

  static {
    SHOW_DAS_QUERY_GENOMETRY = 
      UnibrowPrefsUtil.getTopNode().getBoolean(DasFeaturesAction2.PREF_SHOW_DAS_QUERY_GENOMETRY,
					       DasFeaturesAction2.default_show_das_query_genometry);
  }

  // input is List of Das2FeatureRequestSyms
  // output is List of _optimized_ Das2FeatureRequestSyms that are equivalent to input List,
  //    based on current state of SingletonGenometryModel/AnnotatedSeqGroup/SmartAnnotBioSeq
  //    annotations --
  //    Could this strategy handle persistent caching??   Would need to redirect to
  //      file:// URLs on disk, but how to deal with proper setting of headers???
  //      would need to have additional info persisted on disk with header info...
  //      but for general case, need more info on disk anyway, to quickly find bounds
  //
  // also attaches
  // assume for now one type, one overlap span
  public static List loadFeatures(Das2FeatureRequestSym request_sym) {
    //  public static List optimizeFeatureRequests(List input_requests) {
    List output_requests = new ArrayList();
    // overlap_span and overlap_sym should actually be the same object, a LeafSeqSymmetry
    SeqSymmetry overlap_sym = request_sym.getOverlapSym();
    SeqSpan overlap_span = request_sym.getOverlapSpan();
    MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)overlap_span.getBioSeq();
    Das2Region region = request_sym.getRegion();
    Das2Type type = request_sym.getDas2Type();
    String typeid = type.getID();
    int omin = overlap_span.getMin();
    int omax = overlap_span.getMax();
    SeqSymmetry split_query = null;

    if (! (seq instanceof SmartAnnotBioSeq)) {
      System.out.println("Can't optimize DAS/2 query for type: " + typeid + ", seq is NOT a SmartAnnotBioSeq!");
      output_requests.add(request_sym);
    }
    else {
      SmartAnnotBioSeq aseq = (SmartAnnotBioSeq)seq;
      MutableSeqSymmetry cont_sym = (MutableSeqSymmetry)aseq.getAnnotation(typeid);

      if ((cont_sym == null) || (cont_sym.getChildCount() == 0)) {
	System.out.println("Can't optimize DAS/2 query, no previous annotations of type: " + typeid);
	output_requests.add(request_sym);
      }

      else {
	int prevcount = cont_sym.getChildCount();
	ArrayList prev_overlaps = new ArrayList(prevcount);
	for (int i=0; i<prevcount; i++) {
	  SeqSymmetry prev_request = cont_sym.getChild(i);
	  if (prev_request instanceof Das2FeatureRequestSym) {
	    prev_overlaps.add(((Das2FeatureRequestSym)prev_request).getOverlapSym());
	  }
	}

	SeqSymmetry prev_union = SeqSymSummarizer.getUnion(prev_overlaps, aseq);
	ArrayList qnewlist = new ArrayList();
	qnewlist.add(overlap_sym);
	ArrayList qoldlist = new ArrayList();
	qoldlist.add(prev_union);
	split_query = SeqSymSummarizer.getExclusive(qnewlist, qoldlist, aseq);
	if (split_query == null || split_query.getChildCount() == 0) {
	  // all of current query overlap range covered by previous queries, so return empty list
	  System.out.println("ALL OF NEW QUERY COVERED BY PREVIOUS QUERIES FOR TYPE: " + typeid);
	}
	else {
	  SeqSpan split_query_span = split_query.getSpan(aseq);
	  System.out.println("DAS/2 optimizer, split query: ");
	  SeqUtils.printSymmetry(split_query);
	  // figure out min/max within bounds based on location of previous queries relative to new query
	  int first_within_min;
	  int last_within_max;
	  java.util.List union_spans = SeqUtils.getLeafSpans(prev_union, aseq);
	  SeqSpanComparator spancomp = new SeqSpanComparator();
	  // since prev_union was created via SeqSymSummarizer, spans should come out already
	  //   sorted by ascending min (and with no overlaps)
	  //          Collections.sort(union_spans, spancomp);
	  int insert = Collections.binarySearch(union_spans, split_query_span, spancomp);
	  if (insert < 0) { insert = -insert -1; }
	  if (insert == 0) { first_within_min = 0; }
	  else { first_within_min = ((SeqSpan)union_spans.get(insert-1)).getMax(); }
	  // since sorted by min, need to make sure that we are at the insert index
	  //   at which get(index).min >= exclusive_span.max,
	  //   so increment till this (or end) is reached
	  while ((insert < union_spans.size()) &&
		 (((SeqSpan)union_spans.get(insert)).getMin() < split_query_span.getMax()))  {
	    insert++;
	  }
	  if (insert == union_spans.size()) { last_within_max = aseq.getLength(); }
	  else { last_within_max = ((SeqSpan)union_spans.get(insert)).getMin(); }
	  // done determining first_within_min and last_within_max

	  int split_count = split_query.getChildCount();
	  if (split_count == 0) { System.out.println("PROBLEM IN DAS2CLIENTOPTIMIZER, SPLIT QUERY HAS NO CHILDREN"); }
	  /*
	  if (split_count == 0 || split_count == 1) {
	    SimpleSymWithProps within_swp = new SimpleSymWithProps();
	    within_swp.addSpan(new SimpleSeqSpan(first_within_min, last_within_max, aseq));
	    within_swp.addChild(new SingletonSeqSymmetry(first_within_min,
							 first_within_min, aseq));
	    if (split_count == 0) { within_swp.addChild(split_query); }
	    else { within_swp.addChild(split_query.getChild(0)); }
	    within_swp.addChild(new SingletonSeqSymmetry(last_within_max,
							 last_within_max, aseq));
	    within_swp.setProperty("method", ("das_within_query:" + long_type));
	    if (SHOW_DAS_QUERY_GENOMETRY) { aseq.addAnnotation(within_swp); }
	  }
	  */
	  else {
	    int cur_within_min;
	    int cur_within_max;
	    for (int k=0; k<split_count; k++) {
	      SeqSymmetry csym = split_query.getChild(k);
	      SeqSpan ospan = csym.getSpan(aseq);
	      if (k == 0) { cur_within_min = first_within_min; }
	      else { cur_within_min = ospan.getMin(); }
	      if (k == (split_count-1)) { cur_within_max = last_within_max; }
	      else { cur_within_max = ospan.getMax(); }

	      SeqSpan ispan = new SimpleSeqSpan(cur_within_min, cur_within_max, aseq);
	      Das2FeatureRequestSym new_request = new Das2FeatureRequestSym(type, region, ospan, ispan);
	      output_requests.add(new_request);

	      SimpleSymWithProps within_swp = new SimpleSymWithProps();
	      within_swp.addSpan(new SimpleSeqSpan(cur_within_min, cur_within_max, aseq));
	      within_swp.addChild(new SingletonSeqSymmetry(cur_within_min,
							   cur_within_min, aseq));
	      within_swp.addChild(csym);
	      within_swp.addChild(new SingletonSeqSymmetry(cur_within_max,
							   cur_within_max, aseq));
	      within_swp.setProperty("method", ("das_within_query:" + typeid));
	      if (SHOW_DAS_QUERY_GENOMETRY) { aseq.addAnnotation(within_swp); }
	    }
	  }
	}
	//	output_requests.add(request_sym);
      }
    }
    for (int i=0; i<output_requests.size(); i++) {
      Das2FeatureRequestSym request = (Das2FeatureRequestSym)output_requests.get(i);
      boolean success = optimizedLoadFeatures(request);
      if (success) {
	// probably want to synchronize on annotated seq, since don't want to add annotations to aseq
	// on one thread when might be rendering based on aseq in event thread...
	//
	// or maybe should make addAnnotation() a synchronized method
	request.getRegion().getAnnotatedSeq().addAnnotation(request);
      }
    }

    if (SHOW_DAS_QUERY_GENOMETRY) {
      SimpleSymWithProps query_sym = new SimpleSymWithProps();
      query_sym.setProperty("method", ("das_raw_query: " + typeid));
      //      query_sym.addSpan(overlap_span);
      SeqUtils.copyToMutable(overlap_sym, query_sym);
      seq.addAnnotation(query_sym);
      if (split_query == null) { split_query = query_sym; }
      SimpleSymWithProps split_sym = new SimpleSymWithProps();
      SeqUtils.copyToMutable(split_query, split_sym);
      split_sym.setProperty("method", ("das_optimized_query:" + typeid));
      seq.addAnnotation(split_sym);
    }

    return output_requests;
  }



  static boolean optimizedLoadFeatures(Das2FeatureRequestSym request_sym) {

    boolean success = true;

    SeqSpan overlap_span = request_sym.getOverlapSpan();
    String overlap_filter = Das2FeatureSaxParser.getPositionString(overlap_span, false, false);
    SeqSpan inside_span = request_sym.getInsideSpan();
    String inside_filter = Das2FeatureSaxParser.getPositionString(inside_span, false, false);
    System.out.println("in Das2Region.getFeatures(), overlap = " + overlap_filter + ", inside = " + inside_filter);
    Das2Type type = request_sym.getDas2Type();
    String format = null;
    if (OPTIMIZE_FORMAT)  {
      format = FormatPriorities.getFormat(type);
    }

    Das2Region region = request_sym.getRegion();
    Das2VersionedSource versioned_source = region.getVersionedSource();
    AnnotatedSeqGroup seq_group = versioned_source.getGenome();
    Das2Source source = versioned_source.getSource();
    String version_url = source.getServerInfo().getRootUrl() + "/" +
      //      getVersionedSource().getSource().getID() + "/" +
      versioned_source.getID();
    System.out.println("   version url: " + version_url);
    System.out.println("   preferred format: " + format);

    StringBuffer buf = new StringBuffer(200);

    buf.append(version_url);
    buf.append("/feature?");
    buf.append("overlaps=");
    buf.append(overlap_filter);
    buf.append(";");
    if (inside_filter != null) {
      buf.append("inside=");
      buf.append(inside_filter);
      buf.append(";");
    }
    buf.append("type=");
    buf.append(type.getID());
    if (OPTIMIZE_FORMAT && format != null) {
      buf.append(";");
      buf.append("format=");
      buf.append(format);
    }

    if (format == null) { format = default_format; }

    String feature_query = buf.toString();
    System.out.println("feature query:  " + feature_query);

    /**
     *  Need to look at content-type of server response
     */
    try {
      URL query_url = new URL(feature_query);
      System.out.println("    opening connection");
      // casting to HttpURLConnection, since Das2 servers should be either accessed via either HTTP or HTTPS
      HttpURLConnection query_con = (HttpURLConnection)query_url.openConnection();
      int response_code = query_con.getResponseCode();
      String response_message = query_con.getResponseMessage();
      //      System.out.println("http response code: " + response_code + ", " + response_message);

      //      Map headers = query_con.getHeaderFields();
      if (DEBUG_HEADERS) {
	int hindex = 0;
	while (true) {
	  String val = query_con.getHeaderField(hindex);
	  String key = query_con.getHeaderFieldKey(hindex);
	  if (val == null && key == null) { break; }
	  System.out.println("header:   key = " + key + ", val = " + val);
	  hindex++;
	}
      }

      if (response_code != 200) {
	System.out.println("WARNING, HTTP response code not 200/OK: " +
			   response_code + ", " + response_message);
      }
      if (response_code >= 400 && response_code < 600) {
	System.out.println("Server returned error code, aborting response parsing!");
	success = false;
      }
      else {
	System.out.println("    getting content type");
	String content_type = query_con.getContentType();
	System.out.println("    getting input stream");
	InputStream istr = query_con.getInputStream();
	BufferedInputStream bis = new BufferedInputStream(istr);
	String content_subtype = content_type.substring(content_type.indexOf("/")+1);
	System.out.println("content subtype: " + content_subtype);
	java.util.List feats = null;
	if (content_type.equals("unknown") ||
	    content_subtype.equals("unknown") ||
	    content_subtype.equals("xml") ||
	    content_subtype.equals("plain") ) {
	  // if content type is not descriptive enough, go by what was requested
	  content_subtype = format;
	}

	if (content_subtype.equals("das2feature") ||
	    content_subtype.startsWith("x-das-feature")) {
	  System.out.println("PARSING DAS2FEATURE FORMAT FOR DAS2 FEATURE RESPONSE");
	  Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
	  feats = parser.parse(new InputSource(bis), seq_group, false);
	}
	else if (content_subtype.equals("bgn")) {
	  System.out.println("PARSING BGN FORMAT FOR DAS2 FEATURE RESPONSE");
	  BgnParser parser = new BgnParser();
	  feats = parser.parse(bis, type.getID(), seq_group.getSeqs(), null, -1, false);
	}
	else if (content_subtype.equals("bps")) {
	  System.out.println("PARSING BPS FORMAT FOR DAS2 FEATURE RESPONSE");
	  BpsParser parser = new BpsParser();
	  DataInputStream dis = new DataInputStream(bis);
	  feats = parser.parse(dis, type.getID(), null, seq_group.getSeqs(), false, false);
	}
	else {
	  System.out.println("ABORTING DAS2 FEATURE LOADING, FORMAT NOT RECOGNIZED: " + content_subtype);
	  success = false;
	}
	bis.close();
	istr.close();


	if (feats == null || feats.size() == 0) {
	  // because many operations will treat empty Das2FeatureRequestSym as a leaf sym, want to
	  //    populate with empty sym child/grandchild
	  // better way might be to have request sym's span on aseq be dependent on children, so
	  //    if no children then no span on aseq (though still an overlap_span and inside_span)
	  /*
	  SimpleSymWithProps child = new SimpleSymWithProps();
	  SimpleSymWithProps grandchild = new SimpleSymWithProps();
	  child.addChild(grandchild);
	  request_sym.addChild(child);
	  */
	}
	else {
	  int feat_count = feats.size();
	  System.out.println("parsed query results, annot count = " + feat_count);
	  for (int k=0; k<feat_count; k++) {
	    SeqSymmetry feat = (SeqSymmetry)feats.get(k);
	    request_sym.addChild(feat);
	  }
	}
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    }
    return success;
  }


}
