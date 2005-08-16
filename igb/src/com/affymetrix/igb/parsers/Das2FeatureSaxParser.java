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

package com.affymetrix.igb.parsers;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.xml.sax.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.MutableSingletonSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;

import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.das2.SimpleDas2Feature;

import com.affymetrix.igb.util.GenometryViewer; // for testing main

/**
 * Das2FeatureSaxParser reads and writes DAS2FEATURE XML format.
 *   Spec for this format is at http://biodas.org/documents/das2/das2_get.html
 *   DTD is at http://www.biodas.org/dtd/das2feature.dtd ???
*/
public class Das2FeatureSaxParser extends org.xml.sax.helpers.DefaultHandler
    implements AnnotationWriter  {

  static boolean DEBUG = false;
  static boolean REPORT_RESULTS = false;

  /**
   *  elements possible in DAS2 feature response
   */
  static final String FEATURELIST = "FEATURELIST";
  static final String FEATURE = "FEATURE";
  static final String LOC = "LOC";
  static final String XID = "XID";
  static final String PART = "PART";
  static final String PROP = "PROP";
  static final String ALIGN = "ALIGN";
  static final String PARENT = "PARENT";

  /**
   *  attributes possible in DAS2 feature respons
   */
  static final String ID = "id";   // in <FEATURE>, <PART>
  static final String TYPE = "type";  // in <FEATURE>
  static final String NAME = "name";  // in <FEATURE>
  // parent has moved from attribute of FEATURE to subelement of FEATURE
  //  static final String PARENT = "parent";  // in <FEATURE>
  static final String CREATED = "created";  // in <FEATURE>
  static final String MODIFIED = "modified";  // in <FEATURE>
  static final String DOC_HREF = "doc_href";  // in <FEATURE>
  static final String POS = "pos";  // in <LOC>
  static final String HREF = "href";  // in <XID>
  static final String PTYPE = "ptype";  // in <PROP>
  static final String MIME_TYPE = "mime_type";  // in <PROP>
  static final String CONTENT_ENCODING = "content_encoding";  // in <PROP>
  static final String TGT = "tgt";  // in <ALIGN>
  static final String GAP = "gap";  // in <ALIGN>

  /**
   *  built-in ptype attribute values possible for <PROP> element in DAS2 feature response
   */
  static final String NOTE_PROP = "das:note";
  static final String ALIAS_PROP = "das:alias";
  static final String PHASE_PROP = "das:phase";
  static final String SCORE_PROP = "das:score";

  static final Pattern range_splitter = Pattern.compile("/");
  static final Pattern interval_splitter = Pattern.compile(":");

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  AnnotatedSeqGroup seqgroup = null;
  boolean add_annots_to_seq = false;

  String current_elem = null;  // current element
  StringBuffer current_chars = null;
  Stack elemstack = new Stack();

  String feat_id = null;
  String feat_type = null;
  String feat_name = null;
  String feat_parent_id = null;
  String feat_created = null;
  String feat_modified = null;
  String feat_doc_href = null;

  /**  list of SeqSpans specifying feature locations */
  List feat_locs = new ArrayList();
  List feat_xids = new ArrayList();
  /**
   *  map of child feature id to either:
   *      itself  (if child feature not parsed yet), or
   *      child feature object (if child feature already parsed)
   */
  Map feat_parts = new LinkedHashMap();
  List feat_props = new ArrayList();
  List feat_aligns = new ArrayList();

  /**
   *  lists for builtin feature properties
   *  not using yet (but clearing in clearFeature() just in case)
   */
  List feat_notes = new ArrayList();
  List feat_aliass = new ArrayList();
  List feat_phases = new ArrayList();
  List feat_scores = new ArrayList();

  /**
   *  List of feature jsyms resulting from parse
   */
  List result_syms = null;

  /**
   *  Need mapping so can connect parents and children after sym has already been created
   */
  Map id2sym = new HashMap();

  /**
   *  Need mapping of parent sym to map of child ids to connect parents and children
   */
  Map parent2parts = new HashMap();

  /**
   *  need mapping of parent id to child count for efficiently figuring out when
   *    symmetry is fully populated with children
   */

  public List parse(InputSource isrc, String seqgroup_name)  throws IOException, SAXException {
    return parse(isrc, seqgroup_name, true);
  }

  public List parse(InputSource isrc, String seqgroup_name, boolean annot_seq)  throws IOException, SAXException {
    AnnotatedSeqGroup group = gmodel.addSeqGroup(seqgroup_name);
    return parse(isrc, group, annot_seq);
  }

  public List parse(InputSource isrc, AnnotatedSeqGroup group)  throws IOException, SAXException {
    return parse(isrc, group, true);
  }

  /**
   *  return value is List of all top-level features as symmetries
   *  if annot_seq, then feature symmetries will also be added as annotations to seqs in seq group
   *
   *  For example of situation where annot_seq = false:
   *   with standard IGB DAS2 access, don't want to add annotatons directly to seqs,
   *   but rather want them to be children of a Das2FeatureRequestSym (which in turn is a child of
   *   Das2ContainerAnnot [or possibly TypeContainerAnnot constructed by SmartAnnotSeq itself]),
   *   which in turn is directly attached to the seq as an annotation (giving two levels of additional
   *   annotation hierarchy)
   */
  public List parse(InputSource isrc, AnnotatedSeqGroup group, boolean annot_seq)  throws IOException, SAXException {
    clearAll();
    add_annots_to_seq = annot_seq;

    /**
     *  result_syms get populated via callbacks from reader.parse(),
     *    eventually leading to result_syms.add() calls in addFeatue();
     */
    result_syms = new ArrayList();

    seqgroup = group;

    try {
      XMLReader reader = new org.apache.xerces.parsers.SAXParser();
      //      reader.setFeature("http://xml.org/sax/features/string-interning", true);
      reader.setFeature("http://xml.org/sax/features/validation", false);
      reader.setFeature("http://apache.org/xml/features/validation/dynamic", false);
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      reader.setContentHandler(this);
      reader.parse(isrc);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    System.out.println("finished parsing das2xml feature doc, number of top-level features: " + result_syms.size());
    if (REPORT_RESULTS) {
      for (int i=0; i<result_syms.size(); i++) {
	SeqUtils.printSymmetry((SeqSymmetry)result_syms.get(i));
      }
    }
    //    return aseq;
    return result_syms;

    //    clearAll();
  }

  /**
   *  implementing sax content handler interface
   */
  public void startDocument() {
    System.out.println("Das2FeaturesSaxParser.startDocument() called");
  }

  /**
   *  implementing sax content handler interface
   */
  public void endDocument() {
    //    System.out.println("Das2FeaturesSaxParser.endDocument() called");
  }

  /**
   *  implementing sax content handler interface
   */
  public void startElement(String uri, String name, String qname, Attributes atts) {
    if (DEBUG)  { System.out.println("start element: " + name); }
    elemstack.push(current_elem);
    current_elem = name.intern();

    //    prev_chars = false;
    //    current_chars = null;
    if (current_elem == FEATURELIST) {
    }
    else if (current_elem == FEATURE) {

      feat_id = atts.getValue("id");
      feat_type = atts.getValue("type");
      feat_name = atts.getValue("name");
      // feat_parent_id has moved to <PARENT> element
      //      feat_parent_id = atts.getValue("parent");
      feat_created = atts.getValue("created");
      feat_modified = atts.getValue("modified");
      feat_doc_href = atts.getValue("doc_href");

    }
    else if (current_elem == LOC)  {
      String pos = atts.getValue("pos");
      SeqSpan span = getPositionSpan(pos, seqgroup);
      feat_locs.add(span);
    }
    else if (current_elem == XID) {
    }
    else if (current_elem == PARENT) {
      if (feat_parent_id == null) { feat_parent_id = atts.getValue("id"); }
      else {
	System.out.println("WARNING:  multiple parents for feature, just using first one");
      }
    }
    else if (current_elem == PART) {
      String part_id = atts.getValue("id");
      /*
       *  Use part_id to look for child sym already constructed and placed in id2sym hash
       *  If child sym found then map part_id to child sym in feat_parts
       *  If child sym not found then map part_id to itself, and swap in child sym later when it's created
       */
      SeqSymmetry child_sym = (SeqSymmetry)id2sym.get(part_id);
      if (child_sym == null) {
	feat_parts.put(part_id, part_id);
      }
      else {
	feat_parts.put(part_id, child_sym);
      }
    }
    else if (current_elem == PROP) {
    }
    else if (current_elem == ALIGN) {
    }
    else {
      System.out.println("element not recognized: " + current_elem);
    }
  }

  public void clearAll() {
    result_syms = null;
    id2sym.clear();
    clearFeature();
  }

  public void clearFeature() {
    feat_id = null;
    feat_type = null;
    feat_name = null;
    feat_parent_id = null;
    feat_created = null;
    feat_modified = null;
    feat_doc_href = null;

    feat_locs.clear();
    feat_xids.clear();
    // making new feat_parts map because ref to old feat_parts map may be held for parent/child resolution
    feat_parts = new LinkedHashMap();
    feat_props.clear();
    feat_aligns.clear();

    feat_notes.clear();
    feat_aliass.clear();
    feat_phases.clear();
    feat_scores.clear();

  }

  /**
   *  implementing sax content handler interface
   */
  public void endElement(String uri, String name, String qname)  {
    if (DEBUG)  { System.out.println("end element: " + name); }
    // only two elements that need post-processing are  <FEATURE> and <PROP> ?
    //   other elements are either top <FEATURELIST> or have only attributes
    if (name == FEATURE) {
      addFeature();
      clearFeature();
    }
    else if (name == PROP) {

    }

    //    prev_chars = false;
    //    current_chars = null;
    current_elem = (String)elemstack.pop();
  }

  /**
   *  implementing sax handler interface
   */
  public void characters(char[] ch, int start, int length) {

  }

  public void addFeature() {
    // checking to make sure feature with same id doesn't already exist
    //   (ids _should_ be unique, but want to make sure)
    if (id2sym.get(feat_id) != null) {
      System.out.println("WARNING, duplicate feature id: " + feat_id);
      return;
    }
    SimpleDas2Feature featsym = new SimpleDas2Feature(feat_id, feat_type, feat_name, feat_parent_id,
					      feat_created, feat_modified, feat_doc_href);
    // add featsym to id2sym hash
    id2sym.put(feat_id, featsym);
    parent2parts.put(featsym, feat_parts);

    // add locations as spans...
    int loc_count = feat_locs.size();
    for (int i=0; i<loc_count; i++) {
      SeqSpan span = (SeqSpan)feat_locs.get(i);
      featsym.addSpan(span);
    }

    /**
     *  Add children _only_ if all children already have symmetries in feat_parts
     *  Otherwise need to wait till have all child syms, because need to be
     *     added to parent sym in order.
     *   add children if already parsed (should then be in id2sym hash);
     */
    if (feat_parts.size() > 0) {
      if (childrenReady(featsym)) {
	addChildren(featsym);
	//	parent2parts.remove(featsym);
      }
    }

    // if no parent, then attach directly to AnnotatedBioSeq(s)  (get seqid(s) from location)
    if (feat_parent_id == null) {
      for (int i=0; i<loc_count; i++) {
	SeqSpan span = (SeqSpan)feat_locs.get(i);
	BioSeq seq = span.getBioSeq();
	MutableAnnotatedBioSeq aseq = seqgroup.getSeq(seq.getID());  // should be a SmartAnnotBioSeq
	if ((seq != null) && (aseq != null) && (seq == aseq)) {
	  // really want an extra level of annotation here (add as child to a Das2FeatureRequestSym),
	  //    but Das2FeatureRequestSym is not yet implemented
	  //
	  result_syms.add(featsym);
	  if (add_annots_to_seq)  {
	    aseq.addAnnotation(featsym);
	  }
	}
      }
    }

    else {
      MutableSeqSymmetry parent = (MutableSeqSymmetry)id2sym.get(feat_parent_id);
      if (parent != null) {
	// add child to parent parts map
	LinkedHashMap parent_parts = (LinkedHashMap)parent2parts.get(parent);
        if (parent_parts == null)  {
          System.out.println("WARNING: no parent_parts found for parent, id=" + feat_parent_id);
        }
	else  {
	  parent_parts.put(feat_id, featsym);
	  if (childrenReady(parent)) {
	    addChildren(parent);
	    //	  parent2parts.remove(parent_sym);
	  }
	}
      }
    }

  }


  protected boolean childrenReady(MutableSeqSymmetry parent_sym)  {
    LinkedHashMap parts = (LinkedHashMap)parent2parts.get(parent_sym);
    Iterator citer = parts.values().iterator();
    boolean all_child_syms = true;
    while (citer.hasNext()) {
      Object val = citer.next();
      if (! (val instanceof SeqSymmetry)) {
	all_child_syms = false;
	break;
      }
    }
    return all_child_syms;
  }



  protected void addChildren(MutableSeqSymmetry parent_sym)  {
    // get parts
    LinkedHashMap parts = (LinkedHashMap)parent2parts.get(parent_sym);
    Iterator citer = parts.entrySet().iterator();
    while (citer.hasNext()) {
      Map.Entry keyval = (Map.Entry)citer.next();
      String child_id = (String)keyval.getKey();
      SeqSymmetry child_sym = (SeqSymmetry)keyval.getValue();
      parent_sym.addChild(child_sym);
    }
    //    id2sym.remove(parent_sym);
    parent2parts.remove(parent_sym);
  }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "DAS2FEATURE" XML format.
   *
   *  getMimeType() should really return "text/x-das-feature+xml" but easier to debug as "text/plain"
   *    need to switch over once stabilized
   **/
  public String getMimeType() {
    //    return "text/x-das-feature+xml";
    return "text/plain";
  }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "DASGFF" XML format
   */
  public boolean writeAnnotations(java.util.Collection syms, BioSeq seq,
                                  String type, OutputStream outstream) {
    boolean success = true;
    try {
      PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outstream)));

      // may need to extract seqid, seq version, genome for properly setting xml:base...
      String seq_id = seq.getID();
      String seq_version = null;
      if (seq instanceof Versioned) {
	seq_version = ((Versioned)seq).getVersion();
      }

      pw.println("<?xml version=\"1.0\" standalone=\"no\"?>");
      pw.println("<!DOCTYPE DAS2FEATURE SYSTEM \"http://www.biodas.org/dtd/das2feature.dtd\"> ");
      pw.println("<FEATURELIST  ");
      pw.println("   xmlns=\"http://www.biodas.org/ns/das/2.00\" ");
      pw.println("   xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");
      pw.println("   xml:base=\"http:...\"> ");

      Iterator iterator = syms.iterator();
      while (iterator.hasNext()) {
        SeqSymmetry annot = (SeqSymmetry)iterator.next();
        writeDasFeature(annot, null, 0, seq, type, pw);
      }
      pw.println("</FEATURELIST>");
      pw.flush();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    }
    return success;
  }


  /**
   *  Write out a SeqSymmetry in DAS2FEATURE format
   *  recursively descends to write out all descendants
   */
  public void writeDasFeature(SeqSymmetry annot, String parent_id, int parent_index,
				     BioSeq aseq, String feat_type, PrintWriter pw) {
    if (feat_type == null && annot instanceof SymWithProps) {
      feat_type = (String)((SymWithProps)annot).getProperty("method");
      if (feat_type == null) {
        feat_type = (String)((SymWithProps)annot).getProperty("meth");
      }
      if (feat_type == null) {
        feat_type = (String)((SymWithProps)annot).getProperty("type");
      }
    }
    String feat_id = getChildID(annot, parent_id, parent_index);
    SeqSpan span = annot.getSpan(aseq);

    // print <FEATURE ...> line
    pw.print("  <FEATURE id=\"");
    pw.print(feat_id);
    pw.print("\" type=\"");
    pw.print(feat_type);
    pw.print("\" ");
    /*   parent has moved from being an attribute to being an element (zero or more)
	 writeDasFeature() currently does not handle multiple parents, only zero or one
    if (parent_id != null) {
      pw.print("parent=\"");
      pw.print(parent_id);
      pw.print("\" ");
    }
    */
    pw.print(">");
    pw.println();

    // print  <LOC .../> line
    pw.print("     <LOC pos=\"");
    String position = getPositionString(span, true, true);
    pw.print(position);
    pw.print("\" />");
    pw.println();

    //  parent has moved from being an attribute to being an element (zero or more)
    //    writeDasFeature() currently does not handle multiple parents, only zero or one
    if (parent_id != null) {
      pw.print("     <PARENT id=\"");
      pw.print(parent_id);
      pw.print("\" />");
      pw.println();
    }

    // print  <PART .../> line for each child
    int child_count = annot.getChildCount();
    if (child_count > 0) {
      for (int i=0; i<child_count; i++) {
	SeqSymmetry child = annot.getChild(i);
	String child_id = getChildID(child, feat_id, i);
	pw.print("     <PART id=\"");
	pw.print(child_id);
	pw.print("\" />");
	pw.println();
      }
    }

    // close this feature element
    pw.println("  </FEATURE>");

    // recursively call writeDasFeature() on each child
    if (child_count > 0) {
      for (int i=0; i<child_count; i++) {
	SeqSymmetry child = annot.getChild(i);
	writeDasFeature(child, feat_id, i, aseq, feat_type, pw);
      }
    }
  }


  protected String getChildID(SeqSymmetry child, String parent_id, int parent_index)  {
    String feat_id = null;
    if (child instanceof Propertied) {
      feat_id = (String)((Propertied)child).getProperty("id");
    }
    if (feat_id == null) {
      if (parent_id != null) {
	feat_id = parent_id + "." + Integer.toString(parent_index);
      }
    }
    if (feat_id == null) {
      feat_id = "unknown";
    }
    return feat_id;
  }



  /**
   *  Or should this be called parseRegion() ??
   *
   *  From the DAS2 spec:
   *----------------------------------------------
   *  Ranges have the following format:
   *    seqid/min:max:strand
   *
   *  seqid is the sequence ID, and can correspond to an assembled chromosome, a contig, a clone,
   *  or any other accessionable chunk of sequence. min and max are the minimum and maximum values
   *  of a range on the sequence, and strand denotes the forward, reverse, or both strands of the
   *  sequence using -1,1,0 notation. Everything but the seqid itself is optional when retrieving a range:
   *
   *    Chr1	The entire sequence named Chr1.
   *    Chr1/1000	Chr1 beginning at position 1000 and going to the end.
   *    Chr1/1000:2000	Chr1 from positions 1000 to 2000.
   *    Chr1/:2000	Chr1 from the start to position 2000.
   *    Chr1/1000:2000:-1	The reverse complement of positions 1000 to 2000.
   *
   *  The semantics of the strand are simple when retrieving sequences.
   *  A value of -1 means reverse complement of min:max, and everything else indicates the forward strand.
   *  As described later, the semantics of strand are more subtle when used in the context of the location
   *    of a feature.
   *
   *  Regions are numbered so that min is always less than max. The strand designation is -1 to indicate
   *  a feature on the reverse strand, 1 to indicate a feature on the forward strand, and 0 to indicate
   *  a feature that is on both strands. Leaving the strand field empty implies a value of "unknown."
   *-------------------------------------------
   *
   *  For first cut, assuming that chromosome, min, and max is always present, and strand is always left out
   *     (therefore SeqSpan is forward)
   *
   *  Currently getPositionSpan() handles both with or without extra [xyz/]* prefix, and with or without strand
   *         region/seqid/min:max:strand OR
   *         seqid/min:max:strand
   *   but _not_ the case where there is no seqid, or no min, or no max
   */
  public static SeqSpan getPositionSpan(String position, AnnotatedSeqGroup group) {
    if (position == null) { return null; }
    String[] fields = range_splitter.split(position);
    String seqid = fields[fields.length-2];
    String remainder = fields[fields.length-1];
    String[] subfields = interval_splitter.split(remainder);
    int min = Integer.parseInt(subfields[0]);
    int max = Integer.parseInt(subfields[1]);
    boolean forward = true;
    if (subfields.length >= 3) {
      if (subfields[2].equals("-1")) { forward = false; }
    }
    BioSeq seq = group.getSeq(seqid);
    if (seq == null) {
      MutableAnnotatedBioSeq newseq = new SmartAnnotBioSeq(seqid, group.getVersion(), 123123123);
      group.addSeq(newseq);
      seq = newseq;
    }
    SeqSpan span;
    if (forward)  {
      span = new SimpleSeqSpan(min, max, seq);
    }
    else {
      span = new SimpleSeqSpan(max, min, seq);
    }
    return span;
  }


  /**
   *   if include_header, then prepends "region/" to String, otherwise leaves it off
   *   if include_strand, then appends strand info to end of String (":1") or (":-1")
   *
   *   Need to enhance this to deal with synonyms, so if seq id is different than
   *     corresponding region id, use region id instead.  To do this, probably
   *     need to add an Das2VersionedSource argument (Das2Region would work also,
   *     but probably better to have this method figure out region based on versioned source
   */
  public static String getPositionString(SeqSpan span,
					 boolean include_header, boolean include_strand) {
    if (span == null) { return null; }
    StringBuffer buf = new StringBuffer(100);
    if (include_header)  { buf.append("region/"); }
    buf.append(span.getBioSeq().getID());
    buf.append("/");
    buf.append(Integer.toString(span.getMin()));
    buf.append(":");
    buf.append(Integer.toString(span.getMax()));
    if (include_strand) {
      if (span.isForward()) { buf.append(":1"); }
      else { buf.append(":-1"); }
    }
    return buf.toString();
  }


  public static void main(String[] args) {
    boolean test_result_list = true;
    Das2FeatureSaxParser test = new Das2FeatureSaxParser();
    try {
      String test_file_name = "c:/data/das2_responses/spec_examples/feature.xml";
      //      String test_file_name = "c:/data/das2_responses/biopackages_server/feature_4.xml";
      //      String test_file_name = "c:/data/das2_responses/biopackages_server/feature-3.xml";
      //String test_file_name = "c:/data/das2_responses/biopackages_server/feature-2.xml";
      //String test_file_name = "c:/data/das2_responses/biopackages_server/feature.xml";
      //      String test_file_name = "c:/data/das2_responses/genometry_server/features2.xml";
      //String test_file_name = "c:/data/das2_responses/das_ev_server/old_feature_chrM_1_1000.xml";
      File test_file = new File(test_file_name);
      FileInputStream fistr = new FileInputStream(test_file);
      BufferedInputStream bis = new BufferedInputStream(fistr);
      List annots = test.parse(new InputSource(bis), "test_group");
      bis.close();
      System.out.println("annot count: " + annots.size());
      SeqSymmetry first_annot = (SeqSymmetry)annots.get(0);
      //      SeqUtils.printSymmetry(first_annot);
      AnnotatedSeqGroup group = gmodel.getSeqGroup("test_group");
      AnnotatedBioSeq aseq = group.getSeq(first_annot);
      System.out.println("seq id: " + aseq.getID());
      GenometryViewer viewer = GenometryViewer.displaySeq(aseq, false);
      viewer.setAnnotatedSeq(aseq);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }


}
