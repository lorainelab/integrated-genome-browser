/**
*   Copyright (c) 2005 Affymetrix, Inc.
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
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.igb.genometry.*;

/**
 *
   *  Currently the type field is ignored (type info is not included in the output .bsnp file),
   *    because for all the snp files looked at so far from UCSC, the type for all entries has been "SNP"
   *
   *  Text SNP format (from UCSC sql tables, snpMap.sql):
   *
   *     bin smallint(5) unsigned NOT NULL default '0',
   *     chrom varchar(255) NOT NULL default '',
   *     chromStart int(10) unsigned NOT NULL default '0',
   *     chromEnd int(10) unsigned NOT NULL default '0',
   *     name varchar(255) NOT NULL default '',
   *     source enum('BAC_OVERLAP','MIXED','RANDOM','OTHER','Affy10K','Affy120K','unknown') NOT NULL default 'unknown',
   *     type enum('SNP','INDEL','SEGMENTAL','unknown') NOT NULL default 'unknown',
   *
   *  BSNP format:
   *
   *
   *   header string???
   *   genome organism / version / etc. ???
   *   seq_count  4-byte signed int
   *   source_count 4-byte signed int
   *   type_count 4-byte signed int
   *   [ id_constructor instructions ???]
   *   for each seq (seq_count)  {
   *      chromid    UTF-8 string
   *      for each (source)  {
   *         source_id  UTF-8 string
   *         for each (type)  {
   *            type_id  UTF-8 string
   *            snp_count  4-byte signed int
   *         }
   *      }
   *   }
   *   for each seq (seq_count)  {
   *      for each (source)  {
   *         for each (type)  {
   *            for each snp (snp_count)  {
   *               base_position 4-byte signed int
   *               numeric_id  4-byte signed int
   *            }
   *         }
   *      }
   *   }
   *
   *
   *  // first pass:
   *  genome_version
   *  number of seqs annotated
   *  for each seq  {
   *     seqid
   *     snp_count
   *  }
   *  for each seq  {
   *     for each snp (snp_count)  {
   *        base_position
   *     }
   *  }
   *
   */
  public class BsnpParser {
    static String text_infile = "c:/data/ucsc/hg17/snpMap_rs.txt";
    static String outfile = text_infile + ".bsnp";
    static String genome_version = "H_sapiens_May_2004";

    //    source enum('BAC_OVERLAP','MIXED','RANDOM','OTHER','Affy10K','Affy120K','unknown') NOT NULL default 'unknown',
    //  type enum('SNP','INDEL','SEGMENTAL','unknown') NOT NULL default 'unknown',

    //  static Pattern line_regex = Pattern.compile("\t");
    static Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace
    Map source_hash = new HashMap();
    Map type_hash = new HashMap();

    public void outputBsnpFormat(java.util.List parents, DataOutputStream dos) {
    try	{
    int pcount = parents.size();
    dos.writeUTF(genome_version);
    dos.writeInt(pcount);  // how many seqs there are
    for (int i=0; i<pcount; i++) {
      SeqSymmetry parent = (SeqSymmetry)parents.get(i);
      BioSeq seq = parent.getSpanSeq(0);
      String seqid = seq.getID();
      int snp_count = parent.getChildCount();
      dos.writeUTF(seqid);
      dos.writeInt(snp_count);
    }

    for (int i=0; i<pcount; i++) {
      SeqSymmetry parent = (SeqSymmetry)parents.get(i);
      int snp_count = parent.getChildCount();
      for (int k=0; k<snp_count; k++) {
	EfficientSnpSym snp = (EfficientSnpSym)parent.getChild(k);
	int base_coord = snp.getSpan(0).getMin();
	dos.writeInt(base_coord);
      }
    }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public java.util.List readTextFormat(BufferedReader br) {
    int snp_count = 0;
    int weird_length_count = 0;
    Map id2psym = new HashMap();
    ArrayList parent_syms = new ArrayList();
    try {
      String line;
      while ((line = br.readLine()) != null) {
	String[] fields = line_regex.split(line);
	String seqid = fields[1].intern();
	MutableSeqSymmetry psym = (MutableSeqSymmetry)id2psym.get(seqid);
	if (psym == null) {
	  psym = new SimpleSymWithProps();
	  MutableAnnotatedBioSeq seq = new SimpleAnnotatedBioSeq(seqid, 1000000000);
	  psym.addSpan(new SimpleSeqSpan(0, 1000000000, seq));
	  id2psym.put(seqid, psym);
	  parent_syms.add(psym);
	}
	int min = Integer.parseInt(fields[2]);
	int max = Integer.parseInt(fields[3]);
	int length = (max - min);
	if (length != 1) {
	  System.out.println("length != 1: " + line);
	  weird_length_count++;
	}
	String snpid = fields[4];
	String snp_source = fields[5].intern();
	String snp_type = fields[6].intern();
	//	EfficientSnpSym snp_sym = new EfficientSnpSym(psym, min, snpid);
	EfficientSnpSym snp_sym = new EfficientSnpSym(psym, min);
	psym.addChild(snp_sym);
	snp_count++;
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    System.out.println("snp count: " + snp_count);
    System.out.println("weird length count: " + weird_length_count);
    return parent_syms;
  }

  public List parse(InputStream istr, String annot_type, Map seq_hash, boolean annot_seq) {
    System.out.println("parsing bsnp file");
    java.util.List snp_syms = null;
    try {
    BufferedInputStream bis;
    if (istr instanceof BufferedInputStream) { bis = (BufferedInputStream)istr; }
    else { bis = new BufferedInputStream(istr); }
    DataInputStream dis = new DataInputStream(bis);
    String genome_version = dis.readUTF();
    int seq_count = dis.readInt();
    int[] snp_counts = new int[seq_count];
    String[] seqids = new String[seq_count];
    MutableAnnotatedBioSeq[] seqs = new MutableAnnotatedBioSeq[seq_count];
    System.out.println("genome version: " + genome_version);
    System.out.println("seqs: " + seq_count);
    int total_snp_count = 0;
    for (int i=0; i<seq_count; i++) {
      String seqid = dis.readUTF();
      seqids[i] = seqid;
      MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)seq_hash.get(seqid);
      seqs[i] = aseq;   // will be null if no seq with given seqid in seqhash,
      snp_counts[i] = dis.readInt();
      total_snp_count += snp_counts[i];
    }
    System.out.println("total snps: " + total_snp_count);
    snp_syms = new ArrayList(total_snp_count);
    for (int i=0; i<seq_count; i++) {
      MutableAnnotatedBioSeq aseq = seqs[i];
      /*
      if (aseq == null) {
	System.out.println("No seq matching seqid: " + seqids[i] + " found, aborting BsnpParser parsing!");
	break;
      }
      */
      int snp_count = snp_counts[i];
      System.out.println("seqid: " + seqids[i] + ", snps: " + snp_counts[i]);
      SimpleSymWithProps psym = new SimpleSymWithProps();
      psym.setProperty("type", annot_type);
      psym.addSpan(new SimpleSeqSpan(0, 1000000000, aseq));
      if (annot_seq && (aseq != null))  {
	aseq.addAnnotation(psym);
      }
      for (int k=0; k<snp_count; k++) {
	int base_coord = dis.readInt();
	EfficientSnpSym snp = new EfficientSnpSym(psym, base_coord);
	psym.addChild(snp);
	snp_syms.add(snp);
      }
    }
    }
    catch (Exception ex) { ex.printStackTrace(); }
    return snp_syms;
  }

  static boolean TEST_BINARY_PARSE = true;
  public static void main(String[] args) {
    try {
      if (TEST_BINARY_PARSE) {
	System.out.println("parsing in snp data from .bsnp file: " + outfile);
	BsnpParser tester = new BsnpParser();
	File ifil = new File(outfile);
	InputStream istr = new FileInputStream(ifil);
	tester.parse(istr, "snp", new HashMap(), true);
	System.out.println("finished parsing in snp data from .bsnp file");
      }
      else {
	BsnpParser tester = new BsnpParser();
	File ifil = new File(text_infile);
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ifil)));
	System.out.println("reading in text data from: " + text_infile);
	java.util.List parent_syms = tester.readTextFormat(br);
	File ofil = new File(outfile);
	DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(ofil)));
	System.out.println("outputing binary data to: " + outfile);
	tester.outputBsnpFormat(parent_syms, dos);
	dos.close();
	System.out.println("finished converting text data to binary .bsnp format");
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  // Annotationwriter implementation
  //  public boolean writeAnnotations(Collection syms, BioSeq seq,
  //				  String type, OutputStream outstream) {
  //  }
  // public String getMimeType()  { return "binary/bsnp"; }

}
