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
import com.affymetrix.genoviz.util.Timer;

import java.util.Comparator;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.UcscPslSym;
import com.affymetrix.igb.genometry.UcscPslComparator;
import com.affymetrix.igb.genometry.SeqSpanComparator;
import com.affymetrix.igb.genometry.SeqSymmetryConverter;
import com.affymetrix.igb.parsers.PSLParser;
import com.affymetrix.igb.parsers.AnnotationWriter;

public class BpsParser implements AnnotationWriter  {

  static java.util.List pref_list = new ArrayList();
  static {
    pref_list.add(".bps");
    pref_list.add(".psl");
  }

  static boolean main_batch_mode = false; // main() should run in batch mode (processing PSL files in psl_input_dir)
  static boolean write_from_text = true; // main() should read psl file from text_file and write bps to bin_file
  static boolean read_from_bps = false;  // main() should read bps file bin_file
  static boolean use_byte_buffer = true;
  static boolean REPORT_LOAD_STATS = true;

  // mod_chromInfo.txt is same as chromInfo.txt, except entries have been arranged so
  //   that all random, etc. bits are at bottom
  static String user_dir = System.getProperty("user.dir");

  // .bps is for "binary PSL format"
  static String default_annot_type = "spliced_EST";

  /*
   *  new alternative
   *  given a psl_input_dir:
   *     for each file in psl_input_dir,
   *        if file ends with ".psl"
   *             assume its a PSL file
   *             load via PSLParser.parse()
   *             output via BpsParser.writeBinaryFile()
   */
  static String psl_input_dir = user_dir + "/moredata/Drosophila_Jan_2003/";
  static String bps_output_dir = user_dir + "/query_server_dro/Drosophila_Jan_2003/";

  /*  PSL format fields (from com.affymetrix.igb.genometry.UcscPslSym)
  int matches;
  int mismatches;
  int repmatches; // should be derivable w/o residues
  int ncount;
  int qNumInsert;  // should be derivable w/o residues
  int qBaseInsert; // should be derivable w/o residues
  int tNumInsert;  // should be derivable w/o residues
  int tBaseInsert; // should be derivable w/o residues
  boolean qforward;
  boolean tforward;  // for mouse only???
  String qname;
  int qsize;
  int qmin;
  int qmax;
  String tname;
  int tsize;
  int tmin;
  int tmax;
  int blockcount; // should be redundant
  int[] blockSizes;
  int[] qmins;
  int[] qmaxs;
  */

  static int estimated_count = 80000;

  public static void main(String[] args) {
    //    BpsParser test = new BpsParser();
    if (write_from_text) {
      if (main_batch_mode) {
	File input_dir = new File(psl_input_dir);
	File[] fils = input_dir.listFiles();
	for (int i=0; i<fils.length; i++) {
	  File fil = fils[i];
	  String in_path = fil.getPath();
	  String in_name = fil.getName();
	  if (in_name.endsWith(".psl")) {
	    System.out.println("processing PSL file: " + in_path);
	    String barename;
	    if (in_name.endsWith(".psl.psl")) {
	      barename = in_name.substring(0, in_name.lastIndexOf(".psl.psl"));
	    }
	    else {
	      barename = in_name.substring(0, in_name.lastIndexOf(".psl"));
	    }
	    System.out.println("bare name: " + barename);
	    String out_path = bps_output_dir + barename + ".bps";
	    System.out.println("output file: " + out_path);
	    convertPslToBps(in_path, out_path);
	  }
	}
      }
      else {
	if (args.length == 2) {
	  String text_file = args[0];
	  String bin_file = args[1];
	  convertPslToBps(text_file, bin_file);
	}
	else {
	  System.out.println("Usage:  java ... BpsParser <text infile> <binary outfile>");
	  System.exit(1);
	}
      }
    }
    if (read_from_bps) {
      Map chrom_hash = new HashMap();
      String bin_file = args[0];
      java.util.List syms = parse(bin_file, default_annot_type, chrom_hash);
      int symcount = syms.size();
      System.out.println("total sym count: " + symcount);
      int[] blockcount = new int[100];
      for (int i=0; i<symcount; i++) {
	SeqSymmetry sym = (SeqSymmetry)syms.get(i);
	int childcount = sym.getChildCount();
	blockcount[childcount]++;
      }
      for (int i=0; i<blockcount.length; i++) {
	if (blockcount[i] != 0) {
	  System.out.println("syms with " + i + " children: " + blockcount[i]);
	}
      }
    }
  }


