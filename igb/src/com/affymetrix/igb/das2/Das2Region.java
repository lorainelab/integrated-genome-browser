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
package com.affymetrix.igb.das2;

import java.io.*;
import java.net.*;
//import java.util.*;
import java.util.List;
import org.xml.sax.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;

public class Das2Region {

    static public boolean USE_SEGMENT = true;  // segment param, or old version with seq included in other filters
    static public boolean USE_SEGMENT_URI = true;
    static public boolean USE_TYPE_URI = true;
    static public boolean URL_ENCODE_QUERY = true;
    URI region_uri;
    String name;
    int length;
    String info_url;  // doc_href
    //  List assembly;  // or should this be a SeqSymmetry??   // or composition of CompositeBioSeq??
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
        //   check for prior existence of BioSeq for Das2Region only if genome group is _not_ a Das2SeqGroup
        //      if group is a Das2SeqGroup, then calling getSeq() will trigger infinite loop as group attempts
        //      to initialize sequences via Das2VersionedSources.getSegments()
        //   But if genome is a Das2SeqGroup, then can assume that no seqs are in group that aren't
        //      being put there in this constructor, and these will be unique, so can skip check for prior existence
        if (!(genome instanceof Das2SeqGroup)) {
            aseq = genome.getSeq(name);
            if (aseq == null) {
                aseq = genome.getSeq(this.getID());
            }
        }
        // b) if can't find a previously seen genome for this DasSource, then
        //     create a new genome entry
        if (aseq == null) {
            // using name instead of id for now
            aseq = genome.addSeq(name, length);
        }
        segment_span = new SimpleSeqSpan(0, length, aseq);
    }

    // public boolean getResidues(SeqSpan span)  {
    /** return a BioSeq (or maybe a CharSeqIterator that can be used for composition of aseq?) */
    public BioSeq getResidues(SeqSpan span) {
        return null;
    }

    public URI getURI() {
        return region_uri;
    }

    public String getID() {
        return region_uri.toString();
    }

    public String getName() {
        return name;
    }

    public String getInfoUrl() {
        return info_url;
    }

    public Das2VersionedSource getVersionedSource() {
        return versioned_source;
    }

    public SeqSpan getSegment() {
        return segment_span;
    }

    // or should this return a SmartAnnotbioSeq???
    public MutableAnnotatedBioSeq getAnnotatedSeq() {
        return aseq;
    }

    /**
     *  Basic retrieval of DAS/2 features, without optimizations
     *  (see {@link Das2ClientOptimizer} for that).
     *  Retrieves features from a DAS2 server based on inforation in the Das2FeatureRequestSym.
     *  Takes an uninitialized Das2FeatureRequestSym as an argument,
     *    constructs a DAS2 query based on sym,
     *    sends query to DAS2 server
     *    hands returned data to a parser, get List of annotations back from parser
     *    adds annotations as children to Das2FeatureRequestSym
     *    (should it also add request_sym to SmartAnnotBioSeq (becomes child of container, or
     *         should that be handled before/after getFeatures() is called?  leaning towards the latter...)
     *
     *  @return true if feature query returns successfully, false otherwise.  Look
     *  at {@link Das2FeatureRequestSym#getLog()} for more details of the results.
     *
     */
    public boolean getFeatures(Das2FeatureRequestSym request_sym) {
        boolean success = true;
        Das2RequestLog request_log = request_sym.getLog();
        SeqSpan overlap_span = request_sym.getOverlapSpan();
        String overlap_filter = getPositionString(overlap_span, false);
        SeqSpan inside_span = request_sym.getInsideSpan();
        String inside_filter = getPositionString(inside_span, false);

        request_log.addLogMessage("in Das2Region.getFeatures(), overlap = " + overlap_filter + ", inside = " + inside_filter);
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
        } else {
            // GAH temporary hack till biopackages recognition of type URIs and/or names are fixed
            if (request_root.indexOf("biopackages") >= 0) {
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
            request_log.addLogMessage("feature query:  " + feature_query);

            Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
            URL query_url = new URL(feature_query);
            HttpURLConnection query_con = (HttpURLConnection) query_url.openConnection();

            int response_code = query_con.getResponseCode();
            String response_message = query_con.getResponseMessage();

            request_log.setHttpResponse(response_code, response_message);

            InputStream istr = query_con.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(istr);
            List feats = parser.parse(new InputSource(bis), feature_query, getVersionedSource().getGenome(), false);
            int feat_count = feats.size();
            request_log.addLogMessage("parsed query results, annot count = " + feat_count);
            for (int k = 0; k < feat_count; k++) {
                SeqSymmetry feat = (SeqSymmetry) feats.get(k);
                //	SeqUtils.printSymmetry(feat);
                request_sym.addChild(feat);
            }
            success = true;
            try {
                bis.close();
            } catch (Exception e) {
                // this sort of exception does NOT mean the request failed.
                request_log.addLogMessage("WARNING: Couldn't close buffered input stream.");
            }
            try {
                istr.close();
            } catch (Exception e) {
                // this sort of exception does NOT mean the request failed.
                request_log.addLogMessage("WARNING: Couldn't close input stream.");
            }
        } catch (Exception ex) {
            request_log.setException(ex);
            success = false;
        }
        request_log.setSuccess(success);
        return success;
    }

    /**
     *   Converts a SeqSpan to a DAS2 region String.
     *   if include_strand, then appends strand info to end of String (":1") or (":-1").
     *
     *   Need to enhance this to deal with synonyms, so if seq id is different than
     *     corresponding region id, use region id instead.  To do this, probably
     *     need to add an Das2VersionedSource argument (Das2Region would work also,
     *     but probably better to have this method figure out region based on versioned source
     */
    // Note similarities to Das2FeatureSaxParser.getRangeString.
    public String getPositionString(SeqSpan span, boolean indicate_strand) {
        if (span == null) {
            return null;
        }
        String result = null;
        BioSeq spanseq = span.getBioSeq();
        if (this.getAnnotatedSeq() == spanseq) {
            StringBuffer buf = new StringBuffer(100);
            buf.append(this.getName());
            buf.append("/");
            buf.append(Integer.toString(span.getMin()));
            buf.append(":");
            buf.append(Integer.toString(span.getMax()));
            if (indicate_strand) {
                if (span.isForward()) {
                    buf.append(":1");
                } else {
                    buf.append(":-1");
                }
            }
            result = buf.toString();
        } else {  // this region's annotated seq is _not_ the same seq as the span argument seq
            // throw an error?
            // return null?
            // try using Das2VersionedSource.getRegion()?
        }
        return result;
    }
}
