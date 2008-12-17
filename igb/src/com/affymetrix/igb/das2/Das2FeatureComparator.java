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

package com.affymetrix.igb.das2;

import com.affymetrix.genometryImpl.das2.SimpleDas2Feature;
import java.util.Comparator;
//import com.affymetrix.genometry.*;

/**
 *  Sorts SeqSymmetries based on lexicographic ordering of IDs 
 */
public class Das2FeatureComparator implements Comparator {
  boolean use_name;

  /**
   *  if use_name == true, then try to use feature name for sorting 
   *      (only use IDs if one or both features have no name)
   *  if use_name == false, then sort by ID
   */
  public Das2FeatureComparator(boolean use_name) {
    this.use_name = use_name;
  }

  public int compare(Object obj1, Object obj2) {
    SimpleDas2Feature sym1 = (SimpleDas2Feature)obj1;
    SimpleDas2Feature sym2 = (SimpleDas2Feature)obj2;
    if (use_name) {
      String name1 = sym1.getName();
      String name2 = sym2.getName();
      if (name1 != null && name2 != null) {
	return name1.compareTo(name2);
      }
    }

    String id1 = sym1.getID();
    String id2 = sym2.getID();
    return id1.compareTo(id2);

  }
  
}
