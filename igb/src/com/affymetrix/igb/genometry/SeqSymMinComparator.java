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

package com.affymetrix.igb.genometry;

import java.util.Comparator;
import com.affymetrix.genometry.*;

/**
 *  Sorts SeqSymmetries based first on {@link SeqSpan#getMin()},
 *   then on {@link SeqSpan#getMax()}.
 *
 *  @see  com.affymetrix.igb.genometry.SeqSymStartComparator
 */
public class SeqSymMinComparator implements Comparator {
  boolean ascending;
  BioSeq seq;

  /** Constructor.
   *  @param s  sequence to base the sorting on
   *  @param b  true to sort ascending, false for descending
   */
  public SeqSymMinComparator(BioSeq s, boolean b) {
    this.seq = s;
    this.ascending = b;
  }

  public void reset(BioSeq s, boolean b) {
    this.seq = s;
    this.ascending = b;
  }

  public int compare(Object obj1, Object obj2) {
    SeqSymmetry sym1 = (SeqSymmetry)obj1;
    SeqSymmetry sym2 = (SeqSymmetry)obj2;
    if (ascending) {
      if (sym1.getSpan(seq).getMin() < sym2.getSpan(seq).getMin()) { return -1; }
      else if (sym1.getSpan(seq).getMin() > sym2.getSpan(seq).getMin()) { return 1; }
      else if (sym1.getSpan(seq).getMax() < sym2.getSpan(seq).getMax()) { return -1; }
      else if (sym1.getSpan(seq).getMax() > sym2.getSpan(seq).getMax()) { return 1; }
      else { return 0; }
    }
    else {
      if (sym1.getSpan(seq).getMin() > sym2.getSpan(seq).getMin()) { return -1; }
      else if (sym1.getSpan(seq).getMin() < sym2.getSpan(seq).getMin()) { return 1; }
      else if (sym1.getSpan(seq).getMax() > sym2.getSpan(seq).getMax()) { return -1; }
      else if (sym1.getSpan(seq).getMax() < sym2.getSpan(seq).getMax()) { return 1; }
      else { return 0; }
    }
  }
  
}
