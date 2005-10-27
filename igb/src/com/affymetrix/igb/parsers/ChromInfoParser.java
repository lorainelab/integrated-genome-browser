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
import com.affymetrix.igb.genometry.*;

public class ChromInfoParser {
  static final Pattern tab_regex = Pattern.compile("\t");
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static MutableAnnotatedBioSeq default_seq_template = new SmartAnnotBioSeq();

  MutableAnnotatedBioSeq template_seq = default_seq_template;

  /** Constructs a ChromInfoParser with a default template sequence. */
  public ChromInfoParser() { }

  public ChromInfoParser(MutableAnnotatedBioSeq template) {
    template_seq = template;
  }

  public Map parse(InputStream istr, String genome_version) throws IOException {
    AnnotatedSeqGroup grp = parseGroup(istr, genome_version);
    Map seqhash = grp.getSeqs();
    return seqhash;
  }

  public AnnotatedSeqGroup parseGroup(InputStream istr, String genome_version)
    throws IOException {

    AnnotatedSeqGroup seq_group = gmodel.addSeqGroup(genome_version);
    BufferedReader dis = new BufferedReader(new InputStreamReader(istr));
    String line;
    java.util.List seqlist = new ArrayList();
    while ((line = dis.readLine()) != null) {
      if (line.equals("") || line.startsWith("#"))  { continue; }
      String[] fields = tab_regex.split(line);
      if (fields.length <= 0) { continue; }
      String chrom_name = fields[0];

      int chrLength = Integer.parseInt(fields[1]);
      MutableAnnotatedBioSeq chrom = seq_group.getSeq(chrom_name);
      if (chrom == null) {  // if chrom already in seq group, then don't add to list
	try {
	  chrom = (MutableAnnotatedBioSeq)template_seq.getClass().newInstance();
	  chrom.setID(chrom_name);
	  chrom.setLength(chrLength);
	  if (chrom instanceof Versioned) {
	    ((Versioned)chrom).setVersion(genome_version);
	  }
	  seqlist.add(chrom);
	} catch (Exception ex) { ex.printStackTrace(); }
      }
    }
    Collections.sort(seqlist, new ChromComparator());
    for (int i=0; i<seqlist.size(); i++) {
      MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)seqlist.get(i);
      seq_group.addSeq(seq);
    }
    return seq_group;
  }

}
