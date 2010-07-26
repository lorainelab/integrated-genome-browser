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
package com.affymetrix.genometryImpl.das2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.FeatureRequestSym;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.BgnParser;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.BrsParser;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.parsers.ExonArrayDesignParser;
import com.affymetrix.genometryImpl.parsers.GFFParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqRegionParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqUtilities;
import com.affymetrix.genometryImpl.util.ClientOptimizer;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;

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
 */
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.util.logging.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class Das2ClientOptimizer {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_HEADERS = false;

    /**
     *  For DAS/2 version >= 300, the segment part of location-based feature filters is split
     *  out into a separate query field, "segment", that applies to all location-based filters in the query
     *  (overlaps, inside, ??)
     */
    private static final String default_format = "das2feature";
    

    // input is a single Das2FeatureRequestSym
    // output is List of _optimized_ Das2FeatureRequestSyms that are equivalent to input request,
    //    based on current state of GenometryModel/AnnotatedSeqGroup/BioSeq
    //    annotations --
    //    Could this strategy handle persistent caching??   Would need to redirect to
    //      file:// URLs on disk, but how to deal with proper setting of headers???
    //      would need to have additional info persisted on disk with header info...
    //      but for general case, need more info on disk anyway, to quickly find bounds
    //
    // also attaches
    // assume for now one type, one overlap span
    public static List<? extends FeatureRequestSym> loadFeatures(Das2FeatureRequestSym request_sym) {
        List<FeatureRequestSym> output_requests = new ArrayList<FeatureRequestSym>();
       
        SeqSpan overlap_span = request_sym.getOverlapSpan();
        BioSeq seq = overlap_span.getBioSeq();
        
        Das2Type type = request_sym.getDas2Type();

        if (seq == null) {
            System.out.println("Can't optimize query: " + type.getURI() + ", seq is null!");
            output_requests.add(request_sym);
        } else {
			ClientOptimizer.OptimizeQuery(seq, type.getURI(), type, type.getName(), output_requests, request_sym);
        }

        for (FeatureRequestSym request : output_requests) {
            loadRequestSym((Das2FeatureRequestSym)request);
        }

        return output_requests;
    }

    private static void loadRequestSym(Das2FeatureRequestSym request_sym) {
        Das2Region region = request_sym.getRegion();
        SeqSpan overlap_span = request_sym.getOverlapSpan();
        SeqSpan inside_span = request_sym.getInsideSpan();
        String overlap_filter = Das2FeatureSaxParser.getRangeString(overlap_span, false);
        String inside_filter = inside_span == null ? null : Das2FeatureSaxParser.getRangeString(inside_span, false);

        if (DEBUG) {
            Logger.getLogger(Das2ClientOptimizer.class.getName()).fine("overlap = " + overlap_filter +
                    ", inside = " + inside_filter);
        }
        Das2Type type = request_sym.getDas2Type();
        String format = request_sym.getFormat();
        // if format already specified in Das2FeatureRequestSym, don't optimize
        if (format == null) {
            format = FormatPriorities.getFormat(type);
            request_sym.setFormat(format);
        }

        BioSeq aseq = region.getAnnotatedSeq();
        Das2VersionedSource versioned_source = region.getVersionedSource();
        AnnotatedSeqGroup seq_group = versioned_source.getGenome();

        Das2Capability featcap = versioned_source.getCapability(Das2VersionedSource.FEATURES_CAP_QUERY);
        String request_root = featcap.getRootURI().toString();

        if (DEBUG) {
            Logger.getLogger(Das2ClientOptimizer.class.getName()).fine("   request root: " + request_root);
            Logger.getLogger(Das2ClientOptimizer.class.getName()).fine("   preferred format: " + format);
        }

        try {
            String query_part = DetermineQueryPart(region, overlap_filter, inside_filter, type, format);

            if (format == null) {
                format = default_format;
            }

            String feature_query = request_root + "?" + query_part;
            if (DEBUG) {
                Logger.getLogger(Das2ClientOptimizer.class.getName()).fine("feature query URL:  " + feature_query);
                Logger.getLogger(Das2ClientOptimizer.class.getName()).fine("url-encoded query URL:  " + URLEncoder.encode(feature_query, Constants.UTF8));
                Logger.getLogger(Das2ClientOptimizer.class.getName()).fine("url-decoded query:  " + URLDecoder.decode(feature_query, Constants.UTF8));
            }
			LoadFeaturesFromQuery(
					overlap_span, aseq, feature_query, format, seq_group, type, request_sym);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }

    private static String DetermineQueryPart(Das2Region region, String overlap_filter, String inside_filter, Das2Type type, String format) throws UnsupportedEncodingException {
      StringBuffer buf = new StringBuffer(200);
		buf.append("segment=");
		buf.append(URLEncoder.encode(region.getID(), Constants.UTF8));
		buf.append(";");
        buf.append("overlaps=");
        buf.append(URLEncoder.encode(overlap_filter, Constants.UTF8));
        buf.append(";");
        if (inside_filter != null) {
            buf.append("inside=");
            buf.append(URLEncoder.encode(inside_filter, Constants.UTF8));
            buf.append(";");
        }
        buf.append("type=");
        buf.append(URLEncoder.encode(type.getID(), Constants.UTF8));
        if (format != null) {
            buf.append(";");
            buf.append("format="); 
            buf.append(URLEncoder.encode(format, Constants.UTF8));
        }
        String query_part = buf.toString();

        return query_part;
    }

    private static boolean LoadFeaturesFromQuery(
            SeqSpan overlap_span, BioSeq aseq, String feature_query, String format, 
            AnnotatedSeqGroup seq_group, Das2Type type, Das2FeatureRequestSym request_sym) {

        /**
         *  Need to look at content-type of server response
         */
        BufferedInputStream bis = null;
        InputStream istr = null;
        String content_subtype = null;
        
        try {
            // if overlap_span is entire length of sequence, then check for caching
            if ((overlap_span.getMin() == 0) && (overlap_span.getMax() == aseq.getLength())) {
                istr = LocalUrlCacher.getInputStream(feature_query);
                if (istr == null) {
                    System.out.println("Server couldn't be accessed with query " + feature_query);
                    return false;
                }
                // for now, assume that when caching, content type returned is same as content type requested
                content_subtype = format;
            } else {
                URL query_url = new URL(feature_query);
                if (DEBUG) {
                    System.out.println("    opening connection " + feature_query);
                }
                // casting to HttpURLConnection, since Das2 servers should be either accessed via either HTTP or HTTPS
                HttpURLConnection query_con = (HttpURLConnection) query_url.openConnection();
                int response_code = query_con.getResponseCode();
                String response_message = query_con.getResponseMessage();

                if (DEBUG) {
                    System.out.println("http response code: " + response_code + ", " + response_message);
                }

                if (DEBUG_HEADERS) {
                    int hindex = 0;
                    while (true) {
                        String val = query_con.getHeaderField(hindex);
                        String key = query_con.getHeaderFieldKey(hindex);
                        if (val == null && key == null) {
                            break;
                        }
                        System.out.println("header:   key = " + key + ", val = " + val);
                        hindex++;
                    }
                }

                if (response_code != 200) {
                    System.out.println("WARNING, HTTP response code not 200/OK: " + response_code + ", " + response_message);
                }

                if (response_code >= 400 && response_code < 600) {
                    System.out.println("Server returned error code, aborting response parsing!");
                    return false;
                }
                String content_type = query_con.getContentType();
				istr = query_con.getInputStream();

				content_subtype = content_type.substring(content_type.indexOf("/") + 1);
				int sindex = content_subtype.indexOf(';');
				if (sindex >= 0) {
					content_subtype = content_subtype.substring(0, sindex);
					content_subtype = content_subtype.trim();
				}
				if (DEBUG) {
					System.out.println("content type: " + content_type);
					System.out.println("content subtype: " + content_subtype);
				}
				if (content_subtype == null || content_type.equals("unknown") || content_subtype.equals("unknown") || content_subtype.equals("xml") || content_subtype.equals("plain")) {
					// if content type is not descriptive enough, go by what was requested
					content_subtype = format;
				}
            }

            AddParsingLogMessage(content_subtype);
			String extension = "." + content_subtype;	// We add a ".", since this is expected to be a file extension
			List<? extends SeqSymmetry> feats = FeatureRequestSym.Parse(extension, type.getURI(), istr, seq_group, type.getName(), overlap_span);

			//watch out for useq format, this can contain stranded graph data from a single DAS/2 response, modify the name so it can be caught while making graphs
			String name = request_sym.getDas2Type().getName();
			if (request_sym.getFormat().equals(USeqUtilities.USEQ_EXTENSION_NO_PERIOD)) name = name + USeqUtilities.USEQ_EXTENSION_WITH_PERIOD;
			
			//add data
			FeatureRequestSym.addToRequestSym(
					feats, request_sym, request_sym.getDas2Type().getURI(), name, request_sym.getOverlapSpan());
			FeatureRequestSym.addAnnotations(feats, request_sym, aseq);
            
            return (feats != null);
        } catch (Exception ex) {
			Logger.getLogger(Das2ClientOptimizer.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} finally {
            GeneralUtils.safeClose(bis);
            GeneralUtils.safeClose(istr);
        }
    }

     private static void AddParsingLogMessage(String content_subtype) {
        System.out.println("PARSING " + content_subtype.toUpperCase() + " FORMAT FOR DAS2 FEATURE RESPONSE");
    }

}
