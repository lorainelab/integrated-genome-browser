/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.bioviews;

/**
 *  A transform used internally by NeoSeq, should not be used directly.
 */
public class IntegralTransform extends LinearTransform {

  /**
   *  if INTEGRAL_TRANSFORM_PARAM, then round PARAM to nearest integer when
   *  performing transform
   */
  public boolean INTEGRAL_TRANSFORM;
  public boolean INTEGRAL_TRANSFORM_X = false;
  public boolean INTEGRAL_TRANSFORM_Y = false;
  public boolean INTEGRAL_TRANSFORM_WIDTH = false;
  public boolean INTEGRAL_TRANSFORM_HEIGHT = false;

  /**
   *  if INTEGRAL_INVERSE_PARAM, then round PARAM to nearest integer when
   *  performing inverse transform
   */
  public boolean INTEGRAL_INVERSE;
  public boolean INTEGRAL_INVERSE_X = false;
  public boolean INTEGRAL_INVERSE_Y = false;
  public boolean INTEGRAL_INVERSE_WIDTH = false;
  public boolean INTEGRAL_INVERSE_HEIGHT = false;

  public IntegralTransform() {
    super();
    setOverallIntegrals();
  }

  public IntegralTransform(LinearTransform LT) {
    super(LT);
    setOverallIntegrals();
  }

  /**
   * setting which fields to round when doing inverseTransform() of
   * Rectangle2D
   */
  public void setIntegralInverse(boolean x, boolean y,
                                 boolean width, boolean height) {
    INTEGRAL_INVERSE_X = x;
    INTEGRAL_INVERSE_Y = y;
    INTEGRAL_INVERSE_WIDTH = width;
    INTEGRAL_INVERSE_HEIGHT = height;
    setOverallIntegrals();
  }

  /**
   * setting which fields to round when doing transform() of Rectangle2D
   */
  public void setIntegralTransform(boolean x, boolean y,
                                   boolean width, boolean height) {
    INTEGRAL_TRANSFORM_X = x;
    INTEGRAL_TRANSFORM_Y = y;
    INTEGRAL_TRANSFORM_WIDTH = width;
    INTEGRAL_TRANSFORM_HEIGHT = height;
    setOverallIntegrals();
  }

  protected void setOverallIntegrals() {
    if (INTEGRAL_TRANSFORM_X || INTEGRAL_TRANSFORM_WIDTH ||
        INTEGRAL_TRANSFORM_Y || INTEGRAL_TRANSFORM_HEIGHT) {
      INTEGRAL_TRANSFORM = true;
    }
    else {
      INTEGRAL_TRANSFORM = false;
    }
    if (INTEGRAL_INVERSE_X || INTEGRAL_INVERSE_WIDTH ||
        INTEGRAL_INVERSE_Y || INTEGRAL_INVERSE_HEIGHT) {
      INTEGRAL_INVERSE = true;
    }
    else {
      INTEGRAL_INVERSE = false;
    }
  }

  public Rectangle2D transform(Rectangle2D src, Rectangle2D dst) {
    super.transform(src, dst);
    if (INTEGRAL_TRANSFORM) {
      if (INTEGRAL_TRANSFORM_X) {
        dst.x = (double)Math.round(dst.x); }
      if (INTEGRAL_TRANSFORM_Y) {
        dst.y = (double)Math.round(dst.y); }
      if (INTEGRAL_TRANSFORM_WIDTH) {
        dst.width = (double)Math.round(dst.width); }
      if (INTEGRAL_TRANSFORM_HEIGHT) {
        dst.height = (double)Math.round(dst.height);
      }
    }
    return dst;
  }

  public Rectangle2D inverseTransform(Rectangle2D src, Rectangle2D dst) {
    super.inverseTransform(src, dst);
    if (INTEGRAL_INVERSE) {
      if (INTEGRAL_INVERSE_X) {
        dst.x = (double)Math.round(dst.x); }
      if (INTEGRAL_INVERSE_Y) {
        dst.y = (double)Math.round(dst.y); }
      if (INTEGRAL_INVERSE_WIDTH) {
        dst.width = (double)Math.round(dst.width); }
      if (INTEGRAL_INVERSE_HEIGHT) {
        dst.height = (double)Math.round(dst.height);
      }
    }
    return dst;
  }

  public String toString() {
    return ("IntegralTransform:  xscale = " + xscale + ", xoffset = " +
            xoffset + ",  yscale = " + yscale + ", yoffset " + yoffset);
  }


}