  public static void convertPslToBps(String psl_in, String bps_out)  {
    System.out.println("reading text psl file");
    java.util.List psl_syms = readPslFile(psl_in);
    System.out.println("done reading text psl file, annot count = " + psl_syms.size());
    System.out.println("writing binary psl file");
    //    writeBpsFile(psl_syms, bps_out);
    writeBinary(bps_out, psl_syms);
    System.out.println("done writing binary psl file");
  }


  /**
   *  @param target_hash  a HashMap of target (chromosome) names to MutableAnnotatedBioSeqs
   *    that represent the targets (chromosomes)
   */
  public static java.util.List parse(String file_name, String annot_type, Map target_hash) {
    System.out.println("loading file: " + file_name);
    try {
      File fil = new File(file_name);
      long flength = fil.length();
      FileInputStream fis = new FileInputStream(fil);
      //      BufferedInputStream bis = new BufferedInputStream(fis, 16384);
      BufferedInputStream bis = new BufferedInputStream(fis);
      DataInputStream dis = null;

      if (use_byte_buffer) {
	byte[] bytebuf = new byte[(int)flength];
	bis.read(bytebuf);
	//	fis.read(bytebuf);
	ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
	dis = new DataInputStream(bytestream);
      }
      else {
	dis = new DataInputStream(bis);
      }
      return parse(dis, annot_type, target_hash);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public static java.util.List parse(DataInputStream dis, String annot_type, Map target_hash) {
    return parse(dis, annot_type, null, target_hash, false, true);
  }

  /** Reads binary PSL data from the given stream.  Note that this method <b>can</b>
   *  be interrupted early by Thread.interrupt().  The input stream will always be closed
   *  before exiting this method.
   */
  public static java.util.List parse(DataInputStream dis, String annot_type,
				      Map qhash, Map thash, boolean annot_query, boolean annot_target) {
    Map query_hash = qhash;
    Map target_hash = thash;
    if (query_hash == null) { query_hash = new HashMap(); }
    if (target_hash == null) { target_hash = new HashMap(); }
    int total_block_count = 0;
    HashMap target2sym = new HashMap(); // maps target chrom name to top-level symmetry
    HashMap query2sym = new HashMap(); // maps query chrom name to top-level symmetry
    ArrayList results = new ArrayList(estimated_count);
    int count = 0;
    int same_count = 0;
    Timer tim = new Timer();
    tim.start();
    boolean reached_EOF = false;
    try {
      Thread thread = Thread.currentThread();
      // Loop will usually be ended by EOFException, but
      // can also be interrupted by Thread.interrupt()
      while (! thread.isInterrupted()) {
	int matches = dis.readInt();
	int mismatches = dis.readInt();
	int repmatches = dis.readInt();
	int ncount = dis.readInt();
	int qNumInsert = dis.readInt();
	int qBaseInsert = dis.readInt();
	int tNumInsert = dis.readInt();
	int tBaseInsert = dis.readInt();
	boolean qforward = dis.readBoolean();
	String qname = dis.readUTF();
	int qsize = dis.readInt();
	int qmin = dis.readInt();
	int qmax = dis.readInt();
	BioSeq queryseq = (BioSeq)query_hash.get(qname);
	if (queryseq == null)  {
	  queryseq = new SimpleAnnotatedBioSeq(qname, qsize);
	  query_hash.put(qname, queryseq);
	}

	String tname = dis.readUTF();
	int tsize = dis.readInt();
	int tmin = dis.readInt();
	int tmax = dis.readInt();
	BioSeq targetseq = (BioSeq)target_hash.get(tname);
	if (targetseq == null) {
	  targetseq = new SimpleAnnotatedBioSeq(tname, tsize);
	  target_hash.put(tname, targetseq);
	}

	int blockcount = dis.readInt();
	int[] blockSizes = new int[blockcount];
	int[] qmins = new int[blockcount];
	int[] tmins = new int[blockcount];
	for (int i=0; i<blockcount; i++) {
	  blockSizes[i] = dis.readInt();
	}
	for (int i=0; i<blockcount; i++) {
	  qmins[i] = dis.readInt();
	}
	for (int i=0; i<blockcount; i++) {
	  tmins[i] = dis.readInt();
	}
	total_block_count += blockcount;
	count++;

	UcscPslSym sym =
	  new UcscPslSym(annot_type, matches, mismatches, repmatches, ncount,
			 qNumInsert, qBaseInsert, tNumInsert, tBaseInsert, qforward,
			 queryseq, qmin, qmax, targetseq, tmin, tmax,
			 blockcount, blockSizes, qmins, tmins);
	results.add(sym);

	if (annot_query && (queryseq instanceof MutableAnnotatedBioSeq)) {
	  SimpleSymWithProps query_parent_sym = (SimpleSymWithProps)query2sym.get(qname);
	  if (query_parent_sym == null) {
	    query_parent_sym = new SimpleSymWithProps();
	    query_parent_sym.addSpan(new SimpleSeqSpan(0, queryseq.getLength(), queryseq));
	    query_parent_sym.setProperty("method", annot_type);
	    query_parent_sym.setProperty("preferred_formats", pref_list);
	    ((MutableAnnotatedBioSeq)queryseq).addAnnotation(query_parent_sym);
	    query2sym.put(qname, query_parent_sym);
	  }
	  query_parent_sym.addChild(sym);
	}

	if (annot_target && (targetseq instanceof MutableAnnotatedBioSeq)) {
	  SimpleSymWithProps target_parent_sym = (SimpleSymWithProps)target2sym.get(tname);
	  if (target_parent_sym == null) {
	    target_parent_sym = new SimpleSymWithProps();
	    target_parent_sym.addSpan(new SimpleSeqSpan(0, targetseq.getLength(), targetseq));
	    target_parent_sym.setProperty("method", annot_type);
	    target_parent_sym.setProperty("preferred_formats", pref_list);
	    ((MutableAnnotatedBioSeq)targetseq).addAnnotation(target_parent_sym);
	    target2sym.put(tname, target_parent_sym);
	  }
	  target_parent_sym.addChild(sym);
	}
      }
    }
    catch (EOFException ex) {
      reached_EOF = true;
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    finally {try { dis.close(); } catch (Exception ex) {}}

    long timecount = tim.read();
    if (REPORT_LOAD_STATS) {
      System.out.println("PSL binary file load time: " + timecount/1000f);
      if (! reached_EOF) {
        System.out.println("File loading was terminated early.");
      }
    }
    if (count <= 0) {
      System.out.println("PSL total counts <= 0 ???");
    }
    else {
      tim.start();
      UcscPslComparator comp = new UcscPslComparator();
      Collections.sort(results, comp);
      if (REPORT_LOAD_STATS) {
	System.out.println("PSL sort time: " + tim.read()/1000f);
	System.out.println("PSL alignment count = " + count);
	System.out.println("PSL total block count = " + total_block_count);
	System.out.println("PSL average blocks / alignment = " +
			   ((double)total_block_count/(double)count));
      }
    }
    return results;
  }


  public static java.util.List readPslFile(String file_name) {
    Timer tim = new Timer();
    tim.start();

    java.util.List results = null;
    try  {
      File fil = new File(file_name);
      double flength = fil.length();
      FileInputStream fis = new FileInputStream(fil);
      InputStream istr = null;
      if (use_byte_buffer) {
	byte[] bytebuf = new byte[(int)flength];
	BufferedInputStream bis = new BufferedInputStream(fis);
	bis.read(bytebuf);
	bis.close();
	ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
	istr = bytestream;
      }
      else {
	istr = fis;
      }
      PSLParser parser = new PSLParser();
      // don't bother annotating the sequences, just get the list of syms
      results = parser.parse(istr, file_name, null, null, false, false);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    long timecount = tim.read();
    System.out.println("finished reading PSL file, time to read = " + (tim.read()/1000f));
    return results;
  }

  public static void writeBinary(String file_name, java.util.List syms)  {
    try  {
      File outfile = new File(file_name);
      FileOutputStream fos = new FileOutputStream(outfile);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      DataOutputStream dos = new DataOutputStream(bos);
      int symcount = syms.size();
      for (int i=0; i<symcount; i++) {
	UcscPslSym psl = (UcscPslSym)syms.get(i);
	psl.outputBpsFormat(dos);
      }
      dos.close();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "binary PSL".
   **/
  public boolean writeAnnotations(java.util.Collection syms, BioSeq seq,
				  String type, OutputStream outstream) {
    //    System.out.println("in BpsParser.writeAnnotations()");
    boolean success = true;
    try {
      DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(outstream));
      Iterator iterator = syms.iterator();
      while (iterator.hasNext()) {
	SeqSymmetry sym = (SeqSymmetry)iterator.next();
	if (! (sym instanceof UcscPslSym)) {
	  int spancount = sym.getSpanCount();
	  if (sym.getSpanCount() == 1) {
	    sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq);
	  }
	  else {
	    BioSeq seq2 = SeqUtils.getOtherSeq(sym, seq);
	    sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq2, seq);
	  }
	}
	((UcscPslSym)sym).outputBpsFormat(dos);
      }
      dos.flush();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    }
    return success;
  }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "binary PSL".
   **/
  public String getMimeType() { return "binary/bps"; }
}
