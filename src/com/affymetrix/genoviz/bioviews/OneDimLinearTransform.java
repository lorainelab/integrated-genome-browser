/**
*   Copyright (c) 2008 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.bioviews;

/**
 * Implements OndDimTransform as a linear transform.
 */
public class OneDimLinearTransform implements OneDimTransform  {
  private double xscale, xoffset;

  /** 
   * Constructs a new LinearTransform
   * with Primary and Secondary scales set at 1
   * and offsets of 0.
   */
  public OneDimLinearTransform() {
    xscale = 1.0f;
    xoffset = 0.0f;
  }

//  /**
//   * Sets the transform's scales and offsets such that the coord_box's space is
//   * mapped to the pixel_box's space.  For example, to map a whole Scene to a 
//   * view, with no zooming, the coord_box would be the coordinate bounds of
//   * the Scene, and the pixel_box the size of the NeoCanvas holding the View.
//   * @param coord_box the coordinates of the Scene
//   * @param pixel_box coordinates of the pixel space to which you are mapping.
//   */
//  public void fit(Rectangle2D.Double coord_box, Rectangle pixel_box) {
//    xscale = (double)pixel_box.width / coord_box.width;
//    xoffset = (double)pixel_box.x - xscale * coord_box.x;
//  }

  @Override
  public double transform(double in) {
    return in * xscale + xoffset;
  }

  @Override
  public double inverseTransform(double in) {
    return (in - xoffset) / xscale;
  }

  /**
   * Appends one transformation to another
   * to build a cumulative transformation.
   */
  public void append(OneDimLinearTransform LT) {
    xoffset = LT.xscale * xoffset + LT.xoffset;
    xscale = xscale * LT.xscale;
  }

  /**
   * Prepends one transformation to another
   * to build a cumulative transformation.
   */
  public void prepend(OneDimLinearTransform LT) {
    xoffset = xscale * LT.xoffset + xoffset;
    xscale = xscale * LT.xscale;
  }

  public double getScaleX() {
    return xscale;
  }

  public void setScaleX(double scale) {
    xscale = scale;
  }

  public double getOffsetX() {
    return xoffset;
  }

  public void setOffsetX(double offset) {
    xoffset = offset;
  }


  /**
   * Creates a shallow copy.
   * Since the only instance variables are doubles,
   * it is also trivially a deep copy.
   */
  @Override
  public OneDimLinearTransform clone() throws CloneNotSupportedException {
    return (OneDimLinearTransform) super.clone();
  }

  @Override
  public String toString() {
    return ("OneDimLinearTransform:  scale = " + xscale + ", offset = " + xoffset);
  }

  @Override
  public boolean equals(Object Tx) {
    if (Tx instanceof OneDimLinearTransform) {
      OneDimLinearTransform lint = (OneDimLinearTransform)Tx;
      return (xscale == lint.getScaleX() &&
              xoffset == lint.getOffsetX());
    }
    else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 89 * hash + (int) (Double.doubleToLongBits(this.xscale) ^ (Double.doubleToLongBits(this.xscale) >>> 32));
    hash = 89 * hash + (int) (Double.doubleToLongBits(this.xoffset) ^ (Double.doubleToLongBits(this.xoffset) >>> 32));
    return hash;
  }

}
