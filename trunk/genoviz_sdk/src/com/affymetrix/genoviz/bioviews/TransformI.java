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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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
	 * first dimension
	 */
	public static final int X = 0;
	/**
	 * second dimension
	 */
	public static final int Y = 1;

	/**
	 * transforms a single coordinate
	 * in the given dimension.
	 *
	 * @param orientation the dimension (X or Y)
	 * @param in the coordinate to be transformed
	 *
	 * @return the coordinate transformed
	 */
	public double transform(int orientation, double in);

	/**
	 * inverts a single coordinate transformation.
	 *
	 * @param orientation the dimension (X or Y)
	 * @param in the coordinate to be transformed
	 *
	 * @return the coordinate transformed
	 */
	public double inverseTransform(int orientation, double in);


	/**
	 * transforms a Point2D.
	 *
	 * @param src the point to transform
	 * @param dst the point transformed
	 *
	 * @return the point transformed
	 */
	public Point2D.Double transform(Point2D.Double src, Point2D.Double dst);

	/**
	 * inverts the transformation of a Point2D.
	 *
	 * @param src the point to transform
	 * @param dst the point transformed
	 *
	 * @return the point transformed
	 */
	public Point2D.Double inverseTransform(Point2D.Double src, Point2D.Double dst);

	/**
	 * transforms a rectangle.
	 *
	 * @param src the rectangle to transform
	 * @param dst the rectangle transformed
	 *
	 * @return the rectangle transformed
	 */
	public Rectangle2D.Double transform(Rectangle2D.Double src, Rectangle2D.Double dst);

	/**
	 * inverts the transformation of a rectangle.
	 *
	 * @param src the rectangle to transform
	 * @param dst the rectangle transformed
	 *
	 * @return the rectangle transformed
	 */
	public Rectangle2D.Double inverseTransform(Rectangle2D.Double src, Rectangle2D.Double dst);

	/**
	 * appends one transformation to another
	 * to build a cumulative transformation.
	 */
	public void append(TransformI Tx);

	/**
	 * prepends one transformation to another
	 * to build a cumulative transformation.
	 */
	public void prepend(TransformI Tx);

	/**
	 * creates a clone of the TransormI.
	 *
	 * @return the new clone.
	 */
	public Object clone()throws CloneNotSupportedException;

	/**
	 * establishes equality to a transform.
	 */
	public boolean equals(TransformI Tx);
}
