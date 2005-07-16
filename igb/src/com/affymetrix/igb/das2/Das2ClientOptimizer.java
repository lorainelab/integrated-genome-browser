package com.affymetrix.igb.das2;

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.InputSource;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.parsers.*;

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

  static String default_format = "das2feature";
  // input is List of Das2FeatureRequestSyms
  // output is List of _optimized_ Das2FeatureRequestSyms that are equivalent to input List,
  //    based on current state of SingletonGenometryModel/AnnotatedSeqGroup/SmartAnnotBioSeq
  //    annotations --
  //    Could this strategy handle persistent caching??   Would need to redirect to
  //      file:// URLs on disk, but how to deal with proper setting of headers???
  public static List loadFeatures(Das2FeatureRequestSym input_request) {
    //  public static List optimizeFeatureRequests(List input_requests) {
    List output_requests = new ArrayList();
    output_requests.add(input_request);
    for (int i=0; i<output_requests.size(); i++) {
      Das2FeatureRequestSym request = (Das2FeatureRequestSym)output_requests.get(i);
      boolean success = optimizedLoadFeatures(request);
      request.getRegion().getAnnotatedSeq().addAnnotation(request);
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
    String format = FormatPriorities.getFormat(type);

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
    if (format != null) {
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
      URLConnection query_con = query_url.openConnection();
      String content_type = query_con.getContentType();
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

      if (feats != null) {
	int feat_count = feats.size();
	System.out.println("parsed query results, annot count = " + feat_count);
	for (int k=0; k<feat_count; k++) {
	  SeqSymmetry feat = (SeqSymmetry)feats.get(k);
	  request_sym.addChild(feat);
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
