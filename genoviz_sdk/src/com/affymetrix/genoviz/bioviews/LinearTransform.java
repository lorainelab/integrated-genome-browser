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

import java.awt.*;

/**
 * Also see interface TransformI for more documentation.
 */
public class LinearTransform implements TransformI  {
	protected double xscale, yscale, xoffset, yoffset;


	/** 
	 * Constructs a new LinearTransform
	 * with X and Y scales set at 1
	 * and offsets of 0.
	 */
	public LinearTransform() {
		xscale = yscale = 1.0f;
		xoffset = yoffset = 0.0f;
	}

	/**
	 * Creates a new transform with the same scales and offsets
	 * as the LinearTransform passed in.
	 */
	public LinearTransform(LinearTransform LT) {
		xscale = LT.getScaleX();
		yscale = LT.getScaleY();
		xoffset = LT.getOffsetX();
		yoffset = LT.getOffsetY();
	}

	/**
	 * Sets the base transform to linearly map coordinate space -- usually in a Scene --
	 * bounded by coord_box to pixel space -- used by the view of the Scene -- bounded
	 * by this.pixel_box.  Should be able to "fit" a glyph hierarchy into the pixel_box
	 * by calling this with the top glyph's coord_box
	 */
	/*
	   public LinearTransform(Rectangle2D coord_box, Rectangle pixel_box)  {
	   xscale = (double)pixel_box.width / coord_box.width;
	   yscale = (double)pixel_box.height / coord_box.height;
	   xoffset = (double)pixel_box.x - xscale * coord_box.x;
	   yoffset = (double)pixel_box.y - yscale * coord_box.y;
	   }*/

	public void copyTransform(LinearTransform LT) {
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
	public void fit(Rectangle2D coord_box, Rectangle pixel_box)  {
		xscale = (double)pixel_box.width / coord_box.width;
		yscale = (double)pixel_box.height / coord_box.height;
		xoffset = (double)pixel_box.x - xscale * coord_box.x;
		yoffset = (double)pixel_box.y - yscale * coord_box.y;
	}

	/**
	 * Transforms the coordinate on the axis indicated.
	 * If transform is being used in between a scene and a view,
	 * this would convert from scene coordinates to view/pixel coordinates.
	 * @param orientation {@link #X} or {@link #Y}
	 * @param in   the coordinate
	 */
	public double transform(int orientation, double in) {
		double out = 0;
		if (orientation == X) {
			out = in * xscale + xoffset;
		} else if (orientation == Y) {
			out = in * yscale + yoffset;
		}
		return out;
	}

	/**
	 * Transforms the coordinate inversely on the axis indicated.
	 * If transform is being used in between a scene and a view,
	 * this would convert from  view/pixel coordinates to Scene coordinates.
	 * @param orientation X or Y
	 * @param in the coordinate
	 */
	public double inverseTransform(int orientation, double in) {
		double out = 0;
		if (orientation == X) {
			out = (in - xoffset) / xscale;
		} else if (orientation == Y) {
			out = (in - yoffset) / yscale;
		}
		return out;
	}

	/** 
	 * Sets the scale of the transform directly.
	 * @param x X scale
	 * @param y Y scale
	 */
	public void setScale(double x, double y) {
		xscale = x;
		yscale = y;
	}

	/**
	 * Sets the offsets directly.
	 * @param x X offset
	 * @param y Y offset
	 */
	public void setTranslation(double x, double y) {
		xoffset = x;
		yoffset = y;
	}

	/**
	 * Transforms the source rectangle.
	 * @param src the Rectangle2D to be transformed.
	 * @param dst ignored
	 * @return the Souce rectangle transformed.
	 */
	public Rectangle2D transform(Rectangle2D src, Rectangle2D dst) {
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
	 * Transforms the source rectangle inversely.
	 * @param src the Rectangle2D to be transformed.
	 * @param dst ignored
	 * @return the souce rectangle transformed.
	 */
	public Rectangle2D inverseTransform(Rectangle2D src, Rectangle2D dst) {
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
	 * Transforms the Point2D.
	 * @param src the Point2D to be transformed.
	 * @param dst ignored
	 * @return the souce Point2D transformed.
	 */
	public Point2D transform(Point2D src, Point2D dst) {
		dst.x = src.x * xscale + xoffset;
		dst.y = src.y * yscale + yoffset;
		return dst;
	}

	/**
	 * Inversely transforms the Point2D.
	 * @param src the Point2D to be transformed.
	 * @param dst ignored
	 * @return the souce Point2D transformed.
	 */
	public Point2D inverseTransform(Point2D src, Point2D dst) {
		dst.x = (src.x - xoffset) / xscale;
		dst.y = (src.y - yoffset) / yscale;
		return dst;
	}

	/* Why not put these in a LinearTransformI interface? */

	public void append(TransformI T) {
		// MUST CHANGE SOON to throw IncompatibleTransformException
		if (! (T instanceof LinearTransform)) { return; }
		else {
			LinearTransform LT = (LinearTransform)T;
			xoffset = LT.xscale * xoffset + LT.xoffset;
			yoffset = LT.yscale * yoffset + LT.yoffset;
			xscale = xscale * LT.xscale;
			yscale = yscale * LT.yscale;
		}
	}

	public void prepend(TransformI T) {
		// MUST CHANGE SOON to throw IncompatibleTransformException
		if (! (T instanceof LinearTransform)) { return; }
		else {
			LinearTransform LT = (LinearTransform)T;
			xoffset = xscale * LT.xoffset + xoffset;
			yoffset = yscale * LT.yoffset + yoffset;
			xscale = xscale * LT.xscale;
			yscale = yscale * LT.yscale;
		}
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
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String toString() {
		return ("LinearTransform:  xscale = " + xscale + ", xoffset = " +
				xoffset + ",  yscale = " + yscale + ", yoffset " + yoffset);
	}

	public boolean equals(TransformI Tx) {
		if (Tx instanceof LinearTransform) {
			LinearTransform lint = (LinearTransform)Tx;
			return (xscale == lint.getScaleX() &&
					yscale == lint.getScaleY() &&
					xoffset == lint.getOffsetX() &&
					yoffset == lint.getOffsetY());
		}
		else {
			return false;
		}
	}

}
