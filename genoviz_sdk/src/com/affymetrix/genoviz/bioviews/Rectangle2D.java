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
 * This is similar to, but <em>not</em> the same as the {java.awt.geom.Rectangle2D.Double},
 * which was not available at the time this class was written.
 * Some methods may change
 * due to differences when dealing with (almost)real numbers instead of integers.
 * See, for example, intersects(Rectangle2D)
 */
public class Rectangle2D
//extends java.awt.geom.Rectangle2D.Double
{

	/**
	 * The x coordinate of the rectangle.
	 */
	public double x;

	/**
	 * The y coordinate of the rectangle.
	 */
	public double y;

	/**
	 * The width of the rectangle.
	 */
	public double width;

	/**
	 * The height of the rectangle.
	 */
	public double height;

	/**
	 * Constructs a new rectangle.
	 */
	public Rectangle2D() {
		super();
	}

	/**
	 * Constructs and initializes a rectangle with the specified parameters.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param width the width of the rectangle
	 * @param height the height of the rectangle
	 */
	public Rectangle2D(double x, double y, double width, double height) {
		this.setRect(x,y,width,height);
	}

	/**
	 * Reshapes the rectangle.
	 */
	public void setRect(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setRect(Rectangle2D r) {
		this.setRect(r.x,r.y,r.width,r.height);
	}

	/**
	 *  Reshape this rectangle to same coords as r
	 */
	/*public void copyRect(Rectangle2D r) {
		this.setRect(r);
	}*/

	/**
	 * Checks if two rectangles intersect.
	 * Just like Rectangle, we consider Rectangle2Ds that only
	 * share an edge as <em>not</em> intersecting.
	 */
	public boolean intersects(Rectangle2D r) {

		// Just like Rectangle, sharing an edge is not considered intersection
		return !((r.x + r.width <= x) ||
				(r.y + r.height <= y) ||
				(r.x >= x + width) ||
				(r.y >= y + height));
	}

	/**
	 * Computes the intersection of two rectangles.
	 */
	public Rectangle2D intersection(Rectangle2D r) {
		double x1 = Math.max(x, r.x);
		double x2 = Math.min(x + width, r.x + r.width);
		double y1 = Math.max(y, r.y);
		double y2 = Math.min(y + height, r.y + r.height);
		return new Rectangle2D(x1, y1, x2 - x1, y2 - y1);
	}

	/**
	 * Computes the union of two rectangles.
	 */
	public Rectangle2D union(Rectangle2D r) {
		double x1 = Math.min(x, r.x);
		double x2 = Math.max(x + width, r.x + r.width);
		double y1 = Math.min(y, r.y);
		double y2 = Math.max(y + height, r.y + r.height);
		return new Rectangle2D(x1, y1, (x2 - x1), (y2 - y1));
	}

	/**
	 * Adds a rectangle to a rectangle.
	 * This results in the union of the two rectangles.
	 */
	public void add(Rectangle2D r) {
		double x1 = Math.min(x, r.x);
		double x2 = Math.max(x + width, r.x + r.width);
		double y1 = Math.min(y, r.y);
		double y2 = Math.max(y + height, r.y + r.height);
		x = x1;
		y = y1;
		width = x2 - x1;
		height = y2 - y1;
	}

	/**
	 * Adds a point to a rectangle.
	 * This results in the smallest rectangle
	 * that contains both the rectangle and the point.
	 */
	public void add(double newx, double newy) {
		double x1 = Math.min(x, newx);
		double x2 = Math.max(x + width, newx);
		double y1 = Math.min(y, newy);
		double y2 = Math.max(y + height, newy);
		x = x1;
		y = y1;
		width = x2 - x1;
		height = y2 - y1;
	}

	/**
	 * Checks whether two rectangles are equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Rectangle2D) {
			Rectangle2D r = (Rectangle2D)obj;
			return (x == r.x) && (y == r.y) && (width == r.width) && (height == r.height);
		}
		return false;
	}

	
	/**
	 * Returns the String representation of this Rectangle2D's values.
	 */
	@Override
	public String toString() {
		return "GenoViz Rect2D: xmin = " + x + ", xmax = " + (x+width) +
			", ymin = " + y + ", ymax = " + (y+height);
	}

}
