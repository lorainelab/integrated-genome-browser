/**
*   Copyright (c) 1998-2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.bioviews;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;

/**
 * A two-dimensional transform which can be expressed
 * as two separate one-dimensional transforms acting
 * independently on the two axes.
 * @author Ed Erwin
 */
public class SeparableTwoDimTransform implements TwoDimTransform {
  
  OneDimTransform xTransform;
  OneDimTransform yTransform;
  
  public SeparableTwoDimTransform(OneDimTransform x, OneDimTransform y) {
    this.xTransform = x;
    this.yTransform = y;
  }

  @Override
  public double transform(WidgetAxis dim, double in) {
    if (dim == WidgetAxis.Primary) {
      return xTransform.transform(in);
    } else {
      return yTransform.transform(in);
    }
  }

  @Override
  public double inverseTransform(WidgetAxis dim, double in) {
    if (dim == WidgetAxis.Primary) {
      return xTransform.inverseTransform(in);
    } else {
      return yTransform.inverseTransform(in);
    }
  }

  @Override
  public Double transform(Point2D.Double src, Point2D.Double dst) {
    dst.x = xTransform.transform(src.x);
    dst.y = yTransform.transform(src.y);
    return dst;
  }

  @Override
  public Double inverseTransform(Point2D.Double src, Point2D.Double dst) {
    dst.x = xTransform.inverseTransform(src.x);
    dst.y = yTransform.inverseTransform(src.y);
    return dst;
  }

  @Override
  public Rectangle2D.Double transform(Rectangle2D.Double src, Rectangle2D.Double dst) {
    dst.x = xTransform.transform(src.x);
    dst.y = yTransform.transform(src.y);    
    dst.width = xTransform.transform(src.x+src.width) - dst.x;
    dst.height = yTransform.transform(src.y+src.height) - dst.y;      
    return dst;
  }

  @Override
  public Rectangle2D.Double inverseTransform(Rectangle2D.Double src, Rectangle2D.Double dst) {
    dst.x = xTransform.inverseTransform(src.x);
    dst.y = yTransform.inverseTransform(src.y);    
    dst.width = xTransform.inverseTransform(src.x+src.width) - dst.x;
    dst.height = yTransform.inverseTransform(src.y+src.height) - dst.y;      
    return dst;
  }

  @Override
  public SeparableTwoDimTransform clone() throws CloneNotSupportedException {
    SeparableTwoDimTransform copy = (SeparableTwoDimTransform) super.clone();
    copy.xTransform = xTransform.clone();
    copy.yTransform = yTransform.clone();
    return copy;
  }

}
