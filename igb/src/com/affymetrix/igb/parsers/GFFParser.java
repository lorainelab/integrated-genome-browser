/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import com.affymetrix.genoviz.util.Memer;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.SeqSymStartComparator;

/**
 *  GFF parser.
 * <pre>
 *  Trying to parse three different forms of GFF:
 *    GFF Version 1.0
 *    GFF Version 2.0
 *    GTF Version ?
 *
 *  GFF format is tab-delimited fields:
 *   <seqname> <source> <feature> <start> <end> <score> <strand> <frame> [attribute] [comment]
 *    where <field> indicates required field, and [field] indicates optional field
 *
 *  for now, assuming that in attribute field their are no backslashed quotes
 *     (in other words, don't see something like "depends on what \"is\" means" )
 *  also, assuming that in attribute field the only ";" characters are for separating
 *     the tag-value sets
 *
 *
 *  GTF is Affy/Neo format, and is GFF v2 with certain restrictions:
 *    ("xyz" line means a line whose feature field == "xyz")
 *    1. All "gene" lines must have a tag-value attribute with tag == "gene_id"
 *        and single value
 *    2. All lines except "gene" lines must have tag-value attributes
 *       with tag == "gene_id" and tag == "transcript_id", each with a single value
 *    3. For each unique "transcript_id" value in tag-value attribtues
 *       over the whole GFF file, there must be a "prim_trans" line
 *
 *  Eventually want to support feature "grouping", and distinguish several special cases:
 *    GFF 1.0, where attributes field (if present) should be a single free-text entry,
 *        which indicates a group to cluster features by
 *    GFF 2.0, where one has prior knowledge of which tag-value entry in attributes
 *        field to use to cluster features by
 *    GTF, where features should be clustered by the value for attribute tag
 *        "transcript_id", and aforementioned restrictions apply
 *  if none of these apply, then don't group features at all
 *  When building genometry models based on GFF, feature "grouping" corresponds to
 *      making some symmetries (the ones to be grouped) into children of other symmetries
 *
  // if loading as new BioSeq, use source id to specify ID of new BioSeq
  // if merging to previous BioSeq, use source id to check for identity with BioSeq
  *
       *  for GTF,
       *  still need to deal with CDS and CDS_insert!!!
       *
       *  CDS_insert means there's bases missing in the genome
       *     for GTF CDS_insert, score field is length of extra bases in transcript/CDS that are
       *     missing from genome
       *  For this kinda stuff, going to need a specific GTF parser that understands
       *     semantics of some of the GFF types (exon, CDS, CDS_insert, etc.) and
       *     can build appropriate genometry models
 *</pre>
 */
public class GFFParser implements AnnotationWriter  {
  // boolean USE_GFF_SYM = true;
  boolean DEBUG_GROUPING = false;
  boolean USE_FILTER = true;
  boolean USE_GROUPING = true;
  boolean SET_LEAF_PROPS = true;
  //  boolean split_groups_across_seqs = true;

  boolean GFF_BASE1 = true;

  // if in GFF 2.0 format, and if attribute field is present, then use first value in first
  //    tag-value entry to group features
  // NOT USING THIS YET
  //  boolean GROUP_BY_FIRST_VALUE = false;
  
  // should only be one tab between each field, but just in case,
  //    allowing for possible multi-tabs
  static final Pattern line_regex = Pattern.compile("\\t+");

  // Note that this simple rule for breaking the string at semicolons doesn't
  // allow for the possibility that some tag's values might contain semicolons
  // inside quotes
  static final Pattern att_regex = Pattern.compile(";");


  // According to http://www.sanger.ac.uk/Software/formats/GFF/GFF_Spec.shtml
  // all tags must match ([A-Za-z][A-Za-z0-9_]*)
  // but we have relaxed this rule (probably inadvertently) to just ([\\w]+)
  // (thus allowing the identifier to start with '_' or a number.)
  static final Pattern tag_regex = Pattern.compile("^\\s*([\\w]+)\\s*");

