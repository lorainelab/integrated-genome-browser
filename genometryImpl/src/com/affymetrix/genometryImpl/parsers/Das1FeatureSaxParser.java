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
package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import java.io.*;
import java.util.*;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.*;
import org.xml.sax.SAXException;

import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;

/**
 *
 * Parses DASGFF format.
 *<pre>
Curently assumes only zero or one group per feature (although the DAS 1.5 spec allows
for more than one per feature)

<DASGFF>
<GFF version="1.0" href="http://genome.cse.ucsc.edu/cgi-bin/das/hg10/features">
<SEGMENT id="chr22" start="20000000" stop="21000000" version="1.00" label="chr22">
<FEATURE id="Em:D87024.C22.12.chr22.20012405.0" label="Em:D87024.C22.12">
<TYPE id="sanger22" category="transcription" reference="no">sanger22</TYPE>
<METHOD></METHOD>
<START>20012406</START>
<END>20012451</END>
<SCORE>-</SCORE>
<ORIENTATION>+</ORIENTATION>
<PHASE>-</PHASE>
<!-- Links and Notes can occur inside or outside the <GROUP> element -->
<NOTE>This is a feature note</NOTE>
<LINK href="http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr22:20012405-20012900&amp;db=hg10">Link to UCSC Browser for the feature</LINK>
<GROUP id="Em:D87024.C22.12.chr22.20012405">
<LINK href="http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr22:20012405-20012900&amp;db=hg10">Link to UCSC Browser</LINK>
<NOTE>This is a group note</NOTE>
</GROUP>
</FEATURE>
</SEGMENT>
</GFF>
</DASGFF>

 *</pre>
 */
