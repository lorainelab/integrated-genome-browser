/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SmartAnnotBioSeq;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.genometry.EfficientProbesetSymA;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.genometry.span.SimpleSeqSpan;

/**
 *
 *  A highly optimized binary format for probesets that meet certain criteria, originally intended for
 *      use with all-exon arrays.
 *<pre>
 *  Preserving just the probe locations, the grouping of probes into probesets, and
 *  the probeset ids, and making some assumptions that I'm pretty sure the exon probes meet:
 *    a) all probes are same length
 *    b) all probes align to a contiguous genome interval (no split probes)
 *    c) probeset ids can be represented numerically
 *    d) all probes within a probeset are on same strand
 *
 *  This "microformat" averages out to about 5.3 bytes/probe
 *    (> 10x compression relative to the already stripped down gff).
 *  At four million probes that's about 21 MB for all of them.
 *
 *  -------------------------
 *  Format
 *  Header:
 *     Format (UTF-8)  "bp1"  OR "bp2"
 *     Format version (int)
 *     Genome name (UTF-8)  [ need to deal with case where name and version are combined into one string?]
 *     Genome version (UTF-8)
 *     Annotation type (UTF-8)  -- need way of deciding whether to use this or extract from file name...
 *     Probe length (int)
 *     [if format="bp2", then here is UTF-8 of prefix for ids in files -- combined with probeset id int, get full id ]
 *     Number of seqs (int)
 *     for each seq
 *        seq name (UTF-8)
 *        seq length (int)
 *        number of probesets for seq
 *     Number of tag-val properties (int)
 *     for each tag-val
 *        tag (UTF-8)
 *        value (UTF-8)
 *     for each seq
 *         for each probeset
 *             id (int)
 *             number of probes & strand (byte, 0 to 127 probes, sign indicates strand)
 *             for each probe
 *                 min genome position (int, zero interbase)
 *</pre>
 */
public class Bprobe1Parser implements AnnotationWriter {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static boolean DEBUG = true;
  static java.util.List pref_list = new ArrayList();
  static {
    pref_list.add("bp2");
  }

  public List parse(InputStream istr, AnnotatedSeqGroup group,
    boolean annotate_seq, String default_type) throws IOException {

    BufferedInputStream bis;
    Map tagvals = new LinkedHashMap();
    Map seq2syms = new LinkedHashMap();
    Map seq2lengths = new LinkedHashMap();
    DataInputStream dis = null;
    String id_prefix = "";
    List results = new ArrayList();
    try  {
      if (istr instanceof BufferedInputStream) {
        bis = (BufferedInputStream) istr;
      }
      else {
        bis = new BufferedInputStream(istr);
      }
      dis = new DataInputStream(bis);
      String format = dis.readUTF();
      int format_version = dis.readInt();
      boolean version2 = (format.equals("bp2"));
      System.out.println("is bp2: " + version2);
      String seq_group_name = dis.readUTF(); // genome name
      String seq_group_version = dis.readUTF(); // genome version
      // combining genome and version to get seq group id
      String seq_group_id = seq_group_name + seq_group_version;
      if (seq_group_id == null) {
	System.err.println("bprobe1 file does not specify a genome name or version, these are required!");
	return null;
      }
      if (! group.isSynonymous(seq_group_id)) {
	System.err.println("In Bprobe1Parser, mismatch between AnnotatedSeqGroup argument: " + group.getID() +
			   " and group name+version in bprobe1 file: " + seq_group_id);
	return null;
      }
      String specified_type = dis.readUTF();
      String annot_type;
      if ( (specified_type == null) || (specified_type.length() <= 0)) {
        annot_type = default_type;
      }
      else {
        annot_type = specified_type;
      }
      int probe_length = dis.readInt();
      if (version2) {
	id_prefix = dis.readUTF();
      }
      int seq_count = dis.readInt();
      if (DEBUG) {
	System.out.println("format: " + format + ", format_version: " + format_version);
	System.out.println("seq_group_name: " + seq_group_name + ", seq_group_version: " + seq_group_version);
	System.out.println("type: " + specified_type);
	System.out.println("probe_length: " + probe_length);
	System.out.println("id_prefix: " + id_prefix);
	System.out.println("seq_count: " + seq_count);
      }

      for (int i = 0; i < seq_count; i++) {
        String seqid = dis.readUTF();
        int seq_length = dis.readInt();
        int probeset_count = dis.readInt();
        SeqSymmetry[] syms = new SeqSymmetry[probeset_count];
        seq2syms.put(seqid, syms);
	seq2lengths.put(seqid, new Integer(seq_length));
      }
      int tagval_count = dis.readInt();
      for (int i = 0; i < tagval_count; i++) {
        String tag = dis.readUTF();
        String val = dis.readUTF();
        tagvals.put(tag, val);
      }
      Iterator seqiter = seq2syms.keySet().iterator();
      while (seqiter.hasNext()) {
        String seqid = (String) seqiter.next();
        SeqSymmetry[] syms = (SeqSymmetry[]) seq2syms.get(seqid);
        int probeset_count = syms.length;
	System.out.println("seq: " + seqid + ", probeset count: " + probeset_count);

	MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)group.getSeq(seqid);
        if (aseq == null) {
	  int seqlength = ((Integer)seq2lengths.get(seqid)).intValue();
	  aseq = group.addSeq(seqid, seqlength);
	}
	SimpleSymWithProps container_sym = new SimpleSymWithProps(probeset_count);
	container_sym.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq) );
	container_sym.setProperty("method", annot_type);
	container_sym.setProperty("preferred_formats", pref_list);

