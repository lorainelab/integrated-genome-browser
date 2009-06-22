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

package com.affymetrix.genoviz.util;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 *  Static methods for efficient geometry operations.
 */
public class GeometryUtils {

	/**
	 *  Calculate the intersection of src1 and src2, and return as modified dst.
	 */
	public static Rectangle intersection(Rectangle src1, Rectangle src2,
			Rectangle dst) {
		int xbeg = Math.max(src1.x, src2.x);
		int xend = Math.min(src1.x + src1.width, src2.x + src2.width);
		int ybeg = Math.max(src1.y, src2.y);
		int yend = Math.min(src1.y + src1.height, src2.y + src2.height);
		dst.setBounds(xbeg, ybeg, xend - xbeg, yend - ybeg);
		return dst;
	}

	/**
	 *  Calculate the intersection of src1 and src2, and return as modified dst.
	 */
	public static Rectangle2D.Double intersection(Rectangle2D.Double src1, Rectangle2D.Double src2,
			Rectangle2D.Double dst) {
		double xbeg = Math.max(src1.x, src2.x);
		double xend = Math.min(src1.x + src1.width, src2.x + src2.width);
		double ybeg = Math.max(src1.y, src2.y);
		double yend = Math.min(src1.y + src1.height, src2.y + src2.height);
		dst.setRect(xbeg, ybeg, xend - xbeg, yend - ybeg);
		return dst;
	}

	/**
	 *  Calculate the union of src1 and src2, and return as modified dst.
	 */
	public static Rectangle union(Rectangle src1, Rectangle src2,
			Rectangle dst) {
		int xbeg = Math.min(src1.x, src2.x);
		int xend = Math.max(src1.x + src1.width, src2.x + src2.width);
		int ybeg = Math.min(src1.y, src2.y);
		int yend = Math.max(src1.y + src1.height, src2.y + src2.height);
		dst.setBounds(xbeg, ybeg, xend - xbeg, yend - ybeg);
		return dst;
	}

	/**
	 *  Calculate the union of src1 and src2, and return as modified dst.
	 */
	public static Rectangle2D.Double union(Rectangle2D.Double src1, Rectangle2D.Double src2,
			Rectangle2D.Double dst) {
		double xbeg = Math.min(src1.x, src2.x);
		double xend = Math.max(src1.x + src1.width, src2.x + src2.width);
		double ybeg = Math.min(src1.y, src2.y);
		double yend = Math.max(src1.y + src1.height, src2.y + src2.height);
		dst.setRect(xbeg, ybeg, xend - xbeg, yend - ybeg);
		return dst;
	}

}
