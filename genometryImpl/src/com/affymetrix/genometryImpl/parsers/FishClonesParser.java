/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.io.*;

/**
 *  Parses tab-delimited output "fishClones.txt" files from UCSC.
 *  The file extension should be {@link #FILE_EXT}.
 */
public class FishClonesParser extends TabDelimitedParser {
  public static final String FILE_EXT = "fsh";
  public static final String FISH_CLONES_METHOD = "fishClones";
  
  public FishClonesParser(boolean addToIndex) {
    super(-1, 0, 1, 2, -1, -1, -1, 3, false, false, addToIndex);
  }
  
  
  public static void main(String[] args) {
    String filname = System.getProperty("user.dir") + "/CVS Repositories/affy/GenotypeConsoleBrowser/data/QuickLoad/hg18/fishClones.fsh";
    File file = new File(filname);
    
    FishClonesParser tester = new FishClonesParser(true);
    try {
      FileInputStream fis = new FileInputStream(file);
      AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
      
      tester.parse(fis, file.getName(), seq_group);
      
      for (int s=0; s<seq_group.getSeqCount() && s<3; s++) {
        MutableAnnotatedBioSeq aseq = seq_group.getSeq(s);
        for (int i=0; i<aseq.getAnnotationCount() && i<5; i++) {
          SeqSymmetry annot = aseq.getAnnotation(i);
          SeqUtils.printSymmetry(annot, "  ", true);
        }
      }
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
