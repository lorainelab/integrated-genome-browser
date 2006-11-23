/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
import org.xml.sax.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.parsers.Das2FeatureSaxParser;

public class Das2Region {
  static public boolean USE_SEGMENT = true;  // segment param, or old version with seq included in other filters
  static public boolean USE_SEGMENT_URI = false;
  static public boolean USE_TYPE_URI = false;
  static public  boolean URL_ENCODE_QUERY = true;


  URI region_uri;
  String name;
  int length;
  String info_url;  // doc_href
  //  java.util.List assembly;  // or should this be a SeqSymmetry??   // or composition of CompositeBioSeq??
  SeqSpan segment_span;
  MutableAnnotatedBioSeq aseq;
  Das2VersionedSource versioned_source;

  public Das2Region(Das2VersionedSource source, URI reg_uri, String nm, String info, int ln) {
    region_uri = reg_uri;
    name = nm;
    info_url = info;
    length = ln;

    versioned_source = source;
    AnnotatedSeqGroup genome = versioned_source.getGenome();
    // a)  see if id of Das2Region maps directly to an already seen annotated seq in genome
    aseq = genome.getSeq(name);
    if (aseq == null) { aseq = genome.getSeq(this.getID()); }
    // b) if can't find a previously seen genome for this DasSource, then
    //     create a new genome entry
    if (aseq == null) {
      // using name instead of id for now
      aseq = genome.addSeq(name, length);
    }
    segment_span = new SimpleSeqSpan(0, length, aseq);
  }


  public URI getURI() { return region_uri; }
  public String getID() { return region_uri.toString(); }
  public String getName() { return name; }
  public String getInfoUrl() { return info_url; }
  public Das2VersionedSource getVersionedSource() { return versioned_source; }

  public SeqSpan getSegment() {
    return segment_span;
  }

  // or should this return a SmartAnnotbioSeq???
  public MutableAnnotatedBioSeq getAnnotatedSeq() {
    return aseq;
  }


  /**
   *  Basic retrieval of DAS/2 features, without optimizations (see Das2ClientOptimizer for that)
   *  Retrieves features from a DAS2 server based on inforation in the Das2FeatureRequestSym.
   *  Takes an uninitialized Das2FeatureRequestSym as an argument,
   *    constructs a DAS2 query based on sym,
   *    sends query to DAS2 server
   *    hands returned data to a parser, get List of annotations back from parser
   *    adds annotations as children to Das2FeatureRequestSym
   *    (should it also add request_sym to SmartAnnotBioSeq (becomes child of container, or
   *         should that be handled before/after getFeatures() is called?  leaning towards the latter...)
   *
   *  returns true if feature query returns successfully, false otherwise
   *
   */
  public boolean getFeatures(Das2FeatureRequestSym request_sym) {
    boolean success = true;
      SeqSpan overlap_span = request_sym.getOverlapSpan();
      //      String overlap_filter = getPositionString(overlap_span, USE_SEGMENT_URI, false);
      String overlap_filter = getPositionString(overlap_span, false);
      SeqSpan inside_span = request_sym.getInsideSpan();
      //      String inside_filter = getPositionString(inside_span, USE_SEGMENT_URI, false);
      String inside_filter = getPositionString(inside_span, false);

      System.out.println("in Das2Region.getFeatures(), overlap = " + overlap_filter + ", inside = " + inside_filter);
      Das2Type type = request_sym.getDas2Type();
      String format = FormatPriorities.getFormat(type);


      Das2Capability featcap = getVersionedSource().getCapability(Das2VersionedSource.FEATURES_CAP_QUERY);
      String request_root = featcap.getRootURI().toString();

      StringBuffer buf = new StringBuffer(200);
      //      buf.append(query_root.toString());
      //      buf.append("?");

      buf.append("overlaps=");
      buf.append(overlap_filter);
      buf.append(";");
      if (inside_filter != null) {
	buf.append("inside=");
	buf.append(inside_filter);
	buf.append(";");
      }
      buf.append("type=");
      if (USE_TYPE_URI) {
	buf.append(type.getID());
      }
      else {
	// GAH temporary hack till biopackages recognition of type URIs and/or names are fixed
	if (request_root.indexOf("biopackages") >= 0)  {
	  buf.append("SO:");
	}
	buf.append(type.getName());
      }

      try {
	String query_part = buf.toString();
	if (URL_ENCODE_QUERY) {
	  query_part = URLEncoder.encode(query_part, "UTF-8");
	}
	String feature_query = request_root + "?" + query_part;
	System.out.println("feature query:  " + feature_query);

	Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
	URL query_url = new URL(feature_query);
	URLConnection query_con = query_url.openConnection();
	InputStream istr = query_con.getInputStream();
	BufferedInputStream bis = new BufferedInputStream(istr);
	List feats = parser.parse(new InputSource(bis), feature_query, getVersionedSource().getGenome(), false);
	int feat_count = feats.size();
	System.out.println("parsed query results, annot count = " + feat_count);
	for (int k=0; k<feat_count; k++) {
	  SeqSymmetry feat = (SeqSymmetry)feats.get(k);
	  //	SeqUtils.printSymmetry(feat);
	  request_sym.addChild(feat);
	}
	success = true;
	bis.close();
	istr.close();
      }
      catch (Exception ex) {
	ex.printStackTrace();
	success = false;
      }
    return success;
  }


  /**
   *  moved into Das2Region
   *  for now, assume that
   *   Converts a SeqSpan to a DAS2 region String.
   *   if use_segment_uri, then uses full URI of the segment, otherwise uses segment name
   *   if include_strand, then appends strand info to end of String (":1") or (":-1")
   *
   *   Need to enhance this to deal with synonyms, so if seq id is different than
   *     corresponding region id, use region id instead.  To do this, probably
   *     need to add an Das2VersionedSource argument (Das2Region would work also,
   *     but probably better to have this method figure out region based on versioned source
   */
  public String getPositionString(SeqSpan span, boolean include_strand) {
  //  public String getPositionString(SeqSpan span, boolean use_segment_uri, boolean include_strand) {
    String result = null;
    if (span != null) {
      BioSeq spanseq = span.getBioSeq();
      if (this.getAnnotatedSeq() == spanseq) {
	StringBuffer buf = new StringBuffer(100);
	// making sure to use name/id given by DAS server, which may be a synonym of the seq's id instead of the seq id itself
	//	if (use_segment_uri) { buf.append(this.getID()); }
	//	else { buf.append(this.getName()); }
	buf.append(this.getName());
	// buf.append(span.getBioSeq().getID());
	buf.append("/");
	buf.append(Integer.toString(span.getMin()));
	buf.append(":");
	buf.append(Integer.toString(span.getMax()));
	if (include_strand) {
	  if (span.isForward()) { buf.append(":1"); }
	  else { buf.append(":-1"); }
	}
	result = buf.toString();
      }
      else {  // this region's annotated seq is _not_ the same seq as the span argument seq
	// throw an error?
	// return null?
	// try using Das2VersionedSource.getRegion()?
      }
    }
    return result;
  }


}
