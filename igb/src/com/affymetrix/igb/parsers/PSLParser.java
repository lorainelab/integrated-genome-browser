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

package com.affymetrix.igb.parsers;

import java.io.*;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Pattern;


import com.affymetrix.genoviz.util.Memer;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.SimpleAnnotatedBioSeq;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.genometry.UcscPslSym;
import com.affymetrix.igb.genometry.Psl3Sym;
import com.affymetrix.igb.genometry.SeqSymmetryConverter;
import com.affymetrix.igb.util.ErrorHandler;

public class PSLParser extends TrackLineParser implements AnnotationWriter  {

  static java.util.List psl_pref_list = new ArrayList();
  static java.util.List psl3_pref_list = new ArrayList();
  static {
    psl_pref_list.add("bps");
    psl_pref_list.add("psl");
    psl3_pref_list.add("psl3");
    psl3_pref_list.add("bps");
    psl3_pref_list.add("psl");
  }

  boolean LOOK_FOR_TARGETS_IN_QUERYHASH = false;
  public boolean DEBUG = false;

  static Pattern line_regex  = Pattern.compile("\t");
  static Pattern comma_regex = Pattern.compile(",");
  static Pattern tagval_regex = Pattern.compile("=");
  static Pattern non_digit = Pattern.compile("[^0-9-]");

  public PSLParser() {
    super();
  }

  public void enableSharedQueryTarget(boolean b) {
    LOOK_FOR_TARGETS_IN_QUERYHASH = b;
  }

