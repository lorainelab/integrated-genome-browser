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
 *  Text repeat format (from UCSC sql tables, chr*_rmsk.sql):
 *
 *  bin smallint(5) unsigned NOT NULL default '0',
 *  swScore int(10) unsigned NOT NULL default '0',
 *  milliDiv int(10) unsigned NOT NULL default '0',
 *  milliDel int(10) unsigned NOT NULL default '0',
 *  milliIns int(10) unsigned NOT NULL default '0',
 *  genoName varchar(255) NOT NULL default '',
 *  genoStart int(10) unsigned NOT NULL default '0',
 *  genoEnd int(10) unsigned NOT NULL default '0',
 *  genoLeft int(11) NOT NULL default '0',
 *  strand char(1) NOT NULL default '',
 *  repName varchar(255) NOT NULL default '',
 *  repClass varchar(255) NOT NULL default '',
 *  repFamily varchar(255) NOT NULL default '',
 *  repStart int(11) NOT NULL default '0',
 *  repEnd int(11) NOT NULL default '0',
 *  repLeft int(11) NOT NULL default '0',
 *  id char(1) NOT NULL default '',
 *
 *
 *  Frist pass at binary representation: 
 *  genome_version
 *  number of seqs annotated
 *  for each seq  {
 *     seqid
 *     repeat_count
 *  }
 *  for each seq  {
 *     for each repeat (repeat_count)  {
 *        base_start
 *        base_end   [ and if base_start > base_end then on negative strand ]
 *     }
 *  }
 *
 */
  public class BrptParser {
    static String text_infile = "c:/data/ucsc/hg17/repeats/rmsk_all.txt";
    static String outfile = text_infile + ".brpt";
    static String genome_version = "H_sapiens_May_2004";


    //  static Pattern line_regex = Pattern.compile("\t");
    static Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace
    Map source_hash = new HashMap();
    Map type_hash = new HashMap();

    public void outputBrptFormat(java.util.List parents, DataOutputStream dos) {
    try	{
      int pcount = parents.size();
      dos.writeUTF(genome_version);
      dos.writeInt(pcount);  // how many seqs there are
      for (int i=0; i<pcount; i++) {
	SeqSymmetry parent = (SeqSymmetry)parents.get(i);
	BioSeq seq = parent.getSpanSeq(0);
	String seqid = seq.getID();
	int rpt_count = parent.getChildCount();
	dos.writeUTF(seqid);
	dos.writeInt(rpt_count);
      }

      for (int i=0; i<pcount; i++) {
	SeqSymmetry parent = (SeqSymmetry)parents.get(i);
	int rpt_count = parent.getChildCount();
	for (int k=0; k<rpt_count; k++) {
	  LeafSingletonSymmetry rpt = (LeafSingletonSymmetry)parent.getChild(k);
	  SeqSpan span = rpt.getSpan(0);
	  int start = span.getStart();
	  int end = span.getEnd();
	  dos.writeInt(start);
	  dos.writeInt(end);
	}
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public java.util.List readTextFormat(BufferedReader br) {
    int weird_length_count = 0;
    Map id2psym = new HashMap();
    ArrayList parent_syms = new ArrayList();
    int repeat_count = 0;
    int pos_count = 0;
    int neg_count = 0;

    try {
      String line;
      while ((line = br.readLine()) != null) {
	String[] fields = line_regex.split(line);
	String seqid = fields[5].intern();
	MutableAnnotatedBioSeq seq = null;
	MutableSeqSymmetry psym = (MutableSeqSymmetry)id2psym.get(seqid);
	if (psym == null) {
	  psym = new SimpleSymWithProps();
	  seq = new SimpleAnnotatedBioSeq(seqid, 1000000000);
	  psym.addSpan(new SimpleSeqSpan(0, 1000000000, seq));
	  id2psym.put(seqid, psym);
	  parent_syms.add(psym);
	}
	else {
	  seq = (MutableAnnotatedBioSeq)psym.getSpanSeq(0);
	}
	int min = Integer.parseInt(fields[6]);
	int max = Integer.parseInt(fields[7]);
	int start;
	int end;
	String strand = fields[9];
	if (strand.equals("-")) {  // on negative strand
	  start = max;
	  end = min;
	  neg_count++;
	}
	else {  // else on positive strand
	  start = min;
	  end = max;
	  pos_count++;
	}
	LeafSingletonSymmetry rpt_sym = new LeafSingletonSymmetry(start, end, seq);
	psym.addChild(rpt_sym);
	repeat_count++;
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    System.out.println("repeat count: " + repeat_count);
    System.out.println("repeats on + strand: " + pos_count);
    System.out.println("repeats on - strand: " + neg_count);
    return parent_syms;
  }

  public List parse(InputStream istr, String annot_type, Map seq_hash, boolean annot_seq) {
    System.out.println("parsing brpt file");
    java.util.List rpt_syms = null;
    try {
    BufferedInputStream bis;
    if (istr instanceof BufferedInputStream) { bis = (BufferedInputStream)istr; }
    else { bis = new BufferedInputStream(istr); }
    DataInputStream dis = new DataInputStream(bis);
    String genome_version = dis.readUTF();
    int seq_count = dis.readInt();
    int[] rpt_counts = new int[seq_count];
    String[] seqids = new String[seq_count];
    MutableAnnotatedBioSeq[] seqs = new MutableAnnotatedBioSeq[seq_count];
    System.out.println("genome version: " + genome_version);
    System.out.println("seqs: " + seq_count);
    int total_rpt_count = 0;
    for (int i=0; i<seq_count; i++) {
      String seqid = dis.readUTF();
      seqids[i] = seqid;
      MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)seq_hash.get(seqid);
      seqs[i] = aseq;   // will be null if no seq with given seqid in seqhash,
      rpt_counts[i] = dis.readInt();
      total_rpt_count += rpt_counts[i];
    }
    System.out.println("total rpts: " + total_rpt_count);
    rpt_syms = new ArrayList(total_rpt_count);
    for (int i=0; i<seq_count; i++) {
      MutableAnnotatedBioSeq aseq = seqs[i];
      /*
      if (aseq == null) {
	System.out.println("No seq matching seqid: " + seqids[i] + " found, aborting BrptParser parsing!");
	break;
      }
      */
      int rpt_count = rpt_counts[i];
      System.out.println("seqid: " + seqids[i] + ", rpts: " + rpt_counts[i]);
      SimpleSymWithProps psym = new SimpleSymWithProps();
      psym.setProperty("type", annot_type);
      psym.addSpan(new SimpleSeqSpan(0, 1000000000, aseq));
      if (annot_seq && (aseq != null))  {
	aseq.addAnnotation(psym);
      }
      for (int k=0; k<rpt_count; k++) {
	int start = dis.readInt();
        int end = dis.readInt();
	LeafSingletonSymmetry rpt = new LeafSingletonSymmetry(start, end, aseq);
	psym.addChild(rpt);
	rpt_syms.add(rpt);
      }
    }
    }
    catch (Exception ex) { ex.printStackTrace(); }
    return rpt_syms;
  }

  static boolean TEST_BINARY_PARSE = false;
  public static void main(String[] args) {
    try {
      if (TEST_BINARY_PARSE) {
	System.out.println("parsing in rpt data from .brpt file: " + outfile);
	BrptParser tester = new BrptParser();
	File ifil = new File(outfile);
	InputStream istr = new FileInputStream(ifil);
	tester.parse(istr, "rpt", new HashMap(), true);
	System.out.println("finished parsing in rpt data from .brpt file");
      }
      else {
	BrptParser tester = new BrptParser();
	File ifil = new File(text_infile);
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ifil)));
	System.out.println("reading in text data from: " + text_infile);
	java.util.List parent_syms = tester.readTextFormat(br);
	File ofil = new File(outfile);
	DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(ofil)));
	System.out.println("outputing binary data to: " + outfile);
	tester.outputBrptFormat(parent_syms, dos);
	dos.close();
	System.out.println("finished converting text data to binary .brpt format");
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
  // public String getMimeType()  { return "binary/brpt"; }

}
