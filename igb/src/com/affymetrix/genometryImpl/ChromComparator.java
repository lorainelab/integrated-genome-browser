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

import java.util.*;
import com.affymetrix.genometry.BioSeq;

/**
 *  Trying to order chromosomes in a sensible way based on ids.
 */
public class ChromComparator implements Comparator  {

  /** Objects must be of type String. */
  public int compare(Object obj1, Object obj2) {
    return compare((String) obj1, (String) obj2);
  }
  
  public int compare(String str1, String str2) {
    if (str1.length() < str2.length()) {
      return -1;
    }
    else if (str1.length() > str2.length()) {
      return 1;
    }
    else {
      return str1.compareTo(str2);
    }
  }
}
