/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

import java.util.Comparator;
import com.affymetrix.genometry.*;

/**
 *  Sorts SeqSymmetries based on {@link SeqSpan#getStart()}.
 *  Value of {@link SeqSpan#getEnd()} is ignored.
 *
 *  @see  SeqSymMinComparator
 *  @see  SeqSymStartComparator
 */
public class SeqSymStartComparator implements Comparator<SeqSymmetry> {
  boolean ascending;
  BioSeq seq;

  /** Constructor.
   *  @param s  sequence to base the sorting on
   *  @param b  true to sort ascending, false for descending
   */
  public SeqSymStartComparator(BioSeq s, boolean b) {
    this.seq = s;
    this.ascending = b;
  }

  public void reset(BioSeq s, boolean b) {
    this.seq = s;
    this.ascending = b;
  }

  public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
    final SeqSpan span1 = sym1.getSpan(seq);
    final SeqSpan span2 = sym2.getSpan(seq);
    if (ascending) {
      if (span1.getStart() < span2.getStart()) { return -1; }
      else if (span1.getStart() > span2.getStart()) { return 1; }
      else { return 0; }
    }
    else {
      if (span1.getStart() > span2.getStart()) { return -1; }
      else if (span1.getStart() < span2.getStart()) { return 1; }
      else { return 0; }
    }
  }
}
