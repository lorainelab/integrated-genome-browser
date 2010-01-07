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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.XMLUtils;

/**
 * A class to help load and parse documents from a DAS server.
 */
public abstract class DasLoader {

	private final static boolean DEBUG = false;
	private static final Pattern white_space = Pattern.compile("\\s+");

	/**
	 *  Returns a List of String's which are the id's of the segments.
	 *  From <entry_points><segment id="...">.
	 */
	public static List<String> parseSegmentsFromEntryPoints(Document doc) {
		List<String> seqs = new ArrayList<String>();
		if (DEBUG) {
			System.out.println("========= Parsing Segments from Entry Points");
		}
		Element top_element = doc.getDocumentElement();
		NodeList children = top_element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String cname = child.getNodeName();
			if (cname != null && cname.equalsIgnoreCase("entry_points")) {
				NodeList entry_children = child.getChildNodes();
				for (int k = 0; k < entry_children.getLength(); k++) {
					Node entry_child = entry_children.item(k);
					String gcname = entry_child.getNodeName();
					if (gcname != null && gcname.equalsIgnoreCase("segment")) {
						Element segment_elem = (Element) entry_child;
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
		if (DEBUG) {
			System.out.println("========= Parsing Source List");
		}
		Element top_element = doc.getDocumentElement();
		NodeList children = top_element.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String cname = child.getNodeName();
			if (cname != null && cname.equalsIgnoreCase("dsn")) {
				NodeList dsn_children = child.getChildNodes();
				for (int k = 0; k < dsn_children.getLength(); k++) {
					Node dsn_child = dsn_children.item(k);
					String gcname = dsn_child.getNodeName();
					if (gcname != null && gcname.equalsIgnoreCase("source")) {
						Element source_elem = (Element) dsn_child;
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
		String request_str = das_server + "/dsn";
		if (DEBUG) {
			System.out.println("Das Request: " + request_str);
		}
		Document doc = XMLUtils.getDocument(request_str);
		List<String> sources = DasLoader.parseSourceList(doc);
		SynonymLookup lookup = SynonymLookup.getDefaultLookup();

		String result = lookup.findMatchingSynonym(sources, source_synonym);
		return result;
	}

	/**
	 *  Finds a DAS sequence id on the given server that is a synonym of the
	 *  String you request.
	 *  @return a matching sequence id on the server, or null.
	 */
	public static String findDasSeqID(String das_server, String das_source, String seqid_synonym)
			throws IOException, SAXException, ParserConfigurationException {
		SynonymLookup lookup = SynonymLookup.getDefaultLookup();
		String request_str = das_server + "/" + das_source + "/entry_points";
		if (DEBUG) {
			System.out.println("Das Request: " + request_str);
		}
		Document doc = XMLUtils.getDocument(request_str);
		List<String> segments = DasLoader.parseSegmentsFromEntryPoints(doc);

		String result = lookup.findMatchingSynonym(segments, seqid_synonym);
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
		String request = das_server + "/"
				+ das_source + "/dna?segment="
				+ das_seqid + ":" + (min + 1) + "," + max;
		InputStream result_stream = LocalUrlCacher.getInputStream(request);
		String residues = parseDasResidues(new BufferedInputStream(result_stream));
		GeneralUtils.safeClose(result_stream);
		return residues;
	}

	private static String parseDasResidues(InputStream das_dna_result)
			throws IOException, SAXException, ParserConfigurationException {
		InputSource isrc = new InputSource(das_dna_result);

		Document doc = XMLUtils.nonValidatingFactory().newDocumentBuilder().parse(isrc);
		Element top_element = doc.getDocumentElement();
		NodeList top_children = top_element.getChildNodes();

		for (int i = 0; i < top_children.getLength(); i++) {
			Node top_child = top_children.item(i);
			String cname = top_child.getNodeName();
			if (cname == null || !cname.equalsIgnoreCase("sequence")) {
				continue;
			}
			NodeList seq_children = top_child.getChildNodes();
			for (int k = 0; k < seq_children.getLength(); k++) {
				Node seq_child = seq_children.item(k);
				if (seq_child == null || !seq_child.getNodeName().equalsIgnoreCase("DNA")) {
					continue;
				}
				NodeList dna_children = seq_child.getChildNodes();
				for (int m = 0; m < dna_children.getLength(); m++) {
					Node dna_child = dna_children.item(m);
					if (dna_child instanceof org.w3c.dom.Text) {
						String residues = ((Text) dna_child).getData();
						Matcher matcher = white_space.matcher("");
						residues = matcher.reset(residues).replaceAll("");
						return residues;
					}
				}
			}
		}
		return null;
	}
}
