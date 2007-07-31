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

package com.affymetrix.genometryImpl;

import com.affymetrix.genometry.BioSeq;

/**
 *  A SeqSymmetry for holding integer-based graph data.
 */
public class GraphSymInt extends GraphSym {
  
  int[] int_y;
  
  public GraphSymInt(int[] x, Object y, String id, BioSeq seq) {
    super(x, y, id, seq);
    if (! (y instanceof int[])) {
      throw new IllegalArgumentException();
    }
    int_y = (int[]) y;
  }

  public float getGraphYCoord(int i) {
    return (float) int_y[i];
  }

  public String getGraphYCoordString(int i) {
    return Integer.toString(int_y[i]);
  }

  public float[] copyGraphYCoords() {
    int pcount = getPointCount();
    float[] new_ycoords = new float[pcount];
    for (int i=0; i<pcount; i++) {
      new_ycoords[i] = (float) int_y[i];
    }
    return new_ycoords;
  }
}
