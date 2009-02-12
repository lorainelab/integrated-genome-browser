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
 * This is <em>not</em> the same as the Java2D API Point2D class.
 * Waiting for that to settle down first.
 * Rather, this is identical to awt.Point,
 * but with doubles instead of ints
 * (and hashCode removed for now).
 */
public class Point2D {
	/**
	 * The x coordinate.
	 */
	public double x;

	/**
	 * The y coordinate.
	 */
	public double y;

	/**
	 * Constructs and initializes a Point2D from the specified x and y
	 * coordinates.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Moves the point.
	 */
	public void move(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Translates the point.
	 */
	public void translate(double x, double y) {
		this.x += x;
		this.y += y;
	}

	/**
	 * Checks whether two pointers are equal.
	 */
	public boolean equals(Object obj) {
		if (obj instanceof Point2D) {
			Point2D pt = (Point2D)obj;
			return (x == pt.x) && (y == pt.y);
		}
		return false;
	}

	/**
	 * Returns the String representation of this Point2D's coordinates.
	 */
	public String toString() {
		return getClass().getName() + "[x=" + x + ",y=" + y + "]";
	}
}
