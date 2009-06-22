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
 * A transform used internally by some NeoWidgets to handle zooming, should
 *    not be used directly.
 *
 * ExponentialTransform is the start of replacing application handling
 *    of things such as zooming a map with scrollbars that can take
 *    both transforms and listeners and do the right thing
 *    Right now it only does about half the work involved in
 *    the "gradual deceleration" of the zoom scrollbar
 */
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

	public double transform(int orientation, double in) {
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

	public double inverseTransform(int orientation, double in) {
		double out = (Math.log(in)-lxmin) / ratio;
		return out;
	}

	public Point2D.Double transform(Point2D.Double src, Point2D.Double dst) {
		// y = f(x), but in this case y is really dst.x
		//   (Exponential is a one-dimensional transform, ignores src.y & dst.y
		double x = src.x;
		double y = Math.exp(x*ratio);
		dst.x = y;
		return dst;
	}

	public Point2D.Double inverseTransform(Point2D.Double src, Point2D.Double dst) {
		double y = src.x;
		double x = Math.log(y/ratio);
		dst.x = x;
		return dst;
	}

	public Rectangle2D.Double transform(Rectangle2D.Double src, Rectangle2D.Double dst) {
		double x = src.x;
		double y = Math.exp(x*ratio);
		dst.x = y;
		return dst;
	}

	public Rectangle2D.Double inverseTransform(Rectangle2D.Double src, Rectangle2D.Double dst) {
		double y = src.x;
		double x = Math.log(y/ratio);
		dst.x = x;
		return dst;
	}

	public void append(TransformI T) {  }
	public void prepend(TransformI T) { }

	/**
	 * creates a shallow copy.
	 * Since the only instance variables are doubles,
	 * it is also trivially a deep copy.
	 */
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	// not checking for transform value equality right now
	public boolean equals(TransformI Tx) {
		return super.equals(Tx);
	}

}
