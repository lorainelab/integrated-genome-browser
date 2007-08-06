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
  
  private int[] int_y;
  
  public GraphSymInt(int[] x, int[] y, String id, BioSeq seq) {
    super(x, id, seq);
    setCoords(x, y);
  }

  public void setCoords(int[] x, int[] y) {
    if (y == null && xcoords != null) {
      throw new IllegalArgumentException("Y-coords cannot be null if x-coords are not null.");
    }
    if (y.length  != xcoords.length) {
      throw new IllegalArgumentException("Y-coords and x-coords must have the same length.");
    }
    this.xcoords = x;
    this.int_y = y;
  }

  public float getGraphYCoord(int i) {
    return (float) int_y[i];
  }

  public String getGraphYCoordString(int i) {
    return Integer.toString(int_y[i]);
  }
  
  public int[] getGraphYCoords() {
    return int_y;
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
