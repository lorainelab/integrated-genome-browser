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
import java.util.*;
import java.util.regex.Pattern;


import com.affymetrix.genoviz.util.Memer;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.UcscPslSym;
import com.affymetrix.igb.genometry.Psl3Sym;
import com.affymetrix.igb.genometry.SeqSymmetryConverter;

public class PSLParser implements AnnotationWriter  {

  static java.util.List psl_pref_list = new ArrayList();
  static java.util.List psl3_pref_list = new ArrayList();
  static {
    psl_pref_list.add("bps");
    psl_pref_list.add("psl");
    
    psl3_pref_list.add("psl3");
    psl3_pref_list.add("bps");
    psl3_pref_list.add("psl");
  }

  boolean look_for_targets_in_query_group = false;
  boolean create_container_annot = false;
  boolean is_link_psl = false;
  public boolean DEBUG = false;

  static Pattern line_regex  = Pattern.compile("\t");
  static Pattern comma_regex = Pattern.compile(",");
  static Pattern tagval_regex = Pattern.compile("=");
  static Pattern non_digit = Pattern.compile("[^0-9-]");

  TrackLineParser track_line_parser = new TrackLineParser();
  
  public PSLParser() {
    super();
  }

  public void enableSharedQueryTarget(boolean b) {
    look_for_targets_in_query_group = b;
  }
  
  public void setCreateContainerAnnot(boolean b) {
    create_container_annot = b;
  }
  
  /**
   *  Whether or not to add new seqs from the file to the target AnnotatedSeqGroup.
   *  Normally true; set this to false for "link.psl" files.
   */
  public void setIsLinkPsl(boolean b) {
    is_link_psl = b;
  }

  public java.util.List parse(InputStream istr, String annot_type,
			      AnnotatedSeqGroup query_group, AnnotatedSeqGroup target_group,
			      boolean annotate_query, boolean annotate_target)  throws IOException  {
    return parse(istr, annot_type, query_group, target_group, null, 
      annotate_query, annotate_target, false);
  }

