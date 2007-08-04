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
  
  float[] float_y;
  
  public GraphSymFloat(int[] x, float[] y, String id, BioSeq seq) {
    super(x, y, id, seq);
    setGraphYCoords(y);
  }

  public void setGraphYCoords(Object y) {
    this.setGraphYCoords((float[]) y);
  }
  
  public void setGraphYCoords(float[] y) {
    // for speed, this class and the superclass use different names 
    // to refer to the same object.
    super.setGraphYCoords((Object) y);
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
