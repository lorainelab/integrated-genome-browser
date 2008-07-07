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
import java.awt.geom.Rectangle2D;

/**
 * A transform used internally by some NeoWidgets to handle zooming, should
 *    not be used directly.
 *
 * This is a one-dimesional transform.  It ignores the second dimension.
 * 
 * ExponentialTransform is the start of replacing application handling
 *    of things such as zooming a map with scrollbars that can take
 *    both transforms and listeners and do the right thing
 *    Right now it only does about half the work involved in
 *    the "gradual deceleration" of the zoom scrollbar
 */
//TODO: fix comments
public class ExponentialTransform implements TransformI {
  protected double xmax, xmin, ymax, ymin, lxmax, lxmin, ratio;

  // for zoomer transform, x is transformed to y
  public ExponentialTransform(double xmin, double xmax, double ymin, double ymax) {
    this.xmax = xmax;
    this.xmin = xmin;
    this.ymax = ymax;
    this.ymin = ymin;
    lxmax = Math.log(xmax);
    lxmin = Math.log(xmin);
    ratio = (lxmax-lxmin)/(ymax-ymin);
  }

  /** @param dim ignored */
  @Override
  public double transform(WidgetAxis dim, double in) {
    double out = Math.exp(in*ratio + lxmin);
    /*
     *  Fix for zooming -- for cases where y _should_ be 7, but ends up
     *  being 6.9999998 or thereabouts because of errors in Math.exp()
     */
    if ( Math.abs(out) > .1) {
      double outround = Math.round(out);
      if (Math.abs(out-outround) < 0.0001) {
        out = outround;
      }
    }
    return out;
  }

  /** @param dim ignored */
  @Override
  public double inverseTransform(WidgetAxis dim, double in) {
    return (Math.log(in)-lxmin) / ratio;
  }

  @Override
  public Point2D.Double transform(Point2D.Double src, Point2D.Double dst) {
    // y = f(x), but in this case y is really dst.x
    //   (Exponential is a one-dimensional transform, ignores src.y & dst.y
    dst.x = Math.exp(src.x * ratio);
    return dst;
  }

  @Override
  //TODO: Fix inverse.  This doesn't match inverseTransform(WidgetAxis, double)
  public Point2D.Double inverseTransform(Point2D.Double src, Point2D.Double dst) {
    dst.x = Math.log(src.x/ratio);
    return dst;
  }

  @Override
  public Rectangle2D.Double transform(java.awt.geom.Rectangle2D.Double src, java.awt.geom.Rectangle2D.Double dst) {
    dst.x = Math.exp(src.x * ratio);
    return dst;
  }

  @Override
  public Rectangle2D.Double inverseTransform(java.awt.geom.Rectangle2D.Double src, java.awt.geom.Rectangle2D.Double dst) {
    dst.x = Math.log(src.x/ratio);
    return dst;
  }

  /**
   * Creates a shallow copy.
   * Since the only instance variables are doubles,
   * it is also trivially a deep copy.
   */
  @Override
  public ExponentialTransform clone() throws CloneNotSupportedException {
    return (ExponentialTransform) super.clone();
  }
}
