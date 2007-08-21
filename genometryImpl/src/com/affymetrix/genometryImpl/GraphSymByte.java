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
public class GraphSymByte extends GraphSym {
  
  private byte[] byte_y;
  
  public GraphSymByte(int[] x, byte[] y, String id, BioSeq seq) {
    super(x, id, seq);
    setCoords(x, y);
  }

  /**
   *  Sets the x and y coordinates.
   *  @param x an array of int, or null.
   *  @param y must be an array of int of same length as x, or null if x is null.
   */
  public void setCoords(int[] x, Object y) {
    setCoords(x, (byte[]) y);
  }
  
  /**
   *  Sets the x and y coordinates.
   *  @param x an array of int, or null.
   *  @param y must be an array of int of same length as x, or null if x is null.
   */
  public void setCoords(int[] x, byte[] y) {
    if ((y == null && x != null) || (x == null && y != null)) {
      throw new IllegalArgumentException("Y-coords cannot be null if x-coords are not null.");
    }
    if (y != null && x != null && y.length  != x.length) {
      throw new IllegalArgumentException("Y-coords and x-coords must have the same length.");
    }
    this.xcoords = x;
    this.byte_y = y;
  }

  public float getGraphYCoord(int i) {
    return (float) byte_y[i];
  }

  public String getGraphYCoordString(int i) {
    return Byte.toString(byte_y[i]);
  }
  
  public byte[] getGraphYCoords() {
    return byte_y;
  }

  public float[] copyGraphYCoords() {
    int pcount = getPointCount();
    float[] new_ycoords = new float[pcount];
    for (int i=0; i<pcount; i++) {
      new_ycoords[i] = (float) byte_y[i];
    }
    return new_ycoords;
  }
}
