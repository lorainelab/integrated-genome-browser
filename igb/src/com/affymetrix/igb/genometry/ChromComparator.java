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

import java.util.*;
import com.affymetrix.genometry.BioSeq;

/**
 *  Trying to order chromosomes in a sensible way based on ids
 */
public class ChromComparator implements Comparator  {

  public int compare(Object obj1, Object obj2) {
    String str1 = ((BioSeq)obj1).getID();
    String str2 = ((BioSeq)obj2).getID();
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
