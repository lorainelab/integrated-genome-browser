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
 *  A SeqSymmetry for holding floating-point graph data.
 */
public class GraphSymFloat extends GraphSym {
  
  private float[] float_y;
  
  public GraphSymFloat(int[] x, float[] y, String id, BioSeq seq) {
    super(x, id, seq);
    setCoords(x, y);
  }
  
  public void setCoords(int[] x, float[] y) {
    if (y == null && xcoords != null) {
      throw new IllegalArgumentException("Y-coords cannot be null if x-coords are not null.");
    }
    if (y.length  != xcoords.length) {
      throw new IllegalArgumentException("Y-coords and x-coords must have the same length.");
    }
    this.xcoords = x;
    this.float_y = y;
  }
  
  public float getGraphYCoord(int i) {
    return float_y[i];
  }

  public String getGraphYCoordString(int i) {
    return Float.toString(float_y[i]);
  }
  
  public float[] getGraphYCoords() {
    return float_y; 
  }

  public float[] copyGraphYCoords() {
    float[] dest = new float[float_y.length];
    System.arraycopy(float_y, 0, dest, 0, float_y.length);
    return dest;
  }
}
