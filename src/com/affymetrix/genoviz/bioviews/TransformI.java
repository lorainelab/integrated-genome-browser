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

/**
 * An object implementing this interface
 * can transform points
 * (defined by a pair of coordinates)
 * in one two-dimensional space
 * to another two-dimensional space.
 * <p>
 * Note: At some future point
 * the inverse functions should throw
 * a "NonInvertableException"
 * of some sort.
 */
public interface TransformI extends Cloneable  {

  /**
   * Transforms a single coordinate
   * in the given dimension.
   *
   * @param dim the dimension
   * @param in the coordinate to be transformed
   *
   * @return the coordinate transformed
   */
  public double transform(WidgetAxis dim, double in);

  /**
   * Inverts a single coordinate transformation.
   *
   * @param dim the dimension
   * @param in the coordinate to be transformed
   *
   * @return the coordinate transformed
   */
  public double inverseTransform(WidgetAxis dim, double in);


  /**
   * Transforms a Point2D.
   *
   * @param src the point to transform
   * @param dst the point transformed
   *
   * @return the point transformed
   */
  public Point2D.Double transform(Point2D.Double src, Point2D.Double dst);

  /**
   * Inverts the transformation of a Point2D.
   *
   * @param src the point to transform
   * @param dst the point transformed
   *
   * @return the point transformed
   */
  public Point2D.Double inverseTransform(Point2D.Double src, Point2D.Double dst);

  /**
   * Transforms a rectangle.
   *
   * @param src the rectangle to transform
   * @param dst the rectangle transformed
   *
   * @return the rectangle transformed
   */
  public java.awt.geom.Rectangle2D.Double transform(java.awt.geom.Rectangle2D.Double src, java.awt.geom.Rectangle2D.Double dst);

  /**
   * Inverts the transformation of a rectangle.
   *
   * @param src the rectangle to transform
   * @param dst the rectangle transformed
   *
   * @return the rectangle transformed
   */
  public java.awt.geom.Rectangle2D.Double inverseTransform(java.awt.geom.Rectangle2D.Double src, java.awt.geom.Rectangle2D.Double dst);

  /**
   * Appends one transformation to another
   * to build a cumulative transformation.
   */
  public void append(TransformI Tx);

  /**
   * prepends one transformation to another
   * to build a cumulative transformation.
   */
  public void prepend(TransformI Tx);

  /**
   * Creates a clone of the TransormI.
   *
   * @return the new clone.
   */
  public Object clone() throws CloneNotSupportedException;

  /**
   * Establishes equality of a transform.
   */
  public boolean equals(TransformI Tx);
}