  public MutableAnnotatedBioSeq parse(InputStream istr) throws IOException {
    return parse(istr, null, "psl");
  }

  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq aseq)
    throws IOException {
    return parse(istr, aseq, "psl");
  }

  /**
   *  @param aseq should be target seq.
   *  if aseq == null, then try and  return a seq based on contents of PSL file by:
   *    feeding and empty hash for target seqs to other parse() method, which will
   *    populate it with AnnotatedBioSeqs, then return first one that can be retrieved
   *    from the hash
   */
  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq aseq, String meth)
    throws IOException {
    return parse(istr, aseq, meth, false, true);
  }

  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq aseq, String meth,
				      boolean annotate_query, boolean annotate_target)
    throws IOException{
    Map target_hash = new HashMap();
    if (aseq != null) {
      target_hash.put(aseq.getID(), aseq);
    }
    parse(istr, meth, null, target_hash, null, annotate_query, annotate_target);
    if (aseq == null) {
      Iterator iter = target_hash.values().iterator();
      if (iter.hasNext()) { return (MutableAnnotatedBioSeq)iter.next(); }
      else { return null; }
    }
    else {
      return aseq;
    }
  }


  public java.util.List parse(InputStream istr, String annot_type,
			      Map qhash, Map thash,
			      boolean annotate_query, boolean annotate_target)  throws IOException  {
    return parse(istr, annot_type, qhash, thash, null, annotate_query, annotate_target);
  }

  public java.util.List parse(InputStream istr, String annot_type,
			      Map qhash, Map thash, AnnotatedSeqGroup seq_group,
			      boolean annotate_query, boolean annotate_target)  throws IOException  {
    return parse(istr, annot_type, qhash, thash, null, seq_group,
		 annotate_query, annotate_target, false);
  }

  public java.util.List parse(InputStream istr, String annot_type,
			      Map qhash, Map thash,  Map ohash,
			      boolean annotate_query, boolean annotate_target, boolean annotate_other) 
  throws IOException {
    return parse(istr, annot_type, qhash, thash, ohash, null, annotate_query, annotate_target, annotate_other);
  }

  /**
   *  Parse.
   *  For convenience parse() methods that end up calling down to this parse(), the default is:
   *     annotate_query = false;
   *     annotate_target = true.
   *
   *  @param annotate_query   if true, then alignment SeqSymmetries are added to query seq as annotations
   *  @param annotate_target  if true, then alignment SeqSymmetries are added to target seq as annotations
   *
   */
  public java.util.List parse(InputStream istr, String annot_type,
			      Map qhash, Map thash,  Map ohash, AnnotatedSeqGroup seq_group,
			      boolean annotate_query, boolean annotate_target, boolean annotate_other)
    throws IOException {
    return parse(istr, annot_type, false,
		 qhash, thash, ohash, seq_group, 
		 annotate_query, annotate_target, annotate_other);
  }

  public java.util.List parse(InputStream istr, String annot_type,  boolean create_container_annot,
			      Map qhash, Map thash,  Map ohash, AnnotatedSeqGroup seq_group,
			      boolean annotate_query, boolean annotate_target, boolean annotate_other)
    throws IOException {
    System.out.println("in PSLParser.parse(), create_container_annot: " + create_container_annot);
    ArrayList results = new ArrayList();

    // added target2types to accomodate using both container syms and psl with track lines,
    //    because then if using a container sym need to first hash (target2types) from
    //    target seq to another hash (usually referred to as type2csym) of types to container sym
    HashMap target2types = new HashMap();

    // added query2types to accomodate using both container syms and psl with track lines,
    //    because then if using a container sym need to first hash (query2types) from
    //    query seq to another hash (usually referred to as type2csym) of types to container sym
    HashMap query2types = new HashMap();

    // added other2types to accomodate using both container syms and psl with track lines,
    //    because then if using a container sym need to first hash (other2types) from
    //    other seq to another hash (usually referred to as type2csym) of types to container sym
    HashMap other2types = new HashMap();

    Map query_hash = qhash;
    if (query_hash == null) { query_hash = new HashMap(); }
    Map target_hash = thash;
    if (target_hash == null) { target_hash = new HashMap(); }
    Map other_hash = ohash;
    if (other_hash == null) { other_hash = new HashMap(); }
    int line_count = 0;
    //    MutableAnnotatedBioSeq seq = aseq;
    //    Hashtable query_seq_hash = new Hashtable();
    BufferedReader br = new BufferedReader(new InputStreamReader(istr));
    String line = null;
    int childcount = 0;
    int total_annot_count = 0;
    int total_child_count = 0;
    String[] block_size_array = null;
    try {
      while ((line = br.readLine()) != null) {
	line_count++;
        // Ignore psl header lines
	if (line.trim().equals("") || line.startsWith("#") ||
            line.startsWith("match\t") || line.startsWith("-------")) {
          continue;
        }
	else if (line.startsWith("track")) {
	  setTrackProperties(line);
	  continue;
	}
	String[] fields = line_regex.split(line);
	//	System.out.println("line: " + line_count + ", " + line);
	// filtering out header lines (and any other line that doesn't start with a first field of all digits)
	String field0 = fields[0];
	boolean non_digits_present = non_digit.matcher(field0).find(0);
	//	System.out.println("field0: " + field0 + ", nondigits_present: " + non_digits_present);
	if (non_digits_present) { continue; }

	/*
	 *  includes_bin_field is so PSLParser can serve double duty:
	 *  1. for standard PSL files (includes_bin_field = false)
	 *  2. for UCSC PSL-like dump from database, where format has extra ushort field at beginning
	 *       that is used to speed up indexing in db (includes_bin_field = true)
	 */
	//	if (line_count < 3) { System.out.println("# of fields: " + fields.length); }

	// trying to determine if there's an extra bin field at beginning of PSL line...
	//   for normal PSL, orientation field is
        boolean includes_bin_field = (fields[9].startsWith("+") || fields[9].startsWith("-"));
	int findex = 0;
	if (includes_bin_field) { findex++; } // skip bin field at beginning if present

	int match = Integer.parseInt(fields[findex++]);
	int mismatch = Integer.parseInt(fields[findex++]);
	int repmatch = Integer.parseInt(fields[findex++]);
	int n_count = Integer.parseInt(fields[findex++]);
	int q_gap_count = Integer.parseInt(fields[findex++]);
	int q_gap_bases = Integer.parseInt(fields[findex++]);
	int t_gap_count = Integer.parseInt(fields[findex++]);
	int t_gap_bases = Integer.parseInt(fields[findex++]);
	//	    boolean qforward = (fields[findex++].equals("+"));
	String strandstring = fields[findex++];
	boolean same_orientation = true;
	//	boolean tmins_flipped = (strandstring.length() > 1);
	boolean qforward = true;
	boolean tforward = true;
	if (strandstring.length() == 1)  {
	  same_orientation = strandstring.equals("+");
	  qforward = (strandstring.charAt(0) == '+');
	  tforward = true;
	}
	else if (strandstring.length() == 2)  {
	  // need to deal with cases (as mentioned in PSL docs) where
	  //    strand field is "++", "+-", "-+", "--"
	  //  (where first char indicates strand of query, and second is strand for ? [target??]
	  //  for now, just call strand based on them being different,
	  //   so "++" OR "--" ==> forward
	  //      "+-" OR "-+" ==> reverse
	  // current implentation assumes "++", "--", "+-", "-+" are the only possibilities
	  same_orientation = (strandstring.equals("++") || strandstring.equals("--"));
	  qforward = (strandstring.charAt(0) == '+');
	  tforward = (strandstring.charAt(1) == '+');
	}
	else {
	  System.err.println("strand field longer than two characters! ==> " + strandstring);
	}
	//	System.out.println("same orientation: " + same_orientation);

	String qname = fields[findex++];
	int qsize = Integer.parseInt(fields[findex++]);
	int qmin = Integer.parseInt(fields[findex++]);
	int qmax = Integer.parseInt(fields[findex++]);
	String tname = fields[findex++];
	int tsize = Integer.parseInt(fields[findex++]);
	int tmin = Integer.parseInt(fields[findex++]);
	int tmax = Integer.parseInt(fields[findex++]);
	int blockcount = Integer.parseInt(fields[findex++]);

	//	String[] block_size_array = comma_regex.split(fields[findex++]);
	block_size_array = comma_regex.split(fields[findex++]);
	String[] q_start_array = comma_regex.split(fields[findex++]);
	String[] t_start_array = comma_regex.split(fields[findex++]);
	childcount = block_size_array.length;

	// skipping entries that have problems with block_size_array
	if ((block_size_array.length <= 0)  ||
	    (block_size_array[0] == null) ||
	    (block_size_array[0].equals("")) ) {
	  System.err.println("PSLParser found problem with blockSizes list, skipping this line: ");
	  System.err.println(line);
	  continue;
	}
	if (blockcount != block_size_array.length) {
	  System.err.println("PLSParser found disagreement over number of blocks, skipping this line: ");
	  System.err.println(line);
	  continue;
	}

	MutableAnnotatedBioSeq qseq = (MutableAnnotatedBioSeq)query_hash.get(qname);
	if (qseq == null)  {
	  // Doing a new String() here gives a > 4X reduction in
	  //    memory requirements!  Possible reason: Regex machinery when it splits a String into
	  //    an array of Strings may use same underlying character array, so essentially
	  //    end up holding a pointer to a character array containing the whole input file ???
	  //
	  String new_qname = new String(qname);
	  qseq = new SimpleAnnotatedBioSeq(new_qname, qsize);
	  query_hash.put(new_qname, qseq);
	}
	if (qseq.getLength() < qsize) { qseq.setLength(qsize); }

	MutableAnnotatedBioSeq tseq = (MutableAnnotatedBioSeq)target_hash.get(tname);
	if (tseq == null) {
	  if (LOOK_FOR_TARGETS_IN_QUERYHASH && (query_hash.get(tname) != null))  {
	    tseq = (MutableAnnotatedBioSeq)query_hash.get(tname);
	  }
	  else {
	    String new_tname = new String(tname);
	    tseq = new SimpleAnnotatedBioSeq(new_tname, tsize);
	    target_hash.put(new_tname, tseq);
	  }
	}
	if (tseq.getLength() < tsize)  { tseq.setLength(tsize); }

	java.util.List child_arrays = calcChildren(qseq, tseq, qforward, tforward,
						   block_size_array, q_start_array, t_start_array);

	int[] blocksizes = (int[])child_arrays.get(0);
	int[] qmins = (int[])child_arrays.get(1);
	int[] tmins = (int[])child_arrays.get(2);

	String type = (String)getCurrentTrackHash().get("name");
	if (type == null) { type = annot_type; }

	UcscPslSym sym = null;
	boolean is_psl3 = false;
	// trying to handle parsing of extended PSL format for three sequence alignment
	//     (putting into a Psl3Sym)
	// extra fields (immediately after tmins), based on Psl3Sym.outputPsl3Format:
	// same_other_orientation  otherseq_id  otherseq_length  other_min other_max omins
	//    (but omins doesn't have weirdness that qmins/tmins does when orientation = "-")
	if (fields.length > findex &&
	    (fields[findex].equals("+") || fields[findex].equals("-")) ) {
	  // a "+" or "-" in first field after tmins indicates that it's a Psl3 format
	  String otherstrand_string = fields[findex++];
	  boolean other_same_orientation = otherstrand_string.equals("+");
	  String oname = fields[findex++];
	  int osize = Integer.parseInt(fields[findex++]);
	  int omin = Integer.parseInt(fields[findex++]);
	  int omax = Integer.parseInt(fields[findex++]);
	  String[] o_min_array = comma_regex.split(fields[findex++]);
	  int[] omins = new int[childcount];
	  for (int i=0; i<childcount; i++) {
	    omins[i] = Integer.parseInt(o_min_array[i]);
	  }

	  MutableAnnotatedBioSeq oseq = (MutableAnnotatedBioSeq)other_hash.get(oname);
	  if (oseq == null)  {
	    oseq = new SimpleAnnotatedBioSeq(oname, osize);
	    other_hash.put(oname, oseq);
	  }
	  if (oseq.getLength() < osize) { oseq.setLength(osize); }

	  sym = new Psl3Sym(type, match, mismatch, repmatch, n_count,
			    q_gap_count, q_gap_bases, t_gap_count, t_gap_bases,
			    same_orientation, other_same_orientation,
			    qseq, qmin, qmax, tseq, tmin, tmax, oseq, omin, omax,
			    blockcount, blocksizes, qmins, tmins, omins);
	  is_psl3 = true;
	  //	  System.out.println("making PSL3 sym");
	  if (annotate_other) {
	    if (create_container_annot) {
	      Map type2csym = (Map)other2types.get(oseq);
	      if (type2csym == null) {
		type2csym = new HashMap();
		other2types.put(oseq, type2csym);
	      }
	      //	      SimpleSymWithProps other_parent_sym = (SimpleSymWithProps)other2sym.get(tname);
	      SimpleSymWithProps other_parent_sym = (SimpleSymWithProps)type2csym.get(type);
	      if (other_parent_sym == null) {
		other_parent_sym = new SimpleSymWithProps();
		other_parent_sym.addSpan(new SimpleSeqSpan(0, oseq.getLength(), oseq));
		other_parent_sym.setProperty("method", type);
		other_parent_sym.setProperty("preferred_formats", psl3_pref_list);
		oseq.addAnnotation(other_parent_sym);
		//		other2sym.put(tname, other_parent_sym);
		type2csym.put(type, other_parent_sym);
	      }
	      other_parent_sym.addChild(sym);
	    }
	    else {
	      oseq.addAnnotation(sym);
	    }
	  }
	}
	else {
	  sym = new UcscPslSym(type, match, mismatch, repmatch, n_count,
			       q_gap_count, q_gap_bases, t_gap_count, t_gap_bases,
			       same_orientation,
			       qseq, qmin, qmax, tseq, tmin, tmax,
			       blockcount, blocksizes, qmins, tmins);
	}
	//	System.out.println("looking for extra tagval fields");

	// looking for extra tag-value fields at end of line
	if (fields.length > findex) {
	  for (int i=findex; i<fields.length; i++) {
	    String field = fields[i];
	    String[] tagval = tagval_regex.split(field);
	    if (tagval.length >= 2) {
	      String tag = tagval[0];
	      String val = tagval[1];
	      //	      System.out.println("setting property: " + tag + ", " + val);
	      sym.setProperty(tag, val);
	    }
	  }
	}

	if (annotate_query) {
	  if (create_container_annot) {
	    Map type2csym = (Map)query2types.get(qseq);
	    if (type2csym == null) {
	      type2csym = new HashMap();
	      query2types.put(qseq, type2csym);
	    }
	    //	    SimpleSymWithProps query_parent_sym = (SimpleSymWithProps)query2sym.get(qname);
	    SimpleSymWithProps query_parent_sym = (SimpleSymWithProps)type2csym.get(type);
	    if (query_parent_sym == null) {
	      query_parent_sym = new SimpleSymWithProps();
	      query_parent_sym.addSpan(new SimpleSeqSpan(0, qseq.getLength(), qseq));
	      query_parent_sym.setProperty("method", type);
	      if (is_psl3) {
		query_parent_sym.setProperty("preferred_formats", psl3_pref_list);
	      }
	      else {
		query_parent_sym.setProperty("preferred_formats", psl_pref_list);
	      }
	      qseq.addAnnotation(query_parent_sym);
	      //	      query2sym.put(qname, query_parent_sym);
	      type2csym.put(type, query_parent_sym);
	    }
	    query_parent_sym.addChild(sym);
	  }
	  else {
	    qseq.addAnnotation(sym); // GAH commenting out for memory testing 8-20-2003
	  }
	}

	if (annotate_target) {

	  // need to work on adding a top-level symmetry X here which is itself added
	  // (just once, then hashed to keep track of it) to the tseq, and add psl syms as
	  //    children of X (rather than directly to tseq)
	  // and similarly with qseq...
	  //	  System.out.println("annotating target: ");
	  //	  SeqUtils.printSymmetry(sym);
	  if (create_container_annot) {
	    Map type2csym = (Map)target2types.get(tseq);
	    if (type2csym == null) {
	      type2csym = new HashMap();
	      target2types.put(tseq, type2csym);
	    }
	    SimpleSymWithProps target_parent_sym = (SimpleSymWithProps)type2csym.get(type);
	    if (target_parent_sym == null) {
	      target_parent_sym = new SimpleSymWithProps();
	      target_parent_sym.addSpan(new SimpleSeqSpan(0, tseq.getLength(), tseq));
	      target_parent_sym.setProperty("method", type);
	      if (is_psl3) {
		target_parent_sym.setProperty("preferred_formats", psl3_pref_list);
	      }
	      else {
		target_parent_sym.setProperty("preferred_formats", psl_pref_list);
	      }
	      tseq.addAnnotation(target_parent_sym);
	      type2csym.put(type, target_parent_sym);
	    }
	    target_parent_sym.addChild(sym);
	  }
	  else {
	    tseq.addAnnotation(sym);
	  }
	}
	total_annot_count++;
	total_child_count += sym.getChildCount();
	results.add(sym);
        if (seq_group != null) {
          seq_group.addToIndex(sym.getID(), sym);
        }
	if (total_annot_count % 5000 == 0) {
	  System.out.println("current annot count: " + total_annot_count);
	}
      }
    } catch (Exception e) {
      StringBuffer sb = new StringBuffer();
      sb.append("Error parsing PSL file\n");
      sb.append("line count: " + line_count+"\n");
      sb.append("child count: " + childcount+"\n");
      if (block_size_array != null && block_size_array.length != 0) {
        sb.append("block_size first element: **" + block_size_array[0] + "**\n");
      }
      ErrorHandler.errorPanel(sb.toString(), e);

    } finally {
      br.close();
    }
    System.out.println("finished parsing PSL file, annot count: " + total_annot_count +
		       ", child count: " + total_child_count);
    return results;
  }


  public java.util.List calcChildren(BioSeq qseq, BioSeq tseq, boolean qforward, boolean tforward,
				     String[] blocksize_strings,
				     String[] qstart_strings, String[] tstart_strings) {
    int childCount = blocksize_strings.length;
    if (qstart_strings.length != childCount || tstart_strings.length != childCount) {
      System.out.println("array counts for block sizes, q starts, and t starts don't agree, " +
			 "skipping children");
      return null;
    }
    int[] blocksizes = new int[childCount];
    int[] qmins = new int[childCount];
    int[] tmins = new int[childCount];

    if (childCount > 0) {
      //      System.out.println("sameorientation: " + same_orientation);
      int qseq_length = qseq.getLength();
      int tseq_length = tseq.getLength();

      if (qforward && tforward) { // query = forward, target = forward
	for (int i=0; i<childCount; i++) {
	  int match_length = Integer.parseInt(blocksize_strings[i]);
	  int qstart = Integer.parseInt(qstart_strings[i]);
	  int tstart = Integer.parseInt(tstart_strings[i]);
	  blocksizes[i] = match_length;
	  qmins[i] = qstart;
	  tmins[i] = tstart;
	}
      }
      else if ((! qforward) && (tforward)) { // query = reverse, target = forward
	for (int i=0; i<childCount; i++)  {
	  int string_index = childCount-i-1;
	  int match_length = Integer.parseInt(blocksize_strings[string_index]);
	  int qstart = qseq_length - Integer.parseInt(qstart_strings[string_index]);
	  int tstart = Integer.parseInt(tstart_strings[string_index]);
	  int qend = qstart - match_length;
	  blocksizes[i] = match_length;
	  qmins[i] = qend;
	  tmins[i] = tstart;
	}
      }
      else if ((qforward) && (! tforward)) {  // query = forward, target = reverse
	for (int i=0; i<childCount; i++)  {
	  int match_length = Integer.parseInt(blocksize_strings[i]);
	  int qstart = Integer.parseInt(qstart_strings[i]);
	  int tstart = tseq_length - Integer.parseInt(tstart_strings[i]);
	  int tend = tstart - match_length;
	  blocksizes[i] = match_length;
	  qmins[i] = qstart;
	  tmins[i] = tend;
	}
      }
      else { // query = reverse, target = reverse
	for (int i=0; i<childCount; i++)  {
	  int string_index = childCount-i-1;
	  int match_length = Integer.parseInt(blocksize_strings[string_index]);
	  int qstart = qseq_length - Integer.parseInt(qstart_strings[string_index]);
	  int tstart = tseq_length - Integer.parseInt(tstart_strings[string_index]);
	  int qend = qstart - match_length;
	  int tend = tstart - match_length;
	  blocksizes[i] = match_length;
	  qmins[i] = qend;
	  tmins[i] = tend;
	}
      }
    }
    java.util.List results = new ArrayList(3);
    results.add(blocksizes);
    results.add(qmins);
    results.add(tmins);
    return results;
  }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "PSL" format
   **/
  public boolean writeAnnotations(java.util.Collection syms, BioSeq seq,
				  String type, OutputStream outstream) {
    return writeAnnotations(syms, seq, false, type, null, outstream);
  }

  /**
   *  This version of the method is able to write out track lines
   **/
  public boolean writeAnnotations(java.util.Collection syms, BioSeq seq,
				  boolean writeTrackLines, String type,
                                  String description, OutputStream outstream) {
    boolean success = true;
    try {
      //    response.setContentType("text/psl");
      //    PrintWriter pw = outstream.getWriter();
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outstream));
      if (writeTrackLines) {
        bw.write ("track");
        if (type != null) {
          bw.write (" name=\"" + type + "\"");
        }
        if (description != null) {
          bw.write (" description=\"" + description + "\"");
        }
        bw.newLine();
      }

      Iterator iterator = syms.iterator();
      while (iterator.hasNext()) {
	SeqSymmetry sym = (SeqSymmetry)iterator.next();
	if (! (sym instanceof UcscPslSym)) {
	  int spancount = sym.getSpanCount();
	  if (sym.getSpanCount() == 1) {
	    sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq, seq);
	  }
	  else {
	    BioSeq seq2 = SeqUtils.getOtherSeq(sym, seq);
	    sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq, seq2);
	  }
	}
	((UcscPslSym)sym).outputPslFormat(bw);
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
   *    to an output stream as "PSL" format
   **/
  public String getMimeType() { return "text/plain"; }


  public void calcLengthDistribution(String input_file) {
    int bincount = 100;
    // log10(n) = log(n) / log(10).
    double logOf10 = Math.log(10);
    int[] bins = new int[bincount];
    for (int i=0; i<bincount; i++) { bins[i] = 0; }
    int line_count = 0;
    int locount = 0;
    int hi1count = 0;
    int hi2count = 0;
    double total_coverage = 0;
    double total_nolo_coverage = 0;
    try {
      FileInputStream str = new FileInputStream(new File(input_file));
      BufferedReader br = new BufferedReader(new InputStreamReader(str));
      String line = null;
      while ((line = br.readLine()) != null) {
	String[] fields = line_regex.split(line);
	int length = Integer.parseInt(fields[16]) - Integer.parseInt(fields[15]);
	//	int bindex = (int)Math.log(length);
	// log10(n) = log(n) / log(10).
	double log10 = Math.log(length)/logOf10;
	int bindex = (int)(log10 * 10);

	bins[bindex]++;
	if (length < 1000) { locount++; }
	else {
	  total_nolo_coverage += length;
	  if (length > 100000) { hi1count++; }
	  if (length > 1000000) { hi2count++; }
	}
	total_coverage += length;

	//	System.out.println("length: " + length);
	line_count++;
	//	if (line_count > 100) { break; };
	if ((line_count % 10000) == 0) {
	  System.out.println("current line: " + line_count);
	}
      }
      br.close();
      str.close();

      int avg_length = (int)(total_coverage / line_count);
      int avg_length_nolo = (int)(total_nolo_coverage / (line_count - locount));

      System.out.println("total count: " + line_count);
      System.out.println(" < 1000 bp count: " + locount);
      System.out.println(" > 100000 bp count: " + hi1count);
      System.out.println(" > 1000000 bp count: " + hi2count);
      System.out.println("average size: " + avg_length);
      System.out.println("average size > 1000: " + avg_length_nolo);
      for (int i=0; i<bincount; i++) {
	System.out.println("" + (i/10.0) + "\t" + bins[i]);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }



  public static void main(String[] args) {
    boolean calc_lengths = false;
    PSLParser test = new PSLParser();
    String file_name = args[0];

    if (calc_lengths) {
      test.calcLengthDistribution(file_name);
    }
    else {
      if (args != null && args.length > 0) {
	file_name = args[0];
      }
      System.out.println("file name = "+file_name);

      test.DEBUG = true;
      test.enableSharedQueryTarget(true);

      Memer mem = new Memer();
      mem.printMemory();
      java.util.List results = null;
      Map query_hash = new HashMap();
      Map target_hash = new HashMap();
      try {
	File fl = new File(file_name);
	FileInputStream fistr = new FileInputStream(fl);
	//      seq = test.parse(fistr, null);
	//	results = test.parse(fistr, "psl_test", query_hash, target_hash, true, true);
	// trying with containers...
	results = test.parse(fistr, "psl_test", true,
			     query_hash, target_hash, null, null, true, true, false);
	fistr.close();
	int acount = results.size();
	System.out.println("Results: annotation count = " + acount);
	for (int i=0; i<acount; i++) {
	  SeqSymmetry sym = (SeqSymmetry)results.get(i);

	  // Sometimes, in testing, it can be useful to trim the sym to see if the
	  // coordinates change as a result
	  //MutableSeqSymmetry tempsym = SeqUtils.copyToDerived(sym);
	  //SeqUtils.trim(tempsym);
	  //sym = tempsym;
	  SeqUtils.printSymmetry(sym, "+  ", true);
	}

	System.out.println("Annotated Sequences: ");
	Map prevseqs = new HashMap();
	for (int i=0; i<acount; i++) {
	  SeqSymmetry sym = (SeqSymmetry)results.get(i);
	  int spancount = sym.getSpanCount();
	  for (int k=0; k<spancount; k++) {
	    SeqSpan span = sym.getSpan(k);
	    BioSeq spanseq = span.getBioSeq();
	    if (prevseqs.get(spanseq) == null) {  // want to only print out each seq once...
	      if (spanseq instanceof AnnotatedBioSeq) {
		AnnotatedBioSeq aseq = (AnnotatedBioSeq)spanseq;
		System.out.println("***************************************************************");
		System.out.println("seq = " + aseq.getID() +
				   ", annotations = " + aseq.getAnnotationCount() +
				   ", " + aseq);
		int mcount = aseq.getAnnotationCount();
		for (int m=0; m<mcount; m++) {
		  SeqSymmetry asym = aseq.getAnnotation(m);
		  System.out.println("Annotation " + m);
		  SeqUtils.printSymmetry(asym, "|  ", true);
		}
	      }
	      prevseqs.put(spanseq, spanseq);
	    }
	  }
	}

      }
      catch (Exception ex) {
	ex.printStackTrace();
      }
    }
  }

}

