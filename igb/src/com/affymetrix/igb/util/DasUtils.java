/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.das.DasLoader;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.regex.*;

import org.xml.sax.*;
import org.w3c.dom.*;
import javax.xml.parsers.ParserConfigurationException;

// TODO: Merge this class with com.affymetrix.igb.das.DasLoader

/**
 *  A set of DAS loading and parsing functions.
 */
public final class DasUtils {
	private static final boolean DEBUG = true;


  static final Pattern white_space = Pattern.compile("\\s+");

  /**
   *  Finds a DAS source on the given server that is a synonym of the
   *  String you request.
   *  @return a matching source on the server, or null.
   */
  public static String findDasSource(String das_server, String source_synonym)
  throws IOException, SAXException, ParserConfigurationException {
    String result = null;
    //      System.out.println("in DasUtils.findDasSource()");
    String request_str = das_server + "/dsn";
		if (DEBUG) {
			System.out.println("Das Request: " + request_str);
		}
    Document doc = DasLoader.getDocument(request_str);
    List<String> sources = DasLoader.parseSourceList(doc);
    SynonymLookup lookup = SynonymLookup.getDefaultLookup();

    result = lookup.findMatchingSynonym(sources, source_synonym);
    return result;
  }

  /**
   *  Finds a DAS sequence id on the given server that is a synonym of the
   *  String you request.
   *  @return a matching sequence id on the server, or null.
   */
  public static String findDasSeqID(String das_server, String das_source, String seqid_synonym)
  throws IOException, SAXException, ParserConfigurationException {
    String result = null;
    //      System.out.println("in DasUtils.findDasSeqID()");
    SynonymLookup lookup = SynonymLookup.getDefaultLookup();
    String request_str = das_server + "/" + das_source + "/entry_points";
		if (DEBUG) {
			System.out.println("Das Request: " + request_str);
		}
    Document doc = DasLoader.getDocument(request_str);
    List<String> segments = DasLoader.parseSegmentsFromEntryPoints(doc);

    result = lookup.findMatchingSynonym(segments,  seqid_synonym);
    return result;
  }

  /**
   *  Get residues for a given region.
   *  min and max are specified in genometry coords (interbase-0),
   *  and since DAS is base-1, inside this method min/max get modified to
   *  (min+1)/max before passing to DAS server
   */
  public static String getDasResidues(String das_server, String das_source, String das_seqid,
				      int min, int max)
  throws IOException, SAXException, ParserConfigurationException {
    String residues = null;
    String request = das_server + "/" +
    das_source + "/dna?segment=" +
    das_seqid + ":" + (min+1) + "," + max;
    URL request_url = new URL(request);
		if (DEBUG) {
    System.out.println("DAS request: " + request);
		}
    URLConnection request_con = request_url.openConnection();
    InputStream result_stream = request_con.getInputStream();
    residues = parseDasResidues(new BufferedInputStream(result_stream));
    return residues;
  }

  public static String parseDasResidues(InputStream das_dna_result)
  throws IOException, SAXException, ParserConfigurationException {
    String residues = null;

    InputSource isrc = new InputSource(das_dna_result);

    Document doc = DasLoader.nonValidatingFactory().newDocumentBuilder().parse(isrc);
    Element top_element = doc.getDocumentElement();
    String name = top_element.getTagName();
    //    System.out.println("top element: " + name);
    NodeList top_children = top_element.getChildNodes();

    Matcher matcher = white_space.matcher("");
      TOP_LOOP:
    for (int i=0; i<top_children.getLength(); i++) {
      Node top_child = top_children.item(i);
      String cname = top_child.getNodeName();
      if (cname != null && cname.equalsIgnoreCase("sequence")) {
	NodeList seq_children = top_child.getChildNodes();
	for (int k=0; k<seq_children.getLength(); k++) {
	  Node seq_child = seq_children.item(k);
	  if (seq_child.getNodeName().equalsIgnoreCase("DNA")) {
	    NodeList dna_children = seq_child.getChildNodes();
	    for (int m=0; m<dna_children.getLength(); m++) {
	      Node dna_child = dna_children.item(m);
	      if (dna_child instanceof org.w3c.dom.Text) {
		residues = ((Text)dna_child).getData();
		// System.out.println("initial residues length: " + residues.length());
                residues = matcher.reset(residues).replaceAll("");
		// System.out.println("residues length w/o whitespace: " + residues.length());
		break TOP_LOOP;
	      }
	    }
	  }
	}
      }
    }

    return residues;
  }

  /**
   *  A thin wrapper around {@link DasLoader#getDocument(InputStream)}.
   */
  public static Document getDocument(InputStream istr) throws IOException, SAXException, ParserConfigurationException {
    Document doc = DasLoader.getDocument(istr);
    return doc;
  }

}
