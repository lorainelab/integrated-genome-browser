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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.MutableSingletonSeqSymmetry;


public class SingletonSymWithIntId extends MutableSingletonSeqSymmetry {
  int nid;

  public SingletonSymWithIntId(int start, int end, BioSeq seq, int nid) {
    super(start, end, seq);
    this.nid = nid;
  }

  public String getID() {
    return Integer.toString(nid);
  }

  public int getIntID() {
    return nid;
  }
}
