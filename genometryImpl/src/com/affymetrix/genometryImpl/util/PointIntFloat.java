/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.util;

import java.util.Comparator;

/**
 * Similar to java.awt.geom.Point2D, but uses int for x
 * and float for y.
 */
public class PointIntFloat {
	public int x;
	public float y;

	/**
	 * Constructor.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	public PointIntFloat(int x, float y) {
		this.x = x;
		this.y = y;
	}

	public static Comparator<PointIntFloat> getComparator(boolean sort_by_x, boolean ascending) {
		return new PointIntFloatComparator(sort_by_x, ascending);
	}

	/**
	 * Checks whether two points are equal.
	 */
	@Override
		public boolean equals(Object obj) {
			if (obj instanceof PointIntFloat) {
				PointIntFloat pt = (PointIntFloat)obj;
				return (x == pt.x) && (y == pt.y);
			}
			return false;
		}

	@Override
		public int hashCode() {
			int hash = 3;
			hash = 89 * hash + this.x;
			hash = 89 * hash + Float.floatToIntBits(this.y);
			return hash;
		}

	public boolean equals(PointIntFloat pt) {
		return (x == pt.x) && (y == pt.y);
	}

	/**
	 * Returns the String representation of the coordinates.
	 */
	@Override
		public String toString() {
			return getClass().getName() + "[x=" + x + ",y=" + y + "]";
		}

	static class PointIntFloatComparator implements Comparator<PointIntFloat>  {
		boolean compare_by_x;
		boolean ascending_order;

		public PointIntFloatComparator(boolean xcomp, boolean ascending) {
			compare_by_x = xcomp;
			ascending_order = ascending;
		}

		public int compare(PointIntFloat p1, PointIntFloat p2) {
			if (compare_by_x) {
				if (ascending_order) {  // compare by x, ascending order
					if (p1.x < p2.x) { return -1; }
					else if (p1.x > p2.x) { return 1; }
					else { return 0; }
				}
				else { // compare by x, descending order
					if (p1.x < p2.x) { return 1; }
					else if (p1.x > p2.x) { return -1; }
					else { return 0; }
				}
			}
			else {
				if (ascending_order) {  // compare by y, ascending order
					if (p1.y < p2.y) { return -1; }
					else if (p1.y > p2.y) { return 1; }
					else { return 0; }
				}
				else { // compare by y, descending order
					if (p1.y < p2.y) { return 1; }
					else if (p1.y > p2.y) { return -1; }
					else { return 0; }
				}
			}
		}

	}
}