  /**
   *  Parse.
   *  The most common parameters are:
   *     annotate_query = false;
   *     annotate_target = true;
   *     annotate_other = false.
   *
   *  @param istr             An input stream
   *  @param annot_type       The method name for the annotation to load from the file, if the track line is missing;
   *                          if there is a track line in the file, the name from the track line will be used instead.
   *  @param query_group      An AnnotatedSeqGroup (or null) to look for query SeqSymmetries in and add SeqSymmetries to.
   *                          Null is ok; this will cause a temporary AnnotatedSeqGroup to be created.
   *  @param target_group      An AnnotatedSeqGroup (or null) to look for target SeqSymmetries in and add SeqSymmetries to.
   *  @param other_group      An AnnotatedSeqGroup (or null) to look for other SeqSymmetries in (in PSL3 format) and add SeqSymmetries to.
   *                          This parameter is ignored if the file is not in psl3 format.
   *  @param annotate_query   if true, then alignment SeqSymmetries are added to query seq as annotations
   *  @param annotate_target  if true, then alignment SeqSymmetries are added to target seq as annotations
   *  @param annotate_other   if true, then alignment SeqSymmetries (in PSL3 format files) are added to other seq as annotations
   *
   */
  public java.util.List parse(InputStream istr, String annot_type,
    AnnotatedSeqGroup query_group, AnnotatedSeqGroup target_group, AnnotatedSeqGroup other_group,
    boolean annotate_query, boolean annotate_target, boolean annotate_other)
    throws IOException {
    
    System.out.println("in PSLParser.parse(), create_container_annot: " + create_container_annot);
    ArrayList results = new ArrayList();

    // Make temporary seq groups for any unspecified group.
    // These temporary groups do not require synonym matching, because they should
    // only refer to sequences from a single file.
    if (query_group == null) {
      query_group = new AnnotatedSeqGroup("Query");
      query_group.setUseSynonyms(false);
    }
    if (target_group == null) {
      target_group = new AnnotatedSeqGroup("Target"); 
      target_group.setUseSynonyms(false);
    }
    if (other_group == null) {
      other_group = new AnnotatedSeqGroup("Other");
      other_group.setUseSynonyms(false);
    }
    
    boolean in_bottom_of_link_psl = false;
    
    // the three xxx2types Maps accomodate using create_container_annot and psl with track lines.
    HashMap target2types = new HashMap();
    HashMap query2types = new HashMap();
    HashMap other2types = new HashMap();

    int line_count = 0;
    //    MutableAnnotatedBioSeq seq = aseq;
    //    Hashtable query_seq_hash = new Hashtable();
    BufferedReader br = new BufferedReader(new InputStreamReader(istr));
    String line = null;
    int childcount = 0;
    int total_annot_count = 0;
    int total_child_count = 0;
    String[] block_size_array = null;
    Thread thread = Thread.currentThread();
    try {
      while ((line = br.readLine()) != null && (! thread.isInterrupted())) {
	line_count++;
        // Ignore psl header lines
	if (line.trim().equals("") || line.startsWith("#") ||
            line.startsWith("match\t") || line.startsWith("-------")) {
          continue;
        }
	else if (line.startsWith("track")) {
	  Map track_props = track_line_parser.setTrackProperties(line);
          if (is_link_psl) {
            String track_name = (String) track_props.get("name");
            if (track_name != null && track_name.endsWith("probesets")) {
              in_bottom_of_link_psl = true;
            }
          }
          // You can later get the track properties with getCurrentTrackHash();
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
        
        MutableAnnotatedBioSeq qseq = query_group.getSeq(qname);
	if (qseq == null)  {
	  // Doing a new String() here gives a > 4X reduction in
	  //    memory requirements!  Possible reason: Regex machinery when it splits a String into
	  //    an array of Strings may use same underlying character array, so essentially
	  //    end up holding a pointer to a character array containing the whole input file ???
	  //
          qseq = query_group.addSeq(new String(qname), qsize);
	}
	if (qseq.getLength() < qsize) { qseq.setLength(qsize); }


 	MutableAnnotatedBioSeq tseq = target_group.getSeq(tname);
	if (tseq == null) {
	  if (look_for_targets_in_query_group && (query_group.getSeq(tname) != null))  {
	    tseq = query_group.getSeq(tname);
	  }
	  else {
            if (look_for_targets_in_query_group && is_link_psl) {
              // If we are in the bottom section of a ".link.psl" file,
              // then add sequences only to the query sequence, never the target sequence.
              if (in_bottom_of_link_psl) {
                tseq = query_group.addSeq(new String(tname), qsize);
              } else {
                tseq = target_group.addSeq(new String(tname), qsize);                
              }
            } else {
              tseq = target_group.addSeq(new String(tname), qsize);              
            }
	  }
	}
	if (tseq.getLength() < tsize)  { tseq.setLength(tsize); }
          

	java.util.List child_arrays = calcChildren(qseq, tseq, qforward, tforward,
						   block_size_array, q_start_array, t_start_array);

	int[] blocksizes = (int[])child_arrays.get(0);
	int[] qmins = (int[])child_arrays.get(1);
	int[] tmins = (int[])child_arrays.get(2);

	String type = (String) track_line_parser.getCurrentTrackHash().get("name");
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
	  is_psl3 = true;
          
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

	  MutableAnnotatedBioSeq oseq = other_group.getSeq(oname);
	  if (oseq == null)  {
            oseq = other_group.addSeq(new String(oname), osize);
	  }
	  if (oseq.getLength() < osize) { oseq.setLength(osize); }

	  sym = new Psl3Sym(type, match, mismatch, repmatch, n_count,
			    q_gap_count, q_gap_bases, t_gap_count, t_gap_bases,
			    same_orientation, other_same_orientation,
			    qseq, qmin, qmax, tseq, tmin, tmax, oseq, omin, omax,
			    blockcount, blocksizes, qmins, tmins, omins);

          if (annotate_other) {
            if (create_container_annot) {
              createContainerAnnot(other2types, oseq, type, sym, is_psl3);
            } else {
              oseq.addAnnotation(sym);
            }          
            other_group.addToIndex(sym.getID(), sym);
          }
	}
	else {
	  sym = new UcscPslSym(type, match, mismatch, repmatch, n_count,
			       q_gap_count, q_gap_bases, t_gap_count, t_gap_bases,
			       same_orientation,
			       qseq, qmin, qmax, tseq, tmin, tmax,
			       blockcount, blocksizes, qmins, tmins);
	}
        
	// looking for extra tag-value fields at end of line
	if (fields.length > findex) {
	  for (int i=findex; i<fields.length; i++) {
	    String field = fields[i];
	    String[] tagval = tagval_regex.split(field);
	    if (tagval.length >= 2) {
	      String tag = tagval[0];
	      String val = tagval[1];
	      sym.setProperty(tag, val);
	    }
	  }
	}

        if (annotate_query) {
          if (create_container_annot) {
            createContainerAnnot(query2types,qseq,type,sym,is_psl3);
          } else {
            qseq.addAnnotation(sym);
          }
          query_group.addToIndex(sym.getID(), sym);
        }

	if (annotate_target) {
	  if (create_container_annot) {
            createContainerAnnot(target2types, tseq, type, sym, is_psl3);
	  }
	  else {
	    tseq.addAnnotation(sym);
	  }
          if (! in_bottom_of_link_psl) {
            target_group.addToIndex(sym.getID(), sym);
          }
	}

        total_annot_count++;
	total_child_count += sym.getChildCount();
	results.add(sym);

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
      IOException ioe = new IOException(sb.toString());
      ioe.initCause(e);
      throw ioe;
    } finally {
      br.close();
    }
    System.out.println("finished parsing PSL file, annot count: " + total_annot_count +
		       ", child count: " + total_child_count);
    return results;
  }
    
  static void createContainerAnnot(Map seq2types, MutableAnnotatedBioSeq seq, String type, SeqSymmetry sym, boolean is_psl3) {
    //    If using a container sym, need to first hash (seq2types) from
    //    seq to another hash (type2csym) of types to container sym
    Map type2csym = (Map) seq2types.get(seq);
    if (type2csym == null) {
      type2csym = new HashMap();
      seq2types.put(seq, type2csym);
    }
    SimpleSymWithProps parent_sym = (SimpleSymWithProps)type2csym.get(type);
    if (parent_sym == null) {
      parent_sym = new SimpleSymWithProps();
      parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
      parent_sym.setProperty("method", type);
      if (is_psl3) {
        parent_sym.setProperty("preferred_formats", psl3_pref_list);
      } else {
        parent_sym.setProperty("preferred_formats", psl_pref_list);
      }
      seq.addAnnotation(parent_sym);
      type2csym.put(type, parent_sym);
    }
    parent_sym.addChild(sym);
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
	  if (spancount == 1) {
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
      AnnotatedSeqGroup query_seq_group = new AnnotatedSeqGroup("Query");
      AnnotatedSeqGroup target_seq_group = new AnnotatedSeqGroup("Target");
      
      try {
	File fl = new File(file_name);
	FileInputStream fistr = new FileInputStream(fl);
        test.setCreateContainerAnnot(true);

        results = test.parse(fistr, "psl_test",
          query_seq_group, target_seq_group, null, 
          true, true, false);

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

