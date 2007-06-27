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

package com.affymetrix.genometryImpl;

import java.util.Comparator;
import com.affymetrix.genometry.*;

/**
 *  Compares two SeqSpan objects based on the order of their minima and maxima.
 *  Always orders in ascending order first by {@link SeqSpan#getMin()}, 
 *  then by {@link SeqSpan#getMax()}, regardless of the span orientations.
 */
public class SeqSpanComparator implements Comparator {
  public int compare(Object obj1, Object obj2) {
    SeqSpan span1 = (SeqSpan)obj1;
    SeqSpan span2 = (SeqSpan)obj2;
    if (span1.getMin() < span2.getMin()) {
      return -1;
    }
    else if (span1.getMin() > span2.getMin()) {
      return 1;
    }
    else {
      // secondary sort by max
      if (span1.getMax() < span2.getMax()) {
	return -1;
      }
      else if (span1.getMax() > span2.getMax()) {
	return 1;
      }
      else {
	// spans are equivalent (span1.getMin() == span2.getMin() && span1.getMax() == span2.getMax())
	return 0; 
      }
    }
  }
}
