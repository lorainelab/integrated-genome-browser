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

import org.xml.sax.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.SimpleAnnotatedBioSeq;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.MutableSingletonSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.NibbleBioSeq;
import com.affymetrix.igb.genometry.SingletonSymWithProps;

import com.affymetrix.igb.util.GenometryViewer; // for testing main

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
           <GROUP id="Em:D87024.C22.12.chr22.20012405">
             <LINK href="http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr22:20012405-20012900&amp;db=hg10">Link to UCSC Browser</LINK>
           </GROUP>
       </FEATURE>
     </SEGMENT>
  </GFF>
</DASGFF>

 *</pre>
*/

public class DasFeat2GenometrySaxParser extends org.xml.sax.helpers.DefaultHandler
    implements AnnotationWriter  {
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

  boolean MAKE_TYPE_CONTAINER_SYM = true;
  boolean READER_DOES_INTERNING = false;
  boolean FILTER_OUT_BY_ID = true;

  MutableAnnotatedBioSeq aseq = null;
  Map seqhash = null;
  SingletonSymWithProps current_sym = null;

  String featid = null;
  String feattype = null;
  int featstart = Integer.MIN_VALUE;
  int featend = Integer.MIN_VALUE;
  int featstrand = UNKNOWN;
  String featgroup = null;
  java.util.List featlink_urls = new ArrayList();
  java.util.List featlink_names = new ArrayList();
  HashMap feat_notes = null;
  HashMap group_notes = null;

  Hashtable grouphash = new Hashtable();  // maps group id/strings to parent SeqSymmetries
  Hashtable typehash = new Hashtable();  // maps type id/strings to type symmetries

  MutableSeqSpan unionSpan = new SimpleMutableSeqSpan();

  String current_elem = null;  // current element
  StringBuffer current_chars = null;

  Stack elemstack = new Stack();
  Stack symstack = new Stack();
  boolean parse_chars_to_int = false;

  // whether to accumulate characters for parsing content...
  boolean collect_characters = false;
  boolean prev_chars = false;
  int cached_int = Integer.MIN_VALUE;

  /** indicates whether currently within a GROUP element or any descendant of a GROUP element */
  boolean within_group_element = false;

  int featcount = 0;
  int groupcount = 0;
  int elemcount = 0;

  /**
   *  a hash used to filter features with at particular "id" attribute value in "TYPE" element
   */
  Hashtable filter_hash = new Hashtable();

  /**
   *  List of syms resulting from parse
   *  These are the "low-level" results, _not_ the top-level "container" syms
   *    (two-level if features have group tags, one-level if features have no group tags)
   */
  List result_syms = null;

  public DasFeat2GenometrySaxParser() {
    //    filter_hash.put("estOrientInfo", "estOrientInfo");
  }

  public DasFeat2GenometrySaxParser(boolean make_container_syms) {
    this();
    MAKE_TYPE_CONTAINER_SYM = make_container_syms;
  }

  /**  Sets whether or not to try to prevent duplicate annotations by using
   *      the Unibrow.getSymHash() global symmetry hash.  If symmetry already
   *      in symhash has same id (key to symhash) then don't add new annotation.
   *  @param filter_out_by_id   
   */
  public void setFilterOutById(boolean filter_out_by_id) {
    FILTER_OUT_BY_ID = filter_out_by_id;
  }

  public void addFeatureFilter(String feat_str) {
    filter_hash.put(feat_str, feat_str);
  }

  public void removeFeatureFilter(String feat_str) {
    filter_hash.remove(feat_str);
  }


  public MutableAnnotatedBioSeq parse(InputStream istr)
  throws IOException, SAXException {
    return parse(istr, new HashMap());
  }

  public MutableAnnotatedBioSeq parse(InputSource insource)
  throws IOException, SAXException {
    return parse(insource, new HashMap());
  }

  public MutableAnnotatedBioSeq parse(String uri, MutableAnnotatedBioSeq seq)
  throws IOException, SAXException {
    InputSource isrc = new InputSource(uri);
    return parse(isrc, seq);
  }

  public MutableAnnotatedBioSeq parse(InputStream istr, Map seqmap)
  throws IOException, SAXException {
    InputSource isrc = new InputSource(istr);
    return parse(isrc, seqmap);
  }

  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq seq)
  throws IOException, SAXException {
    InputSource isrc = new InputSource(istr);
    return parse(isrc, seq);
  }

  public MutableAnnotatedBioSeq parse(InputSource isrc, MutableAnnotatedBioSeq seq)
  throws IOException, SAXException {
    HashMap singlet_hash = new HashMap();
    if (seq != null) {
      singlet_hash.put(seq.getID(), seq);
    }
    return parse(isrc, singlet_hash);
  }


  public MutableAnnotatedBioSeq parse(InputSource isrc, Map seqmap)
  throws IOException, SAXException {
    return parse(isrc, seqmap, null);
  }

  /**
   *  @param annotmap  parameter currently not used
   */
  public MutableAnnotatedBioSeq parse(InputSource isrc, Map seqmap, Map annotmap)
  throws IOException, SAXException {
    parseWithResultList(isrc, seqmap);
    return aseq;
  }


  public List parseWithResultList(InputSource isrc, Map seqmap)
  throws IOException, SAXException {
    /*
     *  result_syms get populated via callbacks from reader.parse(),
     *    eventually leading to result_syms.add() calls in addFeatue();
     */
    //    System.out.println("in DasFeat2GenometrySaxParser, seqhash = " + seqhash);
    result_syms = new ArrayList();
    seqhash = seqmap;
    //  For now assuming the source XML contains only a single segment
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
    //    return aseq;
    return result_syms;
  }

  public void startDocument() {
  }

  public void endDocument() {
  }


  public void startElement(String uri, String name, String qname, Attributes atts) {
    //    System.out.println(name);
    elemstack.push(current_elem);
    elemcount++;
    String iname = null;
    if (READER_DOES_INTERNING) { iname = name; }
    else { iname = name.intern(); }
    current_elem = iname;
    prev_chars = false;
    current_chars = null;
    if (iname == FEATURE) {
      featcount++;
      featid = atts.getValue("id");
    }
    else if (iname == GROUP) {
      featgroup = atts.getValue("id");
      within_group_element = true;
    }
    else if (iname == LINK) {
      featlink_urls.add(atts.getValue("href"));
    }
    else if (iname == NOTE) {
      // handling NOTES in characters method...
    }
    else if (iname == TYPE) {
      feattype = atts.getValue("id").intern();
    }

    else if (iname == DASGFF) { }
    else if (iname == GFF) { }
    else if (iname == SEGMENT) {
      //      System.out.println("got segment element start");
      String seqid = atts.getValue("id").intern();
      int seqlength = Integer.parseInt(atts.getValue("stop"));
      // if no sequence passed in (to merge), or if id doesn't match, go ahead and make new seq here
      //      if (aseq == null || aseq.getID() == null || (! aseq.getID().equals(seqid))) {
      if (seqhash != null) {
        if (seqhash.get(seqid) != null) {
          aseq = (MutableAnnotatedBioSeq)seqhash.get(seqid);
        }
        else {
          Iterator iter = seqhash.values().iterator();
          while (iter.hasNext()) {
            MutableAnnotatedBioSeq checkseq = (MutableAnnotatedBioSeq)iter.next();
            //            System.out.println("checking for id match:  seqid = " + seqid +
            //                               ", seq from hash id = " + checkseq.getID());
            if (lookup.isSynonym(seqid, checkseq.getID())) {
              aseq = (MutableAnnotatedBioSeq)checkseq;
              break;
            }
          }
        }
      }
      if (aseq == null) {
        System.out.println("making new annotated sequence: " + seqid + ", length = " + seqlength);
        aseq = new SimpleAnnotatedBioSeq(seqid, seqlength);
      }
      // otherwise, try and merge?
      // should really make sure it's also the same server, mapmaster, data-source...
      else {  // same seqid as previous
        // just keep same aseq
        //        System.out.println("trying to merge with prior sequence, " +
        //                           "not all equality checks are in place yet...");
      }
    }
  }

  public void endElement(String uri, String name, String qname)  {
    String iname = null;
    if (READER_DOES_INTERNING) {
      iname = name;
    }
    else {
      iname = name.intern();
    }
    if (iname == FEATURE) {
      addFeature();
      clearFeature();
    }
    else if (iname == GROUP) {
      within_group_element = false;
    }
    else if (iname == LINK) {
      if (featlink_names.size() < featlink_urls.size()) {
        String url_as_name = (String)featlink_urls.get(featlink_names.size());
              featlink_names.add(url_as_name);
      }
      //      while (featlink_names.size() < featlink_urls.size()) {
      //        featlink_names.add("");
      //      }
    }
    current_elem = (String)elemstack.pop();
    current_chars = null;
    prev_chars = false;
  }

  public void clearFeature() {
    featid = null;
    feattype = null;
    featstart = Integer.MIN_VALUE;
    featend = Integer.MIN_VALUE;
    featstrand = UNKNOWN;
    featgroup = null;
    //    featlink = null;
    featlink_urls.clear();
    featlink_names.clear();
    feat_notes = null;
    group_notes = null;
  }

  public void addFeature() {
    Map global_symhash = IGB.getSymHash();
    boolean filter = false;

    /*
     *  filter out this feature if either:
     *     it's feature type is entered in the filter_hash OR
     *     filtering by id is enabled and the annotation is already present
     *        in Unibrow symhash (based on id hashing)
     */
    if (featgroup != null)  {
      filter =
        (
         ((feattype != null) && (filter_hash.get(feattype) != null)) ||
         (FILTER_OUT_BY_ID && (grouphash.get(featgroup) == null) && (global_symhash.get(featgroup) != null))
         );
    }
    else {
      filter =
        (
         ((feattype != null) && (filter_hash.get(feattype) != null)) ||
         (FILTER_OUT_BY_ID && (global_symhash.get(featid) != null))
         );
    }

    if (filter) {
      //      System.err.println("filtering out, already have a symmetry with same id: " +
      //                         " featgroup = " + featgroup + ", featid = " + featid);
      //      filter_count++;
    }
    else {  // not filtered out

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
      if (max > aseq.getLength()) { aseq.setLength(max); }

      if (featstrand == FORWARD || featstrand == UNKNOWN) {
        //        span.set(min, max, aseq);
        //      current_sym = new MutableSingletonSeqSymmetry(min, max, aseq);
        // GAH 5-21-2003 -- switching to using SingletonSymWithProps to allow
        //    for possibility of attaching note tag/val properties to leaf annotations
        current_sym = new SingletonSymWithProps(min, max, aseq);
      }
      else {  // featstrand == MINUS
        //        span.set(max, min, aseq);
        //      current_sym = new MutableSingletonSeqSymmetry(max, min, aseq);
        current_sym = new SingletonSymWithProps(max, min, aseq);
        //        System.out.println("reversed:" + featcount);
        //        SeqUtils.printSymmetry(current_sym); }
        //        if (featcount <= 10)  { SeqUtils.printSymmetry(current_sym); }
      }
      //    MutableSingletonSeqSymmetry parent_sym = null;
      SingletonSymWithProps parent_sym = null;
      //    SymWithProps grandparent_sym = null;
      SimpleSymWithProps grandparent_sym = null;
      if (feattype != null && MAKE_TYPE_CONTAINER_SYM) {
        grandparent_sym = (SimpleSymWithProps)typehash.get(feattype);
        if (grandparent_sym == null) {
          //        grandparent_sym =
          //          new MutableSingletonSeqSymmetry(current_sym.getStart(), current_sym.getEnd(), aseq);
          grandparent_sym = new SimpleSymWithProps();
          MutableSeqSpan gpspan = new SimpleMutableSeqSpan(current_sym.getStart(),
                                                           current_sym.getEnd(), aseq);
          grandparent_sym.setProperty("method", feattype);
          grandparent_sym.addSpan(gpspan);
          typehash.put(feattype, grandparent_sym);
          aseq.addAnnotation(grandparent_sym);
        }
        else {
          MutableSeqSpan gpspan = (MutableSeqSpan)grandparent_sym.getSpan(aseq);
          SeqUtils.encompass(gpspan, (SeqSpan)current_sym, unionSpan);
          gpspan.set(unionSpan.getStart(), unionSpan.getEnd(), aseq);
        }
      }
      if (featgroup != null) {  // if there is a group id, add annotation to parent annotation
        //      parent_sym = (MutableSingletonSeqSymmetry)grouphash.get(featgroup);
        parent_sym = (SingletonSymWithProps)grouphash.get(featgroup);
        if (parent_sym == null) {
          groupcount++;
          //        parent_sym = new MutableSingletonSeqSymmetry(current_sym.getStart(), current_sym.getEnd(), aseq);
          parent_sym = new SingletonSymWithProps(current_sym.getStart(), current_sym.getEnd(), aseq);
          parent_sym.setProperty("id", featgroup);
          if (feattype != null)  { parent_sym.setProperty("method", feattype); }
          grouphash.put(featgroup, parent_sym);
          global_symhash.put(featgroup, parent_sym);
          if (MAKE_TYPE_CONTAINER_SYM && (grandparent_sym != null))  { grandparent_sym.addChild(parent_sym); }
          else { aseq.addAnnotation(parent_sym); }
          result_syms.add(parent_sym);
        }
        else {
          SeqUtils.encompass((SeqSpan)parent_sym, (SeqSpan)current_sym, unionSpan);
          parent_sym.set(unionSpan.getStart(), unionSpan.getEnd(), aseq);
        }
        parent_sym.addChild(current_sym);
        if (feat_notes != null) {
          Iterator iter = feat_notes.keySet().iterator();
          while (iter.hasNext()) {
            String key = (String)iter.next();
            String val = (String)feat_notes.get(key);
            //          System.out.println("key = " + key + ", val = " + val);
            // for now, adding notes to parent instead of child...
            current_sym.setProperty(key, val);
          }
        }
        if (group_notes != null) {
          Iterator iter = group_notes.keySet().iterator();
          while (iter.hasNext()) {
            String key = (String)iter.next();
            String val = (String)group_notes.get(key);
            //          System.out.println("key = " + key + ", val = " + val);
            // for now, adding notes to parent instead of child...
            parent_sym.setProperty(key, val);
          }
        }
        /*
        if (featlink != null) {
          //        System.out.println("setting link: " + featlink);
          parent_sym.setProperty("link", featlink);
        }
        */
        if (featlink_urls.size() > 0)  {
          Object prev_links = parent_sym.getProperty("link");
          Map links_hash = null;
          String prev_link = null;

          if (prev_links instanceof String) {
            prev_link = (String)prev_links;
          }
          else if (prev_links instanceof Map) {
            links_hash = (Map)prev_links;
          }
          int linkcount = featlink_urls.size();
          if (linkcount == 1 &&
              ((prev_links == null) ||
                ((prev_link != null) && prev_link.equals((String)featlink_urls.get(0))) )  )  {
            parent_sym.setProperty("link", featlink_urls.get(0));
            parent_sym.setProperty("link_name", featlink_urls.get(0));
          }
          else {
            if (links_hash == null) {
              links_hash = new HashMap();
              parent_sym.setProperty("link", links_hash);
              if (prev_link != null) {
                //                links_list.add(prev_link);
                links_hash.put(prev_link, prev_link);
              }
            }
            for (int i=0; i<linkcount; i++) {
              String linkurl = (String)featlink_urls.get(i);
              //              links_list.add(linkurl);
              String linkname = (String)featlink_names.get(i);
              links_hash.put(linkname, linkurl);
            }
          }
        }
      }
      else {  // if no group id, add annotation directly to AnnotatedBioSeq
        global_symhash.put(featid, current_sym);
        //        if (featlink != null) { current_sym.setProperty("link", featlink); }
        if (featlink_urls.size() > 0)  {
          int linkcount = featlink_urls.size();
          for (int i=0; i<linkcount; i++) {
            String linkurl = (String)featlink_urls.get(i);
            parent_sym.setProperty("link", linkurl);
          }
        }
        if (featid != null) { current_sym.setProperty("id", featid); }
        if (feattype != null) { current_sym.setProperty("method", feattype); }
        if (MAKE_TYPE_CONTAINER_SYM && (grandparent_sym != null)) { grandparent_sym.addChild(current_sym); }
        else { aseq.addAnnotation(current_sym); }
        result_syms.add(current_sym);
      }
    }

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
  public void characters(char[] ch, int start, int length) {
    //    System.out.println("***" + new String(ch, start, length) + "&&&");
    //    if (inStartElem || inEndElem) {
    if (current_elem == START || current_elem == END)  {  // parse out integer
      // if no previously collected characters, go ahead and parse as an integer
      //   -- if for some reason more characters are needed, keep adding to already created
      //   integer...,  but watch out for the case where an integer is split near a '0' !
      if (prev_chars && (cached_int != Integer.MIN_VALUE)) {
        int temp_int = parseInt(ch, start, length);
        
        int x = 0; // x is the number of digits in the string used to make temp_int
        while (x <= length && ch[start+x] >= 0x0030 && ch[start+x] <= 0x0039) {
          x++;
        }
        int scale = (int)Math.pow(10, x);
        cached_int = (cached_int * scale) + temp_int;
        
        // Note: If the xml file doesn't include any extraneous white space, then
        // it will always be true that x == length, but counting the characters
        // is more generally safe.
      }
      else {
        cached_int = parseInt(ch, start, length);
      }
      if (current_elem == START) {
        featstart = cached_int;
        // adjusting from "start at 1, base coords" to "start at 0, between coords" numbering
        featstart--;
      }
      else if (current_elem == END) { featend = cached_int; }
    }
    else if (current_elem == ORIENTATION) {
      for (int i=start; i<start+length; i++) {
        if (ch[i] == '+') { featstrand = FORWARD; break; }
        else if (ch[i] == '-') { featstrand = REVERSE; break; }
      }
    }
    else if (current_elem == LINK) {
      String link_name = new String(ch, start, length);
      featlink_names.add(link_name);
    }
    else if (current_elem == NOTE) {
      String note_text = new String(ch, start, length);
      int split_pos = note_text.indexOf("=");
      if (split_pos > 0 && (split_pos < (note_text.length()-1))){
        // assuming parsing out a tag-value pair...
        String tag = note_text.substring(0, note_text.indexOf("="));
        String val = note_text.substring(note_text.indexOf("=")+1);
        if ((val != null) &&
            (val.charAt(0) == '\"') &&
            (val.charAt(val.length()-1) == '\"') ) {
          val = val.substring(1, val.length()-1);
        }
        if (within_group_element) {
          if (group_notes == null) { group_notes = new HashMap(); }
          group_notes.put(tag, val);
        }
        else {
          if (feat_notes == null) { feat_notes = new HashMap(); }
          feat_notes.put(tag, val);
        }
      }
    }
    prev_chars = true;
  }

  public static void main(String[] args) {
    boolean test_result_list = false;
    DasFeat2GenometrySaxParser test = new DasFeat2GenometrySaxParser();
    try {
      String user_dir = System.getProperty("user.dir");
      //      String test_file_name = user_dir + "/testdata/das/dastesting2.xml";
      String test_file_name = user_dir + "/testdata/das/dastesting3.xml";
      File test_file = new File(test_file_name);
      FileInputStream fistr = new FileInputStream(test_file);
      if (test_result_list) {
        InputSource isrc = new InputSource(fistr);
        HashMap result_seqs = new LinkedHashMap();
        List results = test.parseWithResultList(isrc, result_seqs);
        System.out.println("result annotation count: " + results.size());
        System.out.println("first annotation:");
        SeqUtils.printSymmetry((SeqSymmetry)results.get(1));
      }
      else {
        MutableAnnotatedBioSeq seq = test.parse(fistr);
        GenometryViewer viewer = GenometryViewer.displaySeq(seq, false);
        viewer.setAnnotatedSeq(seq);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // trying to parse integers without having to make lots 'o new Strings...
  //
  // adapted from java.lang.Integer, streamlined to assume radix=10,
  //    no NumberFormatExceptions
  //    assumes won't go beyond integer representation limits...
  //    (~ -2.15 billion to ~ +2.14 billion)
  //    public static int parseInt(String s, int radix)
  // assuming only ISO-LATIN numbers
  // 0..9 is 0x0030..0x0039 Unicode
  public static int parseInt(char[] chars, int start, int length) {
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
    }

    else {
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

    return result;
  }

  public static void writeDasFeatHeader(SeqSpan qspan, PrintWriter pw) {
    BioSeq aseq = qspan.getBioSeq();
    String seq_id = aseq.getID();
    int start = qspan.getMin();
    int stop = qspan.getMax();
    String version = "unknown";
    if (aseq instanceof NibbleBioSeq) {
      version = ((NibbleBioSeq)aseq).getVersion();
    }
    pw.println("<?xml version=\"1.0\" standalone=\"no\"?>");
    pw.println("<DASGFF>");
    pw.println("<GFF version=\"1.0\" href=\"dummy href\">");
    pw.println("<SEGMENT id=\"" + seq_id + "\" start=\"" + start + "\"" +
             " stop=\"" + stop + "\" version=\"" + version + "\" >");
  }

  public static void writeDasFeatFooter(PrintWriter pw) {
    pw.println("</SEGMENT>");
    pw.println("</GFF>");
    pw.println("</DASGFF>");
  }

  public static void writeDasFeature(SeqSymmetry annot, BioSeq aseq, String feat_type, PrintWriter pw) {
    if (feat_type == null && annot instanceof SymWithProps) {
      feat_type = (String)((SymWithProps)annot).getProperty("method");
      if (feat_type == null) {
        feat_type = (String)((SymWithProps)annot).getProperty("meth");
      }
      if (feat_type == null) {
        feat_type = (String)((SymWithProps)annot).getProperty("type");
      }
    }
    String group_id = "unknown";
    if (annot instanceof SymWithProps) {
      group_id = (String)((SymWithProps)annot).getProperty("id");
      if (group_id == null) { group_id = "unknown"; }
    }

    //    String group_id = "" + (int)(Math.random() * 1000000000);
    int child_count = annot.getChildCount();

    if (child_count == 0) {
      int i=0;
      SeqSymmetry csym = annot;
      SeqSpan cspan = csym.getSpan(aseq);
      String child_id = group_id + "." + i;
      String orient;
      if (cspan.isForward()) { orient = "+"; }
      else { orient = "-"; }
      pw.println("  <FEATURE id=\"" + child_id + "\" >");
      pw.println("      <TYPE id=\"" + feat_type + "\" />");
      pw.println("      <METHOD id=\"unknown\" />");
      pw.println("      <START>" + (cspan.getMin() +1) + "</START>");  // +1 to compensate for DAS
      pw.println("      <END>" + cspan.getMax() + "</END>");
      pw.println("      <ORIENTATION>" + orient + "</ORIENTATION>");
      pw.println("      <PHASE>-</PHASE>");
      pw.println("      <GROUP id=\"" + group_id + "\" />");
      pw.println("  </FEATURE>");
    }

    else {
      String orient;
      if (annot.getSpan(aseq).isForward()) { orient = "+"; }
      else { orient = "-"; }
      for (int i=0; i<child_count; i++) {
        SeqSymmetry csym = annot.getChild(i);
        SeqSpan cspan = csym.getSpan(aseq);
        String child_id = group_id + "." + i;
        pw.println("  <FEATURE id=\"" + child_id + "\" >");
        pw.println("      <TYPE id=\"" + feat_type + "\" />");
        pw.println("      <METHOD id=\"unknown\" />");
        pw.println("      <START>" + (cspan.getMin() +1) + "</START>");  // +1 to compensate for DAS
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
  public String getMimeType() { return "text/plain"; }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "DASGFF" XML format
   */
  public boolean writeAnnotations(java.util.Collection syms, BioSeq seq,
                                  String type, OutputStream outstream) {
    boolean success = true;
    // for now, assume bounds of query are min/max of syms...
    // for rightnow, assume bounds of query are bounds of seq
    int min = 0;
    int max = seq.getLength();
    SeqSpan qspan = new SimpleSeqSpan(min, max, seq);
    try {
      PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outstream)));
      DasFeat2GenometrySaxParser.writeDasFeatHeader(qspan, pw);
      Iterator iterator = syms.iterator();
      while (iterator.hasNext()) {
        SeqSymmetry annot = (SeqSymmetry)iterator.next();
        DasFeat2GenometrySaxParser.writeDasFeature(annot, seq, type, pw);
      }
      //      System.out.println("annot returned: " + annot_count);
      DasFeat2GenometrySaxParser.writeDasFeatFooter(pw);
      pw.flush();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    }
    return success;
  }
}
