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

import java.awt.*;
import java.lang.*;

public class RotateLinearTransform extends LinearTransform {

  public RotateLinearTransform() {
    xscale = yscale = 1.0f;
    xoffset = yoffset = 0.0f;
  }

  public RotateLinearTransform(LinearTransform LT) {
    xscale = yscale = 1.0f;
    xoffset = yoffset = 0.0f;
  }

  /**
   * Sets the base transform to linearly map coordinate space bounded
   * by coord_box to pixel space bounded by this.pixel_box.
   * Should be able to "fit" a glyph hierarchy into the pixel_box
   * by calling this with the top glyph's coord_box.
   */
  public RotateLinearTransform(Rectangle2D coord_box, Rectangle pixel_box)
  {
    xscale = (double)pixel_box.width / coord_box.width;
    yscale = (double)pixel_box.height / coord_box.height;
    xoffset = (double)pixel_box.x - xscale * coord_box.x;
    yoffset = (double)pixel_box.y - yscale * coord_box.y;
  }

  public Rectangle2D transform(Rectangle2D src, Rectangle2D dst) {
    dst.x = src.y * yscale + yoffset;
    dst.y = src.x * xscale + xoffset;
    dst.width = src.height * yscale;
    dst.height = src.width * xscale;
    return dst;
  }

  public Rectangle2D inverseTransform(Rectangle2D src, Rectangle2D dst) {
    dst.x = (src.y - yoffset) / yscale;
    dst.y = (src.x - xoffset) / xscale;
    dst.width = src.height / yscale;
    dst.height = src.width / xscale;
    return dst;
  }

  public Point2D transform(Point2D src, Point2D dst) {
    dst.x = src.x * xscale + xoffset;
    dst.y = src.y * yscale + yoffset;
    return dst;
  }

  public Point2D inverseTransform(Point2D src, Point2D dst) {
    dst.x = (src.x - xoffset) / xscale;
    dst.y = (src.y - yoffset) / yscale;
    return dst;
  }

  /* Why not put these in a LinearTransformI interface? */

  public void append(TransformI T) {
    // MUST CHANGE SOON to throw IncompatibleTransformException
    if (! (T instanceof LinearTransform)) { return; }
    else {
      LinearTransform LT = (LinearTransform)T;
      xoffset = LT.xscale * xoffset + LT.xoffset;
      yoffset = LT.yscale * yoffset + LT.yoffset;
      xscale = xscale * LT.xscale;
      yscale = yscale * LT.yscale;
    }
  }

  public void prepend(TransformI T) {
    // MUST CHANGE SOON to throw IncompatibleTransformException
    if (! (T instanceof LinearTransform)) { return; }
    else {
      LinearTransform LT = (LinearTransform)T;
      xoffset = xscale * LT.xoffset + xoffset;
      yoffset = yscale * LT.yoffset + yoffset;
      xscale = xscale * LT.xscale;
      yscale = yscale * LT.yscale;
    }
  }

  public double getScaleX() {
    return xscale;
  }

  public double getScaleY() {
    return yscale;
  }

  public void setScaleX(double scale) {
    xscale = scale;
  }

  public void setScaleY(double scale) {
    yscale = scale;
  }

  public double getOffsetX() {
    return xoffset;
  }

  public double getOffsetY() {
    return yoffset;
  }

  public void setOffsetX(double offset) {
    xoffset = offset;
  }

  public void setOffsetY(double offset) {
    yoffset = offset;
  }

  public String toString() {
    return ("RotateLinearTransform:  xscale = " + xscale + ", xoffset = " +
        xoffset + ",  yscale = " + yscale + ", yoffset " + yoffset);
  }

  // not checking for transform value equality right now
  public boolean equals(TransformI Tx) {
    return super.equals(Tx);
  }


}