  // a regular expression to find values for tag-value entries
  // values are either
  //   (1): quote-delimited free text (and this code makes the further
  //          simplifying assumption that there are no backslashed quotes
  //          and no semicolons in the free text)
  //   (2): non-whitespace that doesn't start with a quote (or a whitespace)
  static final Pattern value_regex = Pattern.compile(
      "\\s*\"([^\"]*)\""    /* pattern 1 */
    + "|"                   /* or */
    + "\\s*([^\"\\s]\\S*)"  /* pattern 2 */
  );
  static final Pattern gff1_regex = Pattern.compile("^(\\S+)($|\\t#)");

  // a hash used to filter
  //  Hashtable fail_filter_hash = new Hashtable();
  Map fail_filter_hash = null;
  Map pass_filter_hash = null;

  //  Hashtable group_hash = new Hashtable();

  /*
   *  tag to group features on
   */
  String group_tag = null;

  public GFFParser() {
    this(true);
  }

  /**
   * Constructor.
   * @param coords_are_base1  whether it is necessary to convert from base-1 
   *     numbering to interbase-0 numbering, to agree with genometry.
   */
  public GFFParser(boolean coords_are_base1) {
    GFF_BASE1 = coords_are_base1;
  }
    
  /**
   *  Adds a filter to the fail_filter_hash.
   *  Like {@link #addFeatureFilter(String, boolean)} with pass_filter=false.
   */
  public void addFeatureFilter(String feature_type)  {
    addFeatureFilter(feature_type, false);
  }

  /**
   *  Allows you to specify the entries you want to accept while parsing, or
   *    the ones you want to reject.
   *  When filtering:
   *      1.  if there are any entries in pass_filter, then _only_ features with type entries
   *          in the pass_filter_hash will pass through the filter;
   *      2.  if there are any entries in fail_filter, then _only_ features that do _not_ have
   *          entries in the fail_filter will pass through the filter.
   *
   *  @param pass_filter  if true then add to the pass_filter_hash;
   *    if false then add to the fail_filter_hash
   */
  public void addFeatureFilter(String feature_type, boolean pass_filter) {
    if (pass_filter) {
      if (pass_filter_hash == null) { pass_filter_hash = new HashMap(); }
      pass_filter_hash.put(feature_type, feature_type);
    }
    else {
      if (fail_filter_hash == null) { fail_filter_hash = new HashMap(); }
      fail_filter_hash.put(feature_type, feature_type);
    }
  }

  /**
   *  Removes a filter from the fail_filter_hash.
   *  Like {@link #removeFeatureFilter(String, boolean)} with pass_filter=false.
   */
  public void removeFeatureFilter(String feature_type) {
    removeFeatureFilter(feature_type, false);
  }


  /**
   *  Remove a filter that had been added with {@link #addFeatureFilter(String, boolean)}.
   *  @param pass_filter if true then remove from pass_filter_hash;
   *                if false then remove from fail_filter_hash
   */
  public void removeFeatureFilter(String feature_type, boolean pass_filter) {
    if (pass_filter && (pass_filter_hash != null)) {
      pass_filter_hash.remove(feature_type);
      if (pass_filter_hash.size() == 0) { pass_filter_hash = null; }
    }
    else if (fail_filter_hash != null) {
      fail_filter_hash.remove(feature_type);
      if (fail_filter_hash.size() == 0) { fail_filter_hash = null; }
    }
  }

  public void setGroupTag(String tag) {
    group_tag = tag;
  }

