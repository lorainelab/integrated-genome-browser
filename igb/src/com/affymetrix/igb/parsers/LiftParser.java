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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.genoviz.util.Timer;

public class LiftParser {
  static Pattern re_tab = Pattern.compile("\t");
  static Pattern re_name = Pattern.compile("/");
  static int CHROM_START = 0;
  static int COMBO_NAME = 1;
  static int MATCH_LENGTH = 2;
  static int CHROM_NAME = 3;
  static int CHROM_LENGTH = 4;
  static int CONTIG_NAME_SUBFIELD = 1;

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static MutableAnnotatedBioSeq default_seq_template = new SmartAnnotBioSeq();
  MutableAnnotatedBioSeq template_seq = default_seq_template;

  public LiftParser()  {  }

  public LiftParser(MutableAnnotatedBioSeq template) {
    template_seq = template;
  }

  public Map loadChroms(String file_name, String genome_version) {
    System.out.println("trying to load lift file: " + file_name);
    try {
      File fil = new File(file_name);
      FileInputStream fistr = new FileInputStream(fil);
      Map liftmap = this.parse(fistr, genome_version);
      return liftmap;
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }


  /**
   *  Reads lift-format from the input stream.
   *  @return  A Map with chromosome ids as keys, and CompositeBioSeqs representing
   *     chromosomes in the lift file as values.
   */
  public Map parse(InputStream istr, String genome_version) throws IOException {
    return parse(istr, genome_version, true);
  }


  private Map parse(InputStream istr, String genome_version, boolean annotate_seq) throws IOException {
    AnnotatedSeqGroup grp = parseGroup(istr, genome_version, annotate_seq);
    Map seqhash = grp.getSeqs();
    return seqhash;
  }

  /**
   *  Reads lift-format from the input stream.
   *  @return an AnnotatedSeqGroup containing CompositeBioSeqs representing
   *     chromosomes in the lift file.
   */
  public AnnotatedSeqGroup parseGroup(InputStream istr, String genome_version, boolean annotate_seq)
    throws IOException {
    System.out.println("parsing in lift file");
    Timer tim = new Timer();
    tim.start();
    int contig_count = 0;
    int chrom_count = 0;
    AnnotatedSeqGroup seq_group = gmodel.addSeqGroup(genome_version);

    BufferedReader br = new BufferedReader(new InputStreamReader(istr));
    java.util.List seqlist = new ArrayList();
    Map temp_seqhash = new HashMap();

    try {
      String line;
      while ((line = br.readLine()) != null)  {
	String fields[] = re_tab.split(line);
	int chrom_start = Integer.parseInt(fields[CHROM_START]);
	int match_length = Integer.parseInt(fields[MATCH_LENGTH]);
	String chrom_name = fields[CHROM_NAME];
	int chrom_length = Integer.parseInt(fields[CHROM_LENGTH]);

	String tempname = fields[COMBO_NAME];
	String splitname[] = re_name.split(tempname);
	String contig_name = splitname[CONTIG_NAME_SUBFIELD];
	// experimenting with constructing virtual sequences by using chromosomes as contigs
	MutableAnnotatedBioSeq contig = seq_group.getSeq(contig_name);
	if (contig == null)  { contig = (MutableAnnotatedBioSeq)temp_seqhash.get(contig_name); }
	if (contig == null)  { contig = new SimpleAnnotatedBioSeq(contig_name, match_length); }

	contig_count++;
	MutableAnnotatedBioSeq chrom = seq_group.getSeq(chrom_name);
	if (chrom == null) { chrom = (MutableAnnotatedBioSeq)temp_seqhash.get(chrom_name); }
	if (chrom == null) {
	  chrom_count++;
	  try  {
	    chrom = (MutableAnnotatedBioSeq)template_seq.getClass().newInstance();
	  } catch (Exception ex) { ex.printStackTrace(); }
	  chrom.setID(chrom_name);
	  chrom.setLength(chrom_length);
	  if (chrom instanceof Versioned) {
	    ((Versioned)chrom).setVersion(genome_version);
	  }
	  /*
	  SymWithProps comp = new SimpleSymWithProps();
	  comp.setProperty("method", "contigs");
	  if (chrom instanceof CompositeBioSeq) {
	    ((CompositeBioSeq)chrom).setComposition(comp);
	  }
	  if (annotate_seq)  { chrom.addAnnotation(comp); }
	  */
	  seqlist.add(chrom);  // adding to seqlist for sorting
	  temp_seqhash.put(chrom_name, chrom);
	}
	if (chrom instanceof CompositeBioSeq) {
	  MutableSeqSymmetry comp = (MutableSeqSymmetry)(((CompositeBioSeq)chrom).getComposition());
          if (comp == null)  {
	    comp = new SimpleSymWithProps();
	    ((SimpleSymWithProps)comp).setProperty("method", "contigs");
            ((CompositeBioSeq)chrom).setComposition(comp);
	    if (annotate_seq)  {
	      chrom.addAnnotation(comp);
	    }
          }
	  SimpleSymWithProps csym = new SimpleSymWithProps();
	  csym.addSpan(new SimpleSeqSpan(chrom_start, (chrom_start + match_length), chrom));
	  csym.addSpan(new SimpleSeqSpan(0, match_length, contig));
	  csym.setProperty("method", "contig");
	  csym.setProperty("id", contig.getID());
	  comp.addChild(csym);
	}
      }
      Collections.sort(seqlist, new ChromComparator()); // sorting seqlist before adding seqs to seq group
      for (int i=0; i<seqlist.size(); i++) {
	MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)seqlist.get(i);
	seq_group.addSeq(seq);
      }
    }
    catch (EOFException ex) {
      System.out.println("reached end of lift file");
    }

    Collection chroms = seq_group.getSeqs().values();
    Iterator iter = chroms.iterator();
    while (iter.hasNext()) {
      CompositeBioSeq chrom = (CompositeBioSeq)iter.next();
      MutableSeqSymmetry comp = (MutableSeqSymmetry)chrom.getComposition();
      if (comp != null) {
	SeqSpan chromspan = SeqUtils.getChildBounds(comp, chrom);
	comp.addSpan(chromspan);
      }
    }
    System.out.println("chroms loaded, load time = " + tim.read()/1000f);
    System.out.println("contig count: " + contig_count);
    System.out.println("chrom count: " + chrom_count);
    return seq_group;
  }

}
