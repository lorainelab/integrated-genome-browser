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

package com.affymetrix.genometry.seq;

import java.util.Vector;
import com.affymetrix.genometry.*;

public class SimpleMultiSeqAlignment extends SimpleBioSeq 
  implements MultiSeqAlignment, MutableBioSeq {

  public Vector alignedseqs = new Vector();

  public SimpleMultiSeqAlignment(String id)  {
    super(id);
  }

  public void addSeqAlignment(CompositeBioSeq aligned_seq) {
    alignedseqs.addElement(aligned_seq);
  }

  public int getAlignmentCount() {
    return alignedseqs.size();
  }

  public CompositeBioSeq getAlignedSeq(int index) {
    return (CompositeBioSeq)alignedseqs.elementAt(index);
  } 
  
  public CompositeBioSeq getAlignedSeq(BioSeq seq) {
    for (int i=0; i<getAlignmentCount(); i++) {
      CompositeBioSeq aligned_seq = getAlignedSeq(i);
      if (aligned_seq.getComposition().getSpan(seq) != null) {
	return aligned_seq;
      }
    }
    return null;
  }
  
}

