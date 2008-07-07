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

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Implements TwoDimTransform as a linear transform on each axis.
 */
public class LinearTwoDimTransform implements TwoDimTransform  {
  protected double xscale, yscale, xoffset, yoffset;


  /** 
   * Constructs a new LinearTwoDimTransform
   * with Primary and Secondary scales set at 1
   * and offsets of 0.
   */
  public LinearTwoDimTransform() {
    xscale = yscale = 1.0f;
    xoffset = yoffset = 0.0f;
  }

  /**
   * Creates a new transform with the same scales and offsets
   * as the LinearTwoDimTransform passed in.
   */
  public LinearTwoDimTransform(LinearTwoDimTransform LT) {
    super();
    copyTransform(LT);
  }

  /**
   * Sets the base transform to linearly map coordinate space -- usually in a Scene --
   * bounded by coord_box to pixel space -- used by the view of the Scene -- bounded
   * by this.pixel_box.  Should be able to "fit" a glyph hierarchy into the pixel_box
   * by calling this with the top glyph's coord_box
   */
  public LinearTwoDimTransform(Rectangle2D.Double coord_box, Rectangle pixel_box)  {
    super();
    fit(coord_box, pixel_box);
  }

  /**
   * Set the paremeters of this transform to match the given one.
   * @param LT
   */
  public void copyTransform(LinearTwoDimTransform LT) {
    xscale = LT.getScaleX();
    yscale = LT.getScaleY();
    xoffset = LT.getOffsetX();
    yoffset = LT.getOffsetY();
  }

  /**
   * Sets the transform's scales and offsets such that the coord_box's space is
   * mapped to the pixel_box's space.  For example, to map a whole Scene to a 
   * view, with no zooming, the coord_box would be the coordinate bounds of
   * the Scene, and the pixel_box the size of the NeoCanvas holding the View.
   * @param coord_box the coordinates of the Scene
   * @param pixel_box coordinates of the pixel space to which you are mapping.
   */
  public void fit(Rectangle2D.Double coord_box, Rectangle pixel_box)  {
    xscale = (double)pixel_box.width / coord_box.width;
    yscale = (double)pixel_box.height / coord_box.height;
    xoffset = (double)pixel_box.x - xscale * coord_box.x;
    yoffset = (double)pixel_box.y - yscale * coord_box.y;
  }

  /**
   * Transforms the coordinate on the axis indicated.
   * If transform is being used in between a scene and a view,
   * this would convert from scene coordinates to view/pixel coordinates.
   * @param orientation 
   * @param in   the coordinate
   */
  @Override
  public double transform(WidgetAxis orientation, double in) {
    final double out;
    switch (orientation) {
      case Primary:
        out = in * xscale + xoffset;
        break;
      case Secondary:
        out = in * yscale + yoffset;
        break;
      default:
        throw new RuntimeException();
    }
    return out;
  }

  /**
   * Transforms the coordinate inversely on the axis indicated.
   * If transform is being used in between a scene and a view,
   * this would convert from  view/pixel coordinates to Scene coordinates.
   * @param orientation the WidgetAxis
   * @param in the coordinate
   */
  @Override
  public double inverseTransform(WidgetAxis orientation, double in) {
    final double out;
    switch (orientation) {
      case Primary:
        out = (in - xoffset) / xscale;
        break;
      case Secondary:
        out = (in - yoffset) / yscale;
        break;
      default:
        throw new RuntimeException();
    }
    return out;
  }

  /** 
   * Sets the scale of the transform directly.
   * @param x Primary scale
   * @param y Secondary scale
   */
  public void setScale(double x, double y) {
    xscale = x;
    yscale = y;
  }

  /**
   * Sets the offsets directly.
   * @param x Primary offset
   * @param y Secondary offset
   */
  public void setTranslation(double x, double y) {
    xoffset = x;
    yoffset = y;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public Rectangle2D.Double transform(java.awt.geom.Rectangle2D.Double src, java.awt.geom.Rectangle2D.Double dst) {
    dst.x = src.x * xscale + xoffset;
    dst.y = src.y * yscale + yoffset;
    dst.width = src.width * xscale;
    dst.height = src.height * yscale;
    if (dst.height < 0) {
      dst.y = dst.y + dst.height;
      dst.height = -dst.height;
    }
    if (dst.width < 0) {
      dst.x = dst.x + dst.width;
      dst.width = -dst.width;
    }
    return dst;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public Rectangle2D.Double inverseTransform(java.awt.geom.Rectangle2D.Double src, java.awt.geom.Rectangle2D.Double dst) {
    dst.x = (src.x - xoffset) / xscale;
    dst.y = (src.y - yoffset) / yscale;
    dst.width = src.width / xscale;
    dst.height = src.height / yscale;

    if (dst.height < 0) {
      dst.y = dst.y + dst.height;
      dst.height = -dst.height;
    }
    if (dst.width < 0) {
      dst.x = dst.x + dst.width;
      dst.width = -dst.width;
    }
    return dst;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public Point2D.Double transform(Point2D.Double src, Point2D.Double dst) {
    dst.x = src.x * xscale + xoffset;
    dst.y = src.y * yscale + yoffset;
    return dst;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public Point2D.Double inverseTransform(Point2D.Double src, Point2D.Double dst) {
    dst.x = (src.x - xoffset) / xscale;
    dst.y = (src.y - yoffset) / yscale;
    return dst;
  }

  /**
   * Appends one transformation to another
   * to build a cumulative transformation.
   */
  public void append(LinearTwoDimTransform LT) {
    xoffset = LT.xscale * xoffset + LT.xoffset;
    yoffset = LT.yscale * yoffset + LT.yoffset;
    xscale = xscale * LT.xscale;
    yscale = yscale * LT.yscale;
  }

  /**
   * Prepends one transformation to another
   * to build a cumulative transformation.
   */
  public void prepend(LinearTwoDimTransform LT) {
    xoffset = xscale * LT.xoffset + xoffset;
    yoffset = yscale * LT.yoffset + yoffset;
    xscale = xscale * LT.xscale;
    yscale = yscale * LT.yscale;
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

  /**
   * creates a shallow copy.
   * Since the only instance variables are doubles,
   * it is also trivially a deep copy.
   */
  @Override
  public LinearTwoDimTransform clone() throws CloneNotSupportedException {
    return (LinearTwoDimTransform) super.clone();
  }

  @Override
  public String toString() {
    return ("LinearTransform:  xscale = " + xscale + ", xoffset = " +
            xoffset + ",  yscale = " + yscale + ", yoffset " + yoffset);
  }

  @Override
  public boolean equals(Object Tx) {
    if (Tx instanceof LinearTwoDimTransform) {
      LinearTwoDimTransform lint = (LinearTwoDimTransform)Tx;
      return (xscale == lint.getScaleX() &&
              yscale == lint.getScaleY() &&
              xoffset == lint.getOffsetX() &&
              yoffset == lint.getOffsetY());
    }
    else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 89 * hash + (int) (Double.doubleToLongBits(this.xscale) ^ (Double.doubleToLongBits(this.xscale) >>> 32));
    hash = 89 * hash + (int) (Double.doubleToLongBits(this.yscale) ^ (Double.doubleToLongBits(this.yscale) >>> 32));
    hash = 89 * hash + (int) (Double.doubleToLongBits(this.xoffset) ^ (Double.doubleToLongBits(this.xoffset) >>> 32));
    hash = 89 * hash + (int) (Double.doubleToLongBits(this.yoffset) ^ (Double.doubleToLongBits(this.yoffset) >>> 32));
    return hash;
  }

}
