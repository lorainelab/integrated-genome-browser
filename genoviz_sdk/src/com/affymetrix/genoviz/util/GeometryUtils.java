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

import java.awt.geom.Rectangle2D;

/**
 *  Static methods for efficient geometry operations.
 */
public class GeometryUtils {

	/**
	 *  Calculate the union of src1 and src2, and return as modified dst.
	 */
	public static Rectangle2D.Double union(Rectangle2D.Double src1, Rectangle2D.Double src2,
			Rectangle2D.Double dst) {
		Rectangle2D.union(src1, src2, dst);
		return dst;
	}

}
