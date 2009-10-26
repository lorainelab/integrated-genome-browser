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
 *  A transform used internally by NeoSeq, should not be used directly.
 */
public final class ConstrainLinearTrnsfm extends LinearTransform {

	protected double constrain_value;

	public ConstrainLinearTrnsfm() {
		constrain_value = 1;
	}

	public void setConstrainValue(double cv) {
		constrain_value = cv;
	}

	public double getConstrainValue() {
		return constrain_value;
	}

	public double transform(int orientation, double in) {
		double out = 0;
		if (orientation == X) {
			out = in * xscale;
		} else if (orientation == Y) {
			out = in * yscale;
		}

		out = out - (out % constrain_value);
	
		if (orientation == X) {
			out += xoffset;
		} else if (orientation == Y) {
			out += yoffset;
		}

		return out;
	}

	public double inverseTransform(int orientation, double in) {
		double out = 0;
		if (orientation == X) {
			out = (in - xoffset) / xscale;
		} else if (orientation == Y) {
			out = (in - yoffset) / yscale;
		}
		return out;
	}

	public void setScale(double x, double y) {
		xscale = x;
		yscale = y;
	}

	public void setTranslation(double x, double y) {
		xoffset = x;
		yoffset = y;
	}

	public Rectangle2D.Double transform(Rectangle2D.Double src, Rectangle2D.Double dst) {
		dst.x = src.x * xscale + xoffset;
		dst.y = src.y * yscale + yoffset;
		dst.width = src.width * xscale;
		dst.height = src.height * yscale;
		return dst;
	}

	public Rectangle2D.Double inverseTransform(Rectangle2D.Double src, Rectangle2D.Double dst) {
		dst.x = (src.x - xoffset) / xscale;
		dst.y = (src.y - yoffset) / yscale;
		dst.width = src.width / xscale;
		dst.height = src.height / yscale;
		return dst;
	}

	public Point2D.Double transform(Point2D.Double src, Point2D.Double dst) {
		dst.x = src.x * xscale + xoffset;
		dst.y = src.y * yscale + yoffset;
		return dst;
	}

	public Point2D.Double inverseTransform(Point2D.Double src, Point2D.Double dst) {
		dst.x = (src.x - xoffset) / xscale;
		dst.y = (src.y - yoffset) / yscale;
		return dst;
	}

	public boolean equals(TransformI Tx) {
		return (Tx instanceof ConstrainLinearTrnsfm) &&
				super.equals(Tx) &&
				(constrain_value == ((ConstrainLinearTrnsfm)Tx).getConstrainValue());
	}

}
