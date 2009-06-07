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

package com.affymetrix.igb.das;

import java.io.*;
import java.io.IOException;
import java.net.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.util.LocalUrlCacher;



/**
 * A class to help load and parse documents from a DAS server.
 */
public abstract class DasLoader {
  final static boolean DEBUG = false;
	static final Pattern white_space = Pattern.compile("\\s+");

	/**
	 * Create a new DocumentBuilder factory with validation disabled.
   * The parser returned is not specifically set-up for DAS, and can be
   * used in any case where you want a non-validating parser.
	 */
	public static DocumentBuilderFactory nonValidatingFactory() {
		if (DEBUG)
			System.out.println("========== Getting a nonValidatingFactory!");

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {
			factory.setFeature("http://xml.org/sax/features/validation", false);
			factory.setFeature("http://apache.org/xml/features/validation/dynamic", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		return factory;
	}

  /**
   *  Set parser factory to _not_ defer node expansion (thus forcing full expansion of DOM when
   *    loaded). This slows down "loading" of DOM significantly (~2-3x), but also significantly
   *    speeds up later access of the document, since that does not
   *    trigger any node expansions.
   *  Saves a lot of memory because it eliminates deferred-node objects that xerces-j uses, which
   *     seem to elude the garbage collector and just keep accumulating....
	 *
	 * Unknown if this is still needed, as we are no longer guaranteed to be using Xerces-j.
   */
	public static void doNotDeferNodeExpansion(DocumentBuilderFactory factory) {
		try {
			factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

  /** Opens an XML document, using {@link #nonValidatingFactory()}. */
  public static Document getDocument(String url)
  throws ParserConfigurationException, MalformedURLException, SAXException, IOException {
    if (DEBUG) System.out.println("=========== Getting a Document from URL: "+url);

    Document doc = null;
    URL request_url = new URL(url);
    URLConnection request_con = request_url.openConnection();
		request_con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
		request_con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
    doc = getDocument(request_con);
    return doc;
  }

  /** Opens an XML document, using {@link #nonValidatingFactory()}. */
  public static Document getDocument(URLConnection request_con)
  throws ParserConfigurationException, SAXException, IOException {
    if (DEBUG) System.out.println("=========== Getting a Document from connection: "+request_con.getURL().toExternalForm());

    if (DEBUG) { LocalUrlCacher.reportHeaders(request_con); }

    InputStream result_stream = null;
    Document doc = null;
    try {
      result_stream = new BufferedInputStream(request_con.getInputStream());
      doc = getDocument(result_stream);
    } finally {
      if (result_stream != null) try {result_stream.close();} catch (Exception e) {}
    }
    return doc;
  }

  /** Opens an XML document, using {@link #nonValidatingFactory()}. */
  public static Document getDocument(InputStream str)
  throws ParserConfigurationException, SAXException, IOException {
    return nonValidatingFactory().newDocumentBuilder().parse(str);
  }

  /**
   *  Returns a Map where keys are String labels and values are SeqSpan's.
   *  Looks for <gff><segment id="..."> where the id's are in the given seq_group.
   */
  public static Map<String,SeqSpan> parseTermQuery(Document doc, AnnotatedSeqGroup seq_group) {
    if (DEBUG) System.out.println("========= Parsing term query");
    Map<String,SeqSpan> segment_hash = new HashMap<String,SeqSpan>();

    Element top_element = doc.getDocumentElement();
    //String name = top_element.getTagName();
    //      System.out.println("top element: " + name);
    NodeList children = top_element.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String cname = child.getNodeName();
      if (cname != null && cname.equalsIgnoreCase("gff")) {
        NodeList gff_children = child.getChildNodes();
        for (int k=0; k<gff_children.getLength(); k++) {
          Node gff_child = gff_children.item(k);
          if (gff_child.getNodeName().equalsIgnoreCase("segment"))  {
            Element seg_elem = (Element)gff_child;
            String id = seg_elem.getAttribute("id");
            if (id != null) {
              SmartAnnotBioSeq segmentseq = seq_group.getSeq(id);
              if (segmentseq != null) {
                int start = Integer.parseInt(seg_elem.getAttribute("start"));
                int end = Integer.parseInt(seg_elem.getAttribute("end"));
                SeqSpan segment_span = new SimpleSeqSpan(start, end, segmentseq);
                String das_label = seg_elem.getAttribute("label");
                String label = id + " : " + start + ", " + end + " ==> " + das_label;

                segment_hash.put(label, segment_span);
                if (DEBUG)  {
                  System.out.println("segment: id = " + id + ", start = " + start + ", end = " + end);
                }
              }
            }
          }
        }
      }
    }
    return segment_hash;
  }

  /**
   *  Returns a List of String's which are the id's of the segments.
   *  From <entry_points><segment id="...">.
   */
  public static List<String> parseSegmentsFromEntryPoints(Document doc) {
    List<String> seqs = new Vector<String>();
    if (DEBUG) System.out.println("========= Parsing Segments from Entry Points");
    Element top_element = doc.getDocumentElement();
    String name = top_element.getTagName();
    //        System.out.println("top element: " + name);
    NodeList children = top_element.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String cname = child.getNodeName();
      if (cname != null && cname.equalsIgnoreCase("entry_points")) {
        NodeList entry_children = child.getChildNodes();
        for (int k=0; k<entry_children.getLength(); k++) {
          Node entry_child = entry_children.item(k);
          String gcname = entry_child.getNodeName();
          if (gcname != null && gcname.equalsIgnoreCase("segment")) {
            Element segment_elem = (Element)entry_child;
            String id = segment_elem.getAttribute("id");
            seqs.add(id);
          }
        }
      }
    }
    return seqs;
  }

  /** Returns a list of source id Strings.
   *  From  <dsn><source id="...">.
   */
  public static List<String> parseSourceList(Document doc) {
    List<String> ids = new ArrayList<String>();
    if (DEBUG) System.out.println("========= Parsing Source List");
    String matching_id = null;
    Element top_element = doc.getDocumentElement();
    String name = top_element.getTagName();
    NodeList children = top_element.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String cname = child.getNodeName();
      // System.out.println(name);
      if (cname != null && cname.equalsIgnoreCase("dsn")) {
        NodeList dsn_children = child.getChildNodes();
        for (int k=0; k<dsn_children.getLength(); k++) {
          Node dsn_child = dsn_children.item(k);
          String gcname = dsn_child.getNodeName();
          if (gcname != null && gcname.equalsIgnoreCase("source")) {
            Element source_elem = (Element)dsn_child;
            String id = source_elem.getAttribute("id");
            ids.add(id);
          }
        }
      }
    }
    return ids;
  }

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
		request_con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
		request_con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
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
		for (int i = 0; i < top_children.getLength(); i++) {
			Node top_child = top_children.item(i);
			String cname = top_child.getNodeName();
			if (cname != null && cname.equalsIgnoreCase("sequence")) {
				NodeList seq_children = top_child.getChildNodes();
				for (int k = 0; k < seq_children.getLength(); k++) {
					Node seq_child = seq_children.item(k);
					if (seq_child.getNodeName().equalsIgnoreCase("DNA")) {
						NodeList dna_children = seq_child.getChildNodes();
						for (int m = 0; m < dna_children.getLength(); m++) {
							Node dna_child = dna_children.item(m);
							if (dna_child instanceof org.w3c.dom.Text) {
								residues = ((Text) dna_child).getData();
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

}