  public MutableAnnotatedBioSeq parse(InputStream istr) throws IOException {
    MutableAnnotatedBioSeq seq = null;
    return parse(istr, seq);
  }

  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq aseq)
  throws IOException {
    Map seqhash = new HashMap();
    if (aseq != null) {
      seqhash.put(aseq.getID(), aseq);
    }
    parse(istr, seqhash);
    if (aseq == null) {
      Iterator iter = seqhash.values().iterator();
      if (iter.hasNext()) { return (MutableAnnotatedBioSeq)iter.next(); }
      else { return null; }
    }
    else {
      return aseq;
    }
  }

  public List parse(InputStream istr, Map seqhash) throws IOException {
    return parse(istr, seqhash, false);
  }

  /**
   *  Note that currently, create_container_annot flag is only applied if
   *  USE_GROUPING is also true.
   **/
  public List parse(InputStream istr, Map seqhash, boolean create_container_annot)
    throws IOException {
    System.out.println("starting GFF parse, create_container_annot: " + create_container_annot);
    //    MutableAnnotatedBioSeq seq = aseq;
    int line_count = 0;
    int sym_count = 0;
    int group_count = 0;
    /*
     *  seq2meths is hash for making container syms (if create_container_annot == true)
     *  each entry in hash is: BioSeq ==> meth2psym hash
     *     Each meth2csym is hash where each entry is "method/source" ==> container_sym
     *  so two-step process to find container sym for a particular meth on a particular seq:
     *    Map meth2csym = (Map)seq2meths.get(seq);
     *    MutableSeqSymmetry container_sym = (MutableSeqSymmetry)meth2csym.get(meth);
     */
    Map seq2meths = new HashMap();
    Map group_hash = new HashMap();
    java.util.List results = new ArrayList();

    BufferedReader br = new BufferedReader(new InputStreamReader(istr));
    try {
      while ((br.ready())) {
	String line = br.readLine();
	if (line == null) { continue; }
	if (line.startsWith("#")) { continue; }
	String fields[] = line_regex.split(line);

	if (fields != null && fields.length >= 8) {
	  line_count++;
	  if ((line_count % 10000) == 0) { System.out.println("" + line_count + " lines processed"); }
          String feature_type = fields[2].intern();

	  // if feature_type is present in fail_filter_hash, skip this line
	  if (USE_FILTER && (fail_filter_hash != null)  && (fail_filter_hash.get(feature_type) != null)) { continue; }
	  // if feature_type is _not_ present in pass_filter_hash, skip this line
	  if (USE_FILTER && (pass_filter_hash != null)  && (pass_filter_hash.get(feature_type) == null)) { continue; }

          String seq_name = fields[0].intern();
          String source = fields[1].intern();
          int start = Integer.parseInt(fields[3]);
          int end = Integer.parseInt(fields[4]);
          String score_str = fields[5];
          String strand_str = fields[6].intern();
          String frame_str = fields[7].intern();

	  String group = null;
	  if (fields.length >= 9)  { group = fields[8].intern(); }

	  float score = Float.NEGATIVE_INFINITY;
	  if (! score_str.equals(".")) { score = Float.parseFloat(score_str); }
	  int frame = Integer.MIN_VALUE;
	  if (! frame_str.equals(".")) { frame = Integer.parseInt(frame_str); }

	  MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)seqhash.get(seq_name);
	  if (seq == null) {
	    seq = new SimpleAnnotatedBioSeq(seq_name, 0);
	    seqhash.put(seq_name, seq);
	  }
	  boolean reverse = (strand_str.equals("-"));
          // could use == instead of equals, because strand has been interned

	  int min = Math.min(start, end);
	  if (GFF_BASE1) {
            // convert from base-1 numbering to interbase-0 numbering,
            //      to agree with genometry
	    min--;
	  }
	  int max = Math.max(start, end);
          
	  SymWithProps sym = new SimpleSymWithProps();
          SimpleSeqSpan span;
          if (reverse)  {
            span = new SimpleSeqSpan(max, min, seq);
          }
          else { span = new SimpleSeqSpan(min, max, seq); }
          ((MutableSeqSymmetry)sym).addSpan(span);
          if (SET_LEAF_PROPS || (! USE_GROUPING)) {
            sym.setProperty("method", source);
            sym.setProperty("type", feature_type);
            if (! (score_str.equals(".")))  { sym.setProperty("score", score_str); }
            if (! (frame_str.equals("."))) { sym.setProperty("frame", frame_str); }
          }
	  Map tagvalue_hash = null;
	  String group_id = null;

	  if (fields.length >= 9) {
	    String attributes = fields[8];
	    // if starts with "#" its a comment, skip attributes processing
	    if (! attributes.startsWith("#")) {
	      // if GFF1, then collect group_id and don't bother with tag-value processing
	      // if attributes is a single non-whitespace entry, then it's GFF 1.0,
	      //   and whole attribute field (after stripping off any # comment on end)
	      //   should be used for grouping
              Matcher gff1_matcher = gff1_regex.matcher(attributes);
	      if (gff1_matcher.matches()) {
		group_id = gff1_matcher.group(1);
		if (DEBUG_GROUPING)  { System.out.println("got a gff1 match: " + group_id); }
	      }
	      // if not GFF1, then assume GFF2 and process attributes field into tag-value(s)
	      else {
		tagvalue_hash = processAttributes(sym, attributes);
	      }
	    }
	  }

	  if (max > seq.getLength()) { seq.setLength(max); }

	  // default if there is no grouping info or if grouping fails for some reason, or if
	  //    USE_GROUPING = false, is to add GFF features directly to AnnotatedBioSeq
	  boolean add_directly = true;

	  // need to add syms to group syms if possible
	  // then add group syms to AnnotatedBioSeq after entire parse is done???
	  //     [otherwise may add a group sym to an AnnotatedBioSeq while the group
	  //      is still growing (it bounds extending and children being added),
	  //      which is okay, except if being incrementally loaded, in which case
	  //      group may get glyphified before it is complete, and right now there's
	  //      no notification mechanism so display can adjust for this...
	  //      Therefore, for now, if using grouping then nothing is added to
	  //      AnnotatedBioSeq until parsing and grouping is completed

	  if (USE_GROUPING)  {
	    // if GFF1, then tagvalue_hash == null, and group_id already assigned if present
	    // if GFF2, then tagvalue_hash != null, so try and find group_id
	    if ((tagvalue_hash != null) && (group_tag != null)) {
	      if (tagvalue_hash != null)  {
		Object value = tagvalue_hash.get(group_tag);
		//		String group_id = null;
		if (DEBUG_GROUPING)  { System.out.println(group_tag + ",  " + value); }
		// currently assuming that if there are multiple values for the group_tag, then take
		//    first one as String value
		if (value != null) {
		  if (value instanceof String) {
		    group_id = (String)value;
		  }
		  else if (value instanceof List) {  // such as a Vector
		    List valist = (List)value;
		    if ((valist.size() > 0) && (valist.get(0) instanceof String)) {
		      group_id = (String)valist.get(0);
		    }
		  }
		}
	      }
	    }
	    // if all of the above has assigned a group_id, then do grouping
	    if (group_id != null) {
	      if (DEBUG_GROUPING)  { System.out.println(group_id); }
	      SimpleSymWithProps groupsym = (SimpleSymWithProps)group_hash.get(group_id);
	      //		System.out.println(groupsym);
	      if (groupsym == null) {
		// make a new groupsym
		groupsym = new SimpleSymWithProps();
		group_count++;
		groupsym.setProperty("group", group_id);
		groupsym.setProperty("method", source);
		//		System.out.println("group id: " + group_id);
		groupsym.setProperty("id", group_id);
		group_hash.put(group_id, groupsym);
		results.add(groupsym);
	      }
	      groupsym.addChild(sym);
	      add_directly = false;
	    }
	  }  // END if (USE_GROUPING)
	  if (add_directly) {
	    // if not grouping (or grouping failed), then add feature directly to AnnotatedBioSeq
	    seq.addAnnotation(sym);
	    results.add(sym);
	  }
	  sym_count++;
	  //	  SeqUtils.printSymmetry(sym);
	}
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    } finally {
      br.close();
    }

    if (USE_GROUPING) {
      Iterator groups = group_hash.values().iterator();
      while (groups.hasNext()) {
	SimpleSymWithProps sym = (SimpleSymWithProps)groups.next();
	String meth = (String)sym.getProperty("method");
	MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)sym.getChild(0).getSpan(0).getBioSeq();
	// stretch sym to bounds of all children
	SeqSpan pspan = SeqUtils.getChildBounds(sym, seq);
	// SeqSpan pspan = SeqUtils.getLeafBounds(sym, seq);  // alternative that does full recursion...
	sym.addSpan(pspan);

	// making sure children are in ascending order of start if forward strand,
	//    descending order of start if reverse strand
	if (seq == null || sym == null) {
	  System.out.println("++++++++++++ warning, sym or seq is nul ++++++++++++");
	}
	resortChildren(sym, seq);

	if (DEBUG_GROUPING)  { SeqUtils.printSymmetry(sym); }
	if (create_container_annot) {
	  Map meth2csym = (Map)seq2meths.get(seq);
	  if (meth2csym == null) {
	    meth2csym = new HashMap();
	    seq2meths.put(seq, meth2csym);
	  }
	  SimpleSymWithProps parent_sym = (SimpleSymWithProps)meth2csym.get(meth);
	  if (parent_sym == null) {
	    parent_sym = new SimpleSymWithProps();
	    parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
	    parent_sym.setProperty("method", meth);
	    seq.addAnnotation(parent_sym);
	    //	    seq2psym.put(seq, parent_sym);
	    meth2csym.put(meth, parent_sym);
	  }
	  parent_sym.addChild(sym);
	}
	else {
	  seq.addAnnotation(sym);
	}
      }
    }

    System.out.println("line count: " + line_count);
    System.out.println("sym count: " + sym_count);
    System.out.println("group count: " + group_count);
    System.out.println("result count: " + results.size());
    //    System.out.println("seq length: " + seq.getLength());
    //    System.out.println("annot count: " + seq.getAnnotationCount());
    return results;
  }

  /**
   *  Resorts child syms of a mutable symmetry in either ascending order if
   *   sym's span on sortseq is forward, or descending if sym's span on sortseq is reverse,
   *   based on child sym's span's start position on BioSeq sortseq.
   */
  public void resortChildren(MutableSeqSymmetry psym, BioSeq sortseq)  {
    SeqSpan pspan = psym.getSpan(sortseq);
    boolean ascending = pspan.isForward();
    //    System.out.println("sortseq: " + sortseq.getID() + ", child list: " + child_count);
    //    System.out.println("sortseq: " + sortseq.getID());
    //    SeqUtils.printSymmetry(psym);
    if (psym.getChildCount() > 0) {
      int child_count = psym.getChildCount();
      java.util.List child_list = new ArrayList(child_count);
      for (int i=0; i<child_count; i++) {
	SeqSymmetry csym = psym.getChild(i);
	if (csym.getSpan(sortseq) != null) {
	  child_list.add(psym.getChild(i));
	}
      }
      psym.removeChildren();
      Comparator comp = new SeqSymStartComparator(sortseq, ascending);
      Collections.sort(child_list, comp);
      int new_child_count = child_list.size();
      for (int i=0; i<new_child_count; i++) {
	psym.addChild((SeqSymmetry)child_list.get(i));
      }
    }
  }



  /**
   *  Parse GFF attributes field into a Hashtable. Each entry is
   *  key = attribute tag, value = attribute values, with following restrictions:
   *    if single value for a key, then hash.get(key) = value
   *    if no value for a key, then hash.get(key) = key
   *    if multiple values for a key, then hash.get(key) = Vector vec,
   *         and each value is an element in vec
   */
  public Map processAttributes(SymWithProps sym, String attributes) {
    Vector vals = new Vector();
    String[] attarray = att_regex.split(attributes);
    for (int i=0; i<attarray.length; i++) {
      String att = attarray[i];
      if (DEBUG_GROUPING)  { System.out.println(att); }
      Matcher tag_matcher = tag_regex.matcher(att);
      if (tag_matcher.find()) {
	String tag = tag_matcher.group(1);
        int index = tag_matcher.end(1);
        Matcher value_matcher = value_regex.matcher(att);
        boolean matches = value_matcher.find(index);
	while (matches) {

          String group1 = value_matcher.group(1);
          String group2 = value_matcher.group(2);
	  if (group1 != null) {
            vals.addElement(group1);
	  }
	  else if (group2 != null) {
            vals.addElement(group2);
	  }
	  else {
	    System.out.println("GOT A PROBLEM");
	  }
          matches = value_matcher.find();
	}
	// common case where there's only one value for a tag,
	//  so hash the tag to that value
	if (vals.size() == 1) {
	  sym.setProperty(tag, vals.elementAt(0));
	  vals.removeAllElements();
	}
	// rare case -- if no value for the tag, hash the tag to itself...
	else if (vals.size() == 0) {
	  sym.setProperty(tag, tag);
	  vals.removeAllElements();
	}
	// not-so-common case where there's multiple values for a tag,
	//   so hash the tag to the Vector/List of all the values,
	//   and make a new Vector for next tag-value entry
	else {
	  sym.setProperty(tag, vals);
	  vals = new Vector();
	}
      }
    }  // end attribute processing
    return sym.getProperties();
  }


  public static void main(String[] args) {
    GFFParser test = new GFFParser();
    String file_name = null;
    if (args.length >= 1)  {
      file_name = args[0];
    }

    System.out.println("filtering introns");
    test.addFeatureFilter("intron");
    test.addFeatureFilter("splice5");
    test.addFeatureFilter("splice3");
    Memer mem = new Memer();
    mem.printMemory();
    java.util.List annots = null;
    try {
      File fl = new File(file_name);
      FileInputStream fistr = new FileInputStream(fl);
      Map seqhash = new HashMap();
      annots = test.parse(fistr, seqhash);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    System.out.println("annots: " + annots.size());
    mem.printMemory();
    System.gc();
    try { Thread.currentThread().sleep(2000); } catch (Exception ex) { }
    mem.printMemory();
  }

  /**
   *  Assumes that the sym being output is of depth = 2 (which UcscPslSyms are).
   */
  public static void outputGffFormat(SymWithProps psym, BioSeq seq, Writer wr)
    throws IOException {
  //  public static void outputGffFormat(UcscPslSym psym, BioSeq seq, Writer wr) throws IOException  {
    int childcount = psym.getChildCount();
    String meth = (String)psym.getProperty("method");
    if (meth == null) { meth = (String)psym.getProperty("type"); }
    //    String id = (String)psym.getProperty("id");
    String group = (String)psym.getProperty("group");
    if (group == null) { group = psym.getID(); }
    if (group == null) { group = (String)psym.getProperty("id"); }

    for (int i=0; i<childcount; i++) {
      SeqSymmetry csym = psym.getChild(i);
      SeqSpan span = csym.getSpan(seq);

      // GFF ==> seqname source feature start end score strand frame group
      wr.write(seq.getID()); // seqname
      wr.write('\t');

      // source
      if (meth != null)  { wr.write(meth); }
      else { wr.write("unknown_source"); }
      wr.write('\t');

      String child_type = null;
      SymWithProps cwp = null;
      if (csym instanceof SymWithProps) {
	cwp = (SymWithProps)csym;
	child_type = (String)cwp.getProperty("type");
      }
      if (child_type != null) { wr.write(child_type); }
      else  { wr.write("unknown_feature_type"); };
      wr.write('\t');

      wr.write(Integer.toString(span.getMin()+1)); wr.write('\t');  // start
      wr.write(Integer.toString(span.getMax())); wr.write('\t');  // end

      // score
      String score = null;
      if (cwp != null) { score = (String)cwp.getProperty("score"); }
      if (score != null) { wr.write(score); }
      else { wr.write("."); }
      wr.write('\t');

      // strand
      if (span.isForward()) { wr.write("+"); } else { wr.write("-"); }
      wr.write('\t');

      // frame
      String frame = null;
      if (cwp != null)  { frame = (String)cwp.getProperty("frame"); }
      if (frame != null) { wr.write(frame); }
      else {  wr.write('.'); }
      wr.write('\t');  // frame

      // group
      //      if (id != null) { wr.write(id); }
      if (group != null) { wr.write(group); }

      wr.write('\n');
    }
  }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "GFF" format.
   *  @param type  currently ignored
   **/
  public boolean writeAnnotations(java.util.Collection syms, BioSeq seq,
				  String type, OutputStream outstream) {
    boolean success = true;
    System.out.println("in GFFParser.writeAnnotations()");
    try {
      Writer bw = new BufferedWriter(new OutputStreamWriter(outstream));
      Iterator iterator = syms.iterator();
      while (iterator.hasNext()) {
	SeqSymmetry sym = (SeqSymmetry)iterator.next();
	if (sym instanceof SymWithProps) {
	  outputGffFormat((SymWithProps)sym, seq, bw);
	}
	else {
	  System.err.println("sym is not instance of SymWithProps");
	}
      }
      bw.flush();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    }
    return success;
  }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "GFF" format.
   **/
  public String getMimeType() { return "text/plain"; }

}