        for (int i = 0; i < probeset_count; i++) {
          int nid = dis.readInt();
          int b = (int) dis.readByte();
          int probe_count = Math.abs(b);
          boolean forward = (b >= 0);
          if (probe_count == 0) {
            // EfficientProbesetSymA does not allow probe sets with 0 probes
            throw new IOException("Probe_count is zero for '"+ nid+ "'");
          }
	  int[] cmins = new int[probe_count];
          for (int k = 0; k < probe_count; k++) {
            int min = dis.readInt();
	    cmins[k] = min;
          }
	  syms[i] = new EfficientProbesetSymA(cmins, probe_length, forward, id_prefix, nid, aseq);
	  container_sym.addChild(syms[i]);
	  results.add(syms[i]);
        }
	if (annotate_seq) {
	  aseq.addAnnotation(container_sym);
	}
      }
      System.out.println("finished parsing probeset file");
    }

    finally {
      if (dis != null) try { dis.close(); } catch (Exception e) {}
    }
    return results;
  }

  /**
   *  Assumptions:
   *      seq is SmartAnnotBioSeq
   *      all syms in collection are EfficientProbesetSymA
   *      all syms share same probe_length, id_prefix
   */
  public boolean writeAnnotations(java.util.Collection syms, BioSeq aseq,
				  String type, OutputStream outstream) {
    boolean success = false;
    AnnotatedSeqGroup group = ((SmartAnnotBioSeq)aseq).getSeqGroup();
    String groupid = group.getID();
    //    String version = groupid;
    String seqid = aseq.getID();
    int acount = syms.size();

    int probe_length = 0;
    String id_prefix = "";

    if (syms.size() > 0) {
      // use first sym to get shared probe_length & id_prefix
      EfficientProbesetSymA fsym = (EfficientProbesetSymA)syms.iterator().next();
      probe_length = fsym.getProbeLength();
      id_prefix = fsym.getPrefixID();
    }

    DataOutputStream dos = null;
    try {
      if (outstream instanceof DataOutputStream) { dos = (DataOutputStream)outstream; }
      else if (outstream instanceof BufferedOutputStream) { dos = new DataOutputStream(outstream); }
      else { dos = new DataOutputStream(new BufferedOutputStream(outstream)); }
      dos.writeUTF("bp2");
      dos.writeInt(1);
      dos.writeUTF(groupid);
      //      dos.writeUTF(version_id);
      dos.writeUTF("");  // version id blank -- version and group are combined in groupid
      dos.writeUTF(type);
      dos.writeInt(probe_length);
      dos.writeUTF(id_prefix);

      dos.writeInt(1);  // only one seq written out by writeAnnotations() call
      System.out.println("seqid: " + seqid + ", annot count: " + acount );
      dos.writeUTF(seqid);
      dos.writeInt(aseq.getLength());
      dos.writeInt(syms.size());

      dos.writeInt(0);  // no tagval properties...
      Iterator siter = syms.iterator();
      while (siter.hasNext())  {
	EfficientProbesetSymA psym  = (EfficientProbesetSymA)siter.next();
	writeProbeset(psym, aseq, dos);
      }
      dos.flush();
      success = true;
    }
    catch (Exception ex) { ex.printStackTrace(); }
    return success;
  };

  /**
   *  Converts a "GFF" file into a "bp1" file.
   *  Assumes
   *     All annotations in GFF file are genome-based probes (contiguous intervals on genome);
   *     25-mer probes (for now)
   */
  public static void convertGff(String gff_file, String output_file, String genome_id,
				String version_id, String annot_type, String id_prefix)
    throws IOException {
    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup(genome_id);
    int probe_length = 25;
    Map tagvals = new HashMap();
    tagvals.put("tagval_test_1", "testing1");
    tagvals.put("tagval_test_2", "testing2");

    List annots = null;
    try {
      System.out.println("parsing gff file: " + gff_file);
      GFFParser gff_parser = new GFFParser();
      BufferedInputStream bis = new BufferedInputStream( new FileInputStream( new File( gff_file) ) );
      annots = gff_parser.parse(bis, seq_group, false);
      bis.close();
    }
    catch (Exception ex) { ex.printStackTrace(); }

    BufferedOutputStream bos = null;
    DataOutputStream dos = null;
    try {
      int total_annot_count = annots.size();
      int seq_count = seq_group.getSeqCount();
      System.out.println("done parsing, seq count = " + seq_count + ", total annot count = " + total_annot_count);
      bos = new BufferedOutputStream( new FileOutputStream( new File( output_file) ) );
      dos = new DataOutputStream(bos);
      dos.writeUTF("bp2");
      dos.writeInt(1);
      dos.writeUTF(seq_group.getID());
      dos.writeUTF(version_id);
      dos.writeUTF(annot_type);
      dos.writeInt(probe_length);
      dos.writeUTF(id_prefix);
      dos.writeInt(seq_count);
      Iterator iter = seq_group.getSeqList().iterator();
      while (iter.hasNext()) {
	AnnotatedBioSeq aseq = (AnnotatedBioSeq)iter.next();
	String seqid = aseq.getID();
	int seq_length = aseq.getLength();
	int container_count = aseq.getAnnotationCount();
	int annot_count = 0;
	for (int i=0; i<container_count; i++) {
	  SeqSymmetry cont = aseq.getAnnotation(i);
	  annot_count += cont.getChildCount();
	}
	System.out.println("seqid: " + seqid + ", annot count: " + annot_count );
	dos.writeUTF(seqid);
	dos.writeInt(seq_length);
	dos.writeInt(annot_count);
      }
      int tagval_count = tagvals.size();
      dos.writeInt(tagval_count);
      Iterator tviter = tagvals.entrySet().iterator();
      while (tviter.hasNext()) {
	Map.Entry ent = (Map.Entry)tviter.next();
	String tag = (String)ent.getKey();
	String val = (String)ent.getValue();
	dos.writeUTF(tag);
	dos.writeUTF(val);
      }
      iter = seq_group.getSeqList().iterator();
      while (iter.hasNext()) {
	AnnotatedBioSeq aseq = (AnnotatedBioSeq)iter.next();
	int container_count = aseq.getAnnotationCount();
	//	System.out.println("seqid: " + aseq.getID() + ", annot count: " + annot_count );
	for (int i=0; i<container_count; i++) {
	  SeqSymmetry cont = aseq.getAnnotation(i);
	  int annot_count = cont.getChildCount();
	  for (int k=0; k<annot_count; k++) {
	    EfficientProbesetSymA psym = (EfficientProbesetSymA)cont.getChild(k);
	    writeProbeset(psym, aseq, dos);
	  }
	  /*
	  for (int k=0; k<annot_count; k++) {
	    SeqSymmetry psym = cont.getChild(k);
	    SeqSpan pspan = psym.getSpan(aseq);
	    int child_count = psym.getChildCount();
	    String symid = psym.getID();
	    //          System.out.println("probeset_id: " + symid);
	    int symint = Integer.parseInt(symid);
	    //	    int symint = psym.getIntID();
	    dos.writeInt(symint);  // probeset id representated as an integer
	    // sign of strnad_and_count indicates forward (+) or reverse (-) strand
	    byte strand_and_count = (byte)(pspan.isForward() ? child_count : -child_count);
	    dos.writeByte(strand_and_count);
	    for (int m=0; m<child_count; m++) {
	      SeqSymmetry csym = psym.getChild(m);
	      dos.writeInt(csym.getSpan(aseq).getMin());
	    }
          }
	  */
	}
      }
    }
    finally {
      if (bos != null) try { bos.close(); } catch (Exception e) {}
      if (dos != null) try { dos.close(); } catch (Exception e) {}
    }
  }


  protected static void writeProbeset(EfficientProbesetSymA psym, BioSeq aseq, DataOutputStream dos) throws IOException {
    SeqSpan pspan = psym.getSpan(aseq);
    int child_count = psym.getChildCount();
    int intid = psym.getIntID();
    dos.writeInt(intid);  // probeset id representated as an integer
    // sign of strnad_and_count indicates forward (+) or reverse (-) strand
    byte strand_and_count = (byte)(pspan.isForward() ? child_count : -child_count);
    dos.writeByte(strand_and_count);
    for (int m=0; m<child_count; m++) {
      SeqSymmetry csym = psym.getChild(m);
      dos.writeInt(csym.getSpan(aseq).getMin());
    }
  }

  public String getMimeType() { return "binary/bp2"; }


  /**
   *  Reads a GFF file and writes a "bp1" (binary bprobe1 format) file.
   *<pre>
   *  The input gff file of genome-based probesets must meet these criteria:
   *    a) all probes are same length
   *    b) all probes align to a contiguous genome interval (no split probes)
   *    c) each probeset id can be represented with unique integer root within the set
   *          and a String prefix shared among all probesets in the file
   *    d) all probes within a probeset are on same strand
   *    e) less than 128 probes per probeset
   *          (but can be different number of probes in each probeset)
   *
   *  writes as output a file in bprobe1 format
   *  first arg is gff input file name
   *  second arg is bprobe output file name
   *  third arg is genomeid
   *  fourth arg is annot type name
   *  fifth arg is optional, and is genome versionid
   *  if no second arg, output is written to standard out??
   *</pre>
   */
  public static void main(String[] args) throws IOException {
    String in_file = "";
    String out_file = "";
    String id_prefix = "";
    String genomeid= "";
    String versionid = "";
    String annot_type = "";

    if (args.length == 5 || args.length == 6) {
      in_file = args[0];
      out_file = args[1];
      id_prefix = args[2];
      annot_type = args[3];
      genomeid = args[4];
      if (args.length == 6) { versionid = args[5]; }
    } else {
      System.out.println("Usage:  java ... Bprobe1Parser <GFF infile> <BP1 outfile> <id_prefix> <annot type> <genomeid> [<version>]");
      System.out.println("Example:  java ... Bprobe1Parser foo.gff foo.bp1 HuEx HuEx-1_0-st-Probes H_sapiens_Jul_2003");
      System.exit(1);
    }


    System.out.println("Creating a '.bp1' format file: ");
    System.out.println("Input '"+in_file+"'");
    System.out.println("Output '"+out_file+"'");
    convertGff(in_file, out_file, genomeid, versionid, annot_type, id_prefix);
    System.out.println("DONE!  Finished converting GFF file to BP1 file.");
    System.out.println("");

    /*
    // After creating the file, parses it (for testing)
    try  {
      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(out_file)));
      AnnotatedSeqGroup group = gmodel.addSeqGroup(genomeid + versionid);
      Bprobe1Parser parser = new Bprobe1Parser();
      parser.parse(bis, group, true, annot_type);
    }
    catch (Exception ex)  {
      ex.printStackTrace();
    }
    */

  }

  /*
  public void makeBprobe1(String gff_input_file, String bprobe_output_file) {

  }
  */

}
