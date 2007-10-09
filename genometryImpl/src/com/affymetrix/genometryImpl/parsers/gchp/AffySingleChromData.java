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

package com.affymetrix.genometryImpl.parsers.gchp;

import com.affymetrix.genometryImpl.util.IntList;
import java.util.*;

public class AffySingleChromData {
  
  int start;
  int count;
  String displayName;
  List<CharSequence> probeSetNames = new ArrayList<CharSequence>();
  IntList positions = new IntList();
  
  List<AffyChpColumnData> columns = new ArrayList<AffyChpColumnData>();
  
  /** Creates a new instance of SingleChromosomeData */
  public AffySingleChromData(String chromDisplayName, int start, int count, List<AffyChpColumnData> columns) {
    this.displayName = chromDisplayName;
    this.start = start;
    this.count = count;
    this.columns = columns;
  }

  @Override
  public String toString() {
    return this.getClass().getName() + " [displayName=" + displayName 
      + ", start=" + start + ", count="
      + count + ", columns=" + columns.size()+"]";
  }
}