public final class Das1FeatureSaxParser extends org.xml.sax.helpers.DefaultHandler
				implements AnnotationWriter {

	static final int UNKNOWN = 0;
	static final int FORWARD = 1;
	static final int REVERSE = 2;
	static final String DASGFF = "DASGFF";
	static final String GFF = "GFF";
	static final String SEGMENT = "SEGMENT";
	static final String FEATURE = "FEATURE";
	static final String TYPE = "TYPE";
	static final String METHOD = "METHOD";
	static final String START = "START";
	static final String END = "END";
	static final String SCORE = "SCORE";
	static final String ORIENTATION = "ORIENTATION";
	static final String PHASE = "PHASE";
	static final String GROUP = "GROUP";
	static final String LINK = "LINK";
	static final String NOTE = "NOTE";
	SynonymLookup lookup = SynonymLookup.getDefaultLookup();
	//boolean MAKE_TYPE_CONTAINER_SYM = true;
	boolean READER_DOES_INTERNING = false;

	// Whether to keep the content of the <NOTE> elements.
	// <NOTE>s that have a "key=value" structure are always kept, but this
	// flag influences whether random textual notes are also kept.
	static final boolean STORE_TEXTUAL_NOTES = false;
	MutableAnnotatedBioSeq aseq = null;
	AnnotatedSeqGroup seq_group = null;
	SingletonSymWithProps current_sym = null;
	String featid = null;
	String feat_label = null;
	String feattype = null;
	int featstart = Integer.MIN_VALUE;
	int featend = Integer.MIN_VALUE;
	int featstrand = UNKNOWN;
	String featgroup = null;
	String featgroup_label = null;
	List<String> featlink_urls = new ArrayList<String>();
	List<String> featlink_names = new ArrayList<String>();
	Map<String, String> feat_notes = null;
	Map<String, String> group_notes = null;
	Map<String, Map<String, Object>> grouphash = new HashMap<String, Map<String, Object>>();  // maps group id/strings to parent SeqSymmetries
	Map<String, SeqSymmetry> typehash = new HashMap<String, SeqSymmetry>();  // maps type id/strings to type symmetries
	MutableSeqSpan unionSpan = new SimpleMutableSeqSpan();
	String current_elem = null;  // current element
	StringBuffer current_chars = null;
	Stack<String> elemstack = new Stack<String>();
	boolean prev_chars = false;
	int cached_int = Integer.MIN_VALUE;
	/** Indicates whether currently within a GROUP element or any descendant of a GROUP element */
	boolean within_group_element = false;
	int featcount = 0;
	int groupcount = 0;
	int elemcount = 0;
	/**
	 *  A Map used to filter features with at particular "id" attribute value in "TYPE" element.
	 */
	Map<String, String> filter_hash = new HashMap<String, String>();
	/**
	 *  List of syms resulting from parse.
	 *  These are the "low-level" results, _not_ the top-level "container" syms
	 *    (two-level if features have group tags, one-level if features have no group tags)
	 */
	List<SeqSymmetry> result_syms = null;

	public Das1FeatureSaxParser() {
		//    filter_hash.put("estOrientInfo", "estOrientInfo");
	}

	/*public Das1FeatureSaxParser(boolean make_container_syms) {
		this();
		MAKE_TYPE_CONTAINER_SYM = make_container_syms;
	}*/

	/**  Sets whether or not to try to prevent duplicate annotations by using
	 *      the Unibrow.getSymHash() global symmetry hash.  If symmetry already
	 *      in symhash has same id (key to symhash) then don't add new annotation.
	 *  @param filter_out_by_id
	 */
	/*public void setFilterOutById(boolean filter_out_by_id) {
		FILTER_OUT_BY_ID = filter_out_by_id;
	}*/

	/*public void addFeatureFilter(String feat_str) {
		filter_hash.put(feat_str, feat_str);
	}*/

	/*public void removeFeatureFilter(String feat_str) {
		filter_hash.remove(feat_str);
	}*/

	public List<SeqSymmetry> parse(InputStream istr, AnnotatedSeqGroup seq_group)
					throws IOException {
		InputSource isrc = new InputSource(istr);
		return parse(isrc, seq_group);
	}

	/**
	 *  Parses a DAS/1 XML file and returns a List of SeqSymmetry's.
	 */
	public List<SeqSymmetry> parse(InputSource isrc, AnnotatedSeqGroup seq_group)
					throws IOException {
		//  result_syms get populated via callbacks from reader.parse(),
		//    eventually leading to result_syms.add() calls in addFeatue();

		result_syms = new ArrayList<SeqSymmetry>();
		this.seq_group = seq_group;

		//  For now assuming the source XML contains only a single segment
		try {
			SAXParserFactory f = SAXParserFactory.newInstance();
			f.setNamespaceAware(true);
			XMLReader reader = f.newSAXParser().getXMLReader();

			try {
				//      reader.setFeature("http://xml.org/sax/features/string-interning", true);
				reader.setFeature("http://xml.org/sax/features/validation", false);
				reader.setFeature("http://apache.org/xml/features/validation/dynamic", false);
				reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			} catch (SAXNotRecognizedException snrex) {
				// We couldn't care less about these exceptions
				System.err.println("WARNING: " + snrex.toString());
			} catch (SAXNotSupportedException snsex) {
				// We couldn't care less about these exceptions
				System.err.println("WARNING: " + snsex.toString());
			}
			reader.setContentHandler(this);
			reader.parse(isrc);
		} catch (SAXException se) {
			IOException ioe = new IOException("Problem parsing DAS XML data: " + se.getMessage());
			ioe.initCause(se);
			throw ioe;
		} catch (ParserConfigurationException e) {
			IOException ioe = new IOException("Problem parsing DAS XML data: " + e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}

		this.seq_group = null; // for garbage-collection
		this.aseq = null;
		this.current_sym = null;
		clearFeature();

		/**
		 *   now check results, set glyph depth to 1 or 2 based on sym depth?
		 */
		return result_syms;
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void endDocument() {
	}

	@Override
	public void startElement(String uri, String name, String qname, Attributes atts) {
		//    System.out.println(name);
		elemstack.push(current_elem);
		elemcount++;
		String iname = null;
		if (READER_DOES_INTERNING) {
			iname = name;
		} else {
			iname = name.intern();
		}
		current_elem = iname;
		prev_chars = false;
		current_chars = null;
		if (iname == FEATURE) {
			featcount++;
			featid = atts.getValue("id");
			feat_label = atts.getValue("label");
		} else if (iname == GROUP) {
			featgroup = atts.getValue("id");
			featgroup_label = atts.getValue("label");
			within_group_element = true;
		} else if (iname == LINK) {
			String href = atts.getValue("href");
			if (href == null) {
				System.out.println("WARNING: DAS Format Error: <LINK> element has no href attribute");
				featlink_urls.add(""); // add a blank URL to keep featlink_urls and featlink_name list sizes the same
			} else {
				featlink_urls.add(href);
			}
		} else if (iname == NOTE) {
			// handling NOTES in endElement method...
			} else if (iname == TYPE) {
			feattype = atts.getValue("id").intern();
		} else if (iname == DASGFF) {
		} else if (iname == GFF) {
		} else if (iname == SEGMENT) {
			String seqid = atts.getValue("id").intern();
			int seqlength = Integer.parseInt(atts.getValue("stop"));
			// if sequence id doesn't match, go ahead and make new seq here
			aseq = seq_group.getSeq(seqid);
			if (aseq == null) {
				System.out.println("making new annotated sequence: " + seqid + ", length = " + seqlength);
				aseq = seq_group.addSeq(seqid, seqlength);
			}
		}
	}

	@Override
	public void endElement(String uri, String name, String qname) {
		String iname = null;
		if (READER_DOES_INTERNING) {
			iname = name;
		} else {
			iname = name.intern();
		}
		if (iname == FEATURE) {
			addFeature();
			clearFeature();
		} else if (iname == GROUP) {
			within_group_element = false;
		} else if (iname == LINK) {
			String link_name = null;
			if (current_chars != null) {
				String text = current_chars.toString().trim();
				if (text.length() > 0) {
					link_name = text;
				}
			}
			if (link_name == null) {
				link_name = featlink_urls.get(featlink_urls.size() - 1);
			}
			featlink_names.add(link_name);

		} else if (iname == NOTE && current_chars != null) {
			String note_text = current_chars.toString();

			if (within_group_element) {
				if (group_notes == null) {
					group_notes = new HashMap<String, String>();
				}
				parseNote(note_text, group_notes);
			} else {
				if (feat_notes == null) {
					feat_notes = new HashMap<String, String>();
				}
				parseNote(note_text, feat_notes);
			}
		}
		current_elem = elemstack.pop();
		current_chars = null;
		prev_chars = false;
	}

	/** Parse the text of a DAS-XML NOTE element and put the results in a map.
	 *  If the note is of form, "tag=value", the tag and value are used as key and value
	 *  in the map.  Otherwise, the string is put in the map using the key "note".
	 */
	static Map<String, String> parseNote(String note_text, Map<String, String> map) {
		int split_pos = note_text.indexOf("=");
		if (split_pos > 0 && (split_pos < (note_text.length() - 1))) {
			// assuming parsing out a tag-value pair...
			String tag = note_text.substring(0, note_text.indexOf("="));
			String val = note_text.substring(note_text.indexOf("=") + 1);
			if ((val != null) &&
							(val.charAt(0) == '\"') &&
							(val.charAt(val.length() - 1) == '\"')) {
				val = val.substring(1, val.length() - 1);
			}
			map.put(tag, val);
		} else if (split_pos == (note_text.length() - 1)) {
			// If note text ends with "=", like "self_cross_hybridizes=",
			// then it isn't useful information, so skip it to save memory.
		} else {
			if (STORE_TEXTUAL_NOTES) {
				map.put("note", note_text);
			}
		}
		return map;
	}

	public void clearFeature() {
		featid = null;
		feat_label = null;
		feattype = null;
		featstart = Integer.MIN_VALUE;
		featend = Integer.MIN_VALUE;
		featstrand = UNKNOWN;
		featgroup = null;
		featgroup_label = null;
		//    featlink = null;
		featlink_urls.clear();
		featlink_names.clear();
		feat_notes = null;
		group_notes = null;
	}

	/**
	 *  Get a parent SeqSymmetry for holding all children of a given group
	 *  in a given feature type.  For example, the type may be "RefSeq" and
	 *  the group may be a gene "NM_0000042".  This is essentially a two-level
	 *  hash.  The return type is declared as Object rather than SeqSymmetry to
	 *  avoid multiple casts since you will most likely have to cast the return value
	 *  to a specific sub-type of SeqSymmetry before using it.
	 *
	 *  @return a SeqSymmetry or null.
	 */
	private Object getGroupSymmetryForType(String feattype, String featgroup) {
		Map<String,Object> map = grouphash.get(feattype);
		if (map == null) {
			return null;
		}
		return map.get(featgroup);
	}

	/**
	 *  Store a SeqSymmetry object for holding all children of a given group
	 *  in a given feature type.  For example, the type may be "RefSeq" and
	 *  the group may be a gene "NM_0000042".
	 *
	 *  @return the Object previously stored for that type and group, or null.
	 */
	private Object putGroupSymmetryForType(String feattype, String featgroup, Object o) {
		Map<String, Object> map = grouphash.get(feattype);
		if (map == null) {
			map = new HashMap<String, Object>();
			grouphash.put(feattype, map);
		}
		return map.put(featgroup, o);
	}

	private void addFeature() {
		boolean filter = false;

		/*
		 *  filter out this feature if either:
		 *     its feature type is entered in the filter_hash OR
		 *     filtering by id is enabled and the annotation is already present
		 *        in Unibrow symhash (based on id hashing)
		 */
		if (featgroup != null) {
			filter = getGroupSymmetryForType(feattype, featgroup) == null &&
							!seq_group.findSyms(featgroup).isEmpty();
		} else {
			filter =	!seq_group.findSyms(featid).isEmpty();
		}

		if (filter) {
			//      System.err.println("filtering out, already have a symmetry with same id: " +
			//                         " featgroup = " + featgroup + ", featid = " + featid);
			//      filter_count++;
		} else {  // not filtered out

			int min = (featstart <= featend ? featstart : featend);
			int max = (featstart >= featend ? featstart : featend);

			// check max of each feature against sequence length --
			// since length of sequence is currently derived from the range of annotations requested
			//    (length = stop attribute of SEGMENT), but all annotations that overlap range are
			//    returned, there may be annotations with min inside range but max extending outside
			//    of range, and therefore outside of derived sequence bounds, so extend length of
			//    sequence to encompass these, since sequence must go beyond them
			// Really would rather have some way of returning the actual length of the BioSeq, but
			//    for DAS this will have to do for now
			if (max > aseq.getLength()) {
				aseq.setLength(max);
			}

			if (featstrand == FORWARD || featstrand == UNKNOWN) {
				//        span.set(min, max, aseq);
				//      current_sym = new MutableSingletonSeqSymmetry(min, max, aseq);
				// GAH 5-21-2003 -- switching to using SingletonSymWithProps to allow
				//    for possibility of attaching note tag/val properties to leaf annotations
				current_sym = new SingletonSymWithProps(min, max, aseq);
			} else {  // featstrand == MINUS
				//        span.set(max, min, aseq);
				//      current_sym = new MutableSingletonSeqSymmetry(max, min, aseq);
				current_sym = new SingletonSymWithProps(max, min, aseq);
			//        System.out.println("reversed:" + featcount);
			//        SeqUtils.printSymmetry(current_sym); }
			//        if (featcount <= 10)  { SeqUtils.printSymmetry(current_sym); }
			}
			//    SymWithProps grandparent_sym = null;
			SimpleSymWithProps grandparent_sym = null;
			if (feattype != null) {
				grandparent_sym = (SimpleSymWithProps) typehash.get(feattype);
				if (grandparent_sym == null) {
					//        grandparent_sym =
					//          new MutableSingletonSeqSymmetry(current_sym.getStart(), current_sym.getEnd(), aseq);
					grandparent_sym = new SimpleSymWithProps();
					MutableSeqSpan gpspan = new SimpleMutableSeqSpan(current_sym.getStart(),
									current_sym.getEnd(), aseq);
					grandparent_sym.setProperty("method", feattype);
					grandparent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
					grandparent_sym.addSpan(gpspan);
					typehash.put(feattype, grandparent_sym);
					aseq.addAnnotation(grandparent_sym);
				} else {
					MutableSeqSpan gpspan = (MutableSeqSpan) grandparent_sym.getSpan(aseq);
					SeqUtils.encompass(gpspan, (SeqSpan) current_sym, unionSpan);
					gpspan.set(unionSpan.getStart(), unionSpan.getEnd(), aseq);
				}
			}
			if (featgroup != null) {  // if there is a group id, add annotation to parent annotation

				addAnnotToParent(grandparent_sym);
			} else {  // if no group id, then no parent sym
				//  so add annotation directly to AnnotatedBioSeq
				//  (or "grandparent" container sym if MAKE_TYPE_CONTAINER_SYM
				addAnnotToBioSeq(grandparent_sym);
			}
		}

	}

	private void addAnnotToParent(SimpleSymWithProps grandparent_sym) {
		// if there is a group id, add annotation to parent annotation
		//    MutableSingletonSeqSymmetry parent_sym = null;
		SingletonSymWithProps parent_sym = (SingletonSymWithProps) getGroupSymmetryForType(feattype, featgroup);
		if (parent_sym == null) {
			groupcount++;
			//        parent_sym = new MutableSingletonSeqSymmetry(current_sym.getStart(), current_sym.getEnd(), aseq);
			parent_sym = new SingletonSymWithProps(current_sym.getStart(), current_sym.getEnd(), aseq);
			if (featgroup_label != null && featgroup_label.trim().length() > 0) {
				parent_sym.setProperty(DAS_GROUP_ID, featgroup);
				parent_sym.setProperty("id", featgroup_label);
			} else {
				//parent_sym.setProperty("das_group_id", featgroup);
				parent_sym.setProperty("id", featgroup);
			}
			if (feattype != null) {
				parent_sym.setProperty("method", feattype);
			}
			putGroupSymmetryForType(feattype, featgroup, parent_sym);
			seq_group.addToIndex(featgroup, parent_sym);
			if (grandparent_sym != null) {
				grandparent_sym.addChild(parent_sym);
			} else {
				aseq.addAnnotation(parent_sym);
			}
			result_syms.add(parent_sym);
		} else {
			SeqUtils.encompass((SeqSpan) parent_sym, (SeqSpan) current_sym, unionSpan);
			parent_sym.set(unionSpan.getStart(), unionSpan.getEnd(), aseq);
		}
		parent_sym.addChild(current_sym);
		if (feat_notes != null) {
			for (String key : feat_notes.keySet()) {
				current_sym.setProperty(key, feat_notes.get(key));
			}
		}
		if (group_notes != null) {
			for (String key : group_notes.keySet()) {
				// for now, adding notes to parent instead of child...
				parent_sym.setProperty(key, group_notes.get(key));
			}
		}
		/*
		if (featlink != null) {
		//        System.out.println("setting link: " + featlink);
		parent_sym.setProperty("link", featlink);
		}
		 */
		if (featlink_urls.size() > 0) {
			Object prev_links = parent_sym.getProperty("link");
			Map<String, String> links_hash = null;
			String prev_link = null;
			if (prev_links instanceof String) {
				prev_link = (String) prev_links;
			} else if (prev_links instanceof Map) {
				links_hash = (Map<String, String>) prev_links;
			}
			int linkcount = featlink_urls.size();
			if (linkcount == 1 && ((prev_links == null) || ((prev_link != null) && prev_link.equals(featlink_urls.get(0))))) {
				parent_sym.setProperty("link", featlink_urls.get(0));
				if (featlink_names.size() > 0) {
					parent_sym.setProperty("link_name", featlink_names.get(0));
				} else {
					parent_sym.setProperty("link_name", featlink_urls.get(0));
				}
			} else {
				if (links_hash == null) {
					links_hash = new HashMap<String, String>();
					parent_sym.setProperty("link", links_hash);
					if (prev_link != null) {
						//                links_list.add(prev_link);
						links_hash.put(prev_link, prev_link);
					}
				}
				for (int i = 0; i < linkcount; i++) {
					String linkurl = featlink_urls.get(i);
					//              links_list.add(linkurl);
					String linkname = featlink_names.get(i);
					links_hash.put(linkname, linkurl);
				}
			}
		}
	}


	private void addAnnotToBioSeq(SimpleSymWithProps grandparent_sym) {
		// if no group id, then no parent sym
		//  so add annotation directly to AnnotatedBioSeq
		//  (or "grandparent" container sym if MAKE_TYPE_CONTAINER_SYM
		seq_group.addToIndex(featid, current_sym);
		//        if (featlink != null) { current_sym.setProperty("link", featlink); }
		for (String linkurl : featlink_urls) {
			current_sym.setProperty("link", linkurl);
		}
		if (feat_label != null && feat_label.trim().length() > 0) {
			if (featid != null) {
				//            System.out.println("featid: " + featid);
				current_sym.setProperty(DAS_FEATURE_ID, featid);
			}
			current_sym.setProperty("id", feat_label);
		} else if (featid != null) {
			//parent_sym.setProperty("das_group_id", featgroup);
			current_sym.setProperty("id", featid);
		}
		if (feattype != null) {
			current_sym.setProperty("method", feattype);
		}
		if (grandparent_sym != null) {
			grandparent_sym.addChild(current_sym);
		} else {
			aseq.addAnnotation(current_sym);
		}
		result_syms.add(current_sym);
	}

	

	/*
	 *  According to SAX2 spec, parsers can split up character content any
	 *    way they wish.  This makes designing an efficient processor that can avoid lots of
	 *    String churn difficult.
	 *  However, the parsers I've seen _usually_ make single calls to characters()
	 *    for most character content (for example, Xerces SAX parser appears to make single calls
	 *    for any character content that is smaller than 16Kb)
	 *  So, I'd like a parser that's optimized for the common case where character content within
	 *    a particular element is a single characters() call, BUT which can handle the cases where this
	 *    content is split across multiple characters() calls
	 */
	@Override
	public void characters(char[] ch, int start, int length) {
		//    System.out.println("***" + new String(ch, start, length) + "&&&");
		//    if (inStartElem || inEndElem) {
		if (current_elem == START || current_elem == END) {  // parse out integer
			// if no previously collected characters, go ahead and parse as an integer
			//   -- if for some reason more characters are needed, keep adding to already created
			//   integer...,  but watch out for the case where an integer is split near a '0' !
			if (prev_chars && (cached_int != Integer.MIN_VALUE)) {
				int temp_int = parseInt(ch, start, length);

				int x = 0; // x is the number of digits in the string used to make temp_int
				while (x <= length && ch[start + x] >= 0x0030 && ch[start + x] <= 0x0039) {
					x++;
				}
				int scale = (int) Math.pow(10, x);
				cached_int = (cached_int * scale) + temp_int;

			// Note: If the xml file doesn't include any extraneous white space, then
			// it will always be true that x == length, but counting the characters
			// is more generally safe.
			} else {
				cached_int = parseInt(ch, start, length);
			}
			if (current_elem == START) {
				featstart = cached_int;
				// adjusting from "start at 1, base coords" to "start at 0, between coords" numbering
				featstart--;
			} else if (current_elem == END) {
				featend = cached_int;
			}
		} else if (current_elem == ORIENTATION) {
			for (int i = start; i < start + length; i++) {
				if (ch[i] == '+') {
					featstrand = FORWARD;
					break;
				} else if (ch[i] == '-') {
					featstrand = REVERSE;
					break;
				}
			}
		} else if (current_elem == LINK || current_elem == NOTE) {
			if (current_chars == null) {
				current_chars = new StringBuffer(length);
			}
			current_chars.append(ch, start, length);
		}
		prev_chars = true;
	}

	/*public static void main(String[] args) {
		//boolean use_viewer = true;
		Das1FeatureSaxParser test = new Das1FeatureSaxParser();
		try {
			String user_dir = System.getProperty("user.dir");
			// String test_file_name = user_dir + "/testdata/das/dastesting2.xml";
			//      String test_file_name = "c:/data/das1_responses/ensembl_feature_response.xml";
			String test_file_name = "c:/data/das1_responses/ensembl_feature_response2.xml";

			File test_file = new File(test_file_name);
			FileInputStream fistr = new FileInputStream(test_file);
			SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
			AnnotatedSeqGroup seq_group = gmodel.addSeqGroup("Test Seq Group");
			List results = test.parse(fistr, seq_group);

			SeqSymmetry first_sym = (SeqSymmetry) results.get(1);

			System.out.println("result annotation count: " + results.size());
			System.out.println("first annotation:");
			SeqUtils.printSymmetry(first_sym);

		//      if (use_viewer) {
		//        MutableAnnotatedBioSeq seq = (AnnotatedBioSeq) first_sym.getSpan(0).getBioSeq();
		//        GenometryViewer viewer = GenometryViewer.displaySeq(seq, false);
		//        viewer.setAnnotatedSeq(seq);
		//      }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}*/

// trying to parse integers without having to make lots 'o new Strings...
//
// adapted from java.lang.Integer, streamlined to assume radix=10,
//    no NumberFormatExceptions
//    assumes won't go beyond integer representation limits...
//    (~ -2.15 billion to ~ +2.14 billion)
//    public static int parseInt(String s, int radix)
// assuming only ISO-LATIN numbers
// 0..9 is 0x0030..0x0039 Unicode
	private static int parseInt(char[] chars, int start, int length) {
		int radix = 10;
		int result = 0;
		boolean negative = false;
		//    int i = 0, max = s.length();
		int i = start;
		int max = start + length - 1;
		int digit;
		char curch;


		if (chars[start] == '-') {
			negative = true;
			i++;
			while (i <= max) {
				curch = chars[i];
				// may want to eliminate this check if can assume that there is no whitespace...
				if (curch >= 0x0030 && curch <= 0x0039) {
					digit = curch - 0x0030;
					//          digit = Character.digit(curch, 10);
					result *= radix;
					result -= digit;
				}
				i++;
			}
		} else {
			while (i <= max) {
				curch = chars[i];
				// may want to eliminate this check if can assume that there is no whitespace...
				if (curch >= 0x0030 && curch <= 0x0039) {
					digit = curch - 0x0030;
					//          digit = Character.digit(curch, 10);
					result *= radix;
					result += digit;
				}
				i++;
			}
		}

		if (negative) {
			return -result;
		} else {
			return result;
		}
	}

	private static void writeDasFeatHeader(SeqSpan qspan, PrintWriter pw) {
		MutableAnnotatedBioSeq aseq = qspan.getBioSeq();
		String seq_id = aseq.getID();
		int start = qspan.getMin();
		int stop = qspan.getMax();
		String version = "unknown";
		if (aseq instanceof BioSeq) {
			version = ((BioSeq) aseq).getVersion();
		}
		pw.println("<?xml version=\"1.0\" standalone=\"no\"?>");
		pw.println("<DASGFF>");
		pw.println("<GFF version=\"1.0\" href=\"dummy href\">");
		pw.println("<SEGMENT id=\"" + seq_id + "\" start=\"" + start + "\"" +
						" stop=\"" + stop + "\" version=\"" + version + "\" >");
	}

	private static void writeDasFeatFooter(PrintWriter pw) {
		pw.println("</SEGMENT>");
		pw.println("</GFF>");
		pw.println("</DASGFF>");
	}
	/**
	 *  When the user has specified a "label" in the GROUP element, the real
	 *  group ID will be stored in a field with this name instead of in the "id"
	 *  field, which will hold the given label instead.
	 */
	static final String DAS_GROUP_ID = "das_group_id";
	static final String DAS_FEATURE_ID = "das_feature_id";

	private static void writeDasFeature(SeqSymmetry annot, MutableAnnotatedBioSeq aseq, String feat_type, PrintWriter pw) {
		if (feat_type == null) {
			feat_type = BioSeq.determineMethod(annot);
		}
		String group_id = "unknown";
		if (annot instanceof SymWithProps) {
			// Typically, the DAS group ID's are simply stored in the "id" field.
			// But if the property DAS_GROUP_ID is non-null, it must be used as
			// the DAS group id.
			group_id = (String) ((SymWithProps) annot).getProperty(DAS_GROUP_ID);
			if (group_id == null) {
				group_id = (String) ((SymWithProps) annot).getProperty("id");
			}
			if (group_id == null) {
				group_id = "unknown";
			}
		}

		//    String group_id = "" + (int)(Math.random() * 1000000000);
		int child_count = annot.getChildCount();

		if (child_count == 0) {
			int i = 0;
			SeqSymmetry csym = annot;
			SeqSpan cspan = csym.getSpan(aseq);
			String child_id = group_id + "." + i;
			String orient;
			if (cspan.isForward()) {
				orient = "+";
			} else {
				orient = "-";
			}
			pw.println("  <FEATURE id=\"" + child_id + "\" >");
			pw.println("      <TYPE id=\"" + feat_type + "\" />");
			pw.println("      <METHOD id=\"unknown\" />");
			pw.println("      <START>" + (cspan.getMin() + 1) + "</START>");  // +1 to compensate for DAS
			pw.println("      <END>" + cspan.getMax() + "</END>");
			pw.println("      <ORIENTATION>" + orient + "</ORIENTATION>");
			pw.println("      <PHASE>-</PHASE>");
			pw.println("      <GROUP id=\"" + group_id + "\" />");
			pw.println("  </FEATURE>");
		} else {
			String orient;
			if (annot.getSpan(aseq).isForward()) {
				orient = "+";
			} else {
				orient = "-";
			}
			for (int i = 0; i < child_count; i++) {
				SeqSymmetry csym = annot.getChild(i);
				SeqSpan cspan = csym.getSpan(aseq);
				String child_id = group_id + "." + i;
				pw.println("  <FEATURE id=\"" + child_id + "\" >");
				pw.println("      <TYPE id=\"" + feat_type + "\" />");
				pw.println("      <METHOD id=\"unknown\" />");
				pw.println("      <START>" + (cspan.getMin() + 1) + "</START>");  // +1 to compensate for DAS
				pw.println("      <END>" + cspan.getMax() + "</END>");
				pw.println("      <ORIENTATION>" + orient + "</ORIENTATION>");
				pw.println("      <PHASE>-</PHASE>");
				pw.println("      <GROUP id=\"" + group_id + "\" />");
				pw.println("  </FEATURE>");
			}
		}

	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "DASGFF" XML format.
	 *
	 *  getMimeType() should really return "xml" as first part, not sure about second,
	 *    maybe like "xml/dasgff".  But only indication in current spec (DAS 1.53) is
	 *    a hint that it should be "text/plain", though unclear...
	 **/
	public String getMimeType() {
		return "text/plain";
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "DASGFF" XML format
	 */
	public boolean writeAnnotations(java.util.Collection<SeqSymmetry> syms, MutableAnnotatedBioSeq seq,
					String type, OutputStream outstream) {
		boolean success = true;
		// for now, assume bounds of query are min/max of syms...
		// for rightnow, assume bounds of query are bounds of seq
		int min = 0;
		int max = seq.getLength();
		SeqSpan qspan = new SimpleSeqSpan(min, max, seq);
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outstream)));
			Das1FeatureSaxParser.writeDasFeatHeader(qspan, pw);
			Iterator<SeqSymmetry> iterator = syms.iterator();
			while (iterator.hasNext()) {
				SeqSymmetry annot = iterator.next();
				Das1FeatureSaxParser.writeDasFeature(annot, seq, type, pw);
			}
			//      System.out.println("annot returned: " + annot_count);
			Das1FeatureSaxParser.writeDasFeatFooter(pw);
			pw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
			success = false;
		}
		return success;
	}

}
