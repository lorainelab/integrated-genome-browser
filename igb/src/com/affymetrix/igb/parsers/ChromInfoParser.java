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

    AnnotatedSeqGroup group = gmodel.addSeqGroup(genome_version);
    BufferedReader dis = new BufferedReader(new InputStreamReader(istr));
    String line;
    java.util.List seqlist = new ArrayList();
    while ((line = dis.readLine()) != null) {
      String[] fields = tab_regex.split(line);
      if (fields.length <= 0) { continue; }
      String chrName = fields[0];
      int chrLength = Integer.parseInt(fields[1]);
      MutableAnnotatedBioSeq chrSeq = null;
      try {
        chrSeq = (MutableAnnotatedBioSeq)template_seq.getClass().newInstance();
      } catch (Exception ex) { ex.printStackTrace(); }
      chrSeq.setID(chrName);
      chrSeq.setLength(chrLength);
      if (chrSeq instanceof Versioned) {
        ((Versioned)chrSeq).setVersion(genome_version);
      }
      //      group.addSeq(chrSeq);
      seqlist.add(chrSeq);
    }
    Collections.sort(seqlist, new ChromComparator());
    for (int i=0; i<seqlist.size(); i++) {
      MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)seqlist.get(i);
      group.addSeq(seq);
    }
    return group;
  }

}
