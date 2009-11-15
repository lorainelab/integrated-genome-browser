package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

/**
 * Also see interface TransformI for more documentation.
 */
public class LinearTransform extends AffineTransform  {
	private double xscale, yscale, xoffset, yoffset;


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
	public void copyTransform(LinearTransform LT) {
		xscale = LT.getScaleX();
		yscale = LT.getScaleY();
		xoffset = LT.getTranslateX();
		yoffset = LT.getTranslateY();
	}

	/**
	 * Sets the transform's scales and offsets such that the coord_box's space is
	 * mapped to the pixel_box's space.  For example, to map a whole Scene to a 
	 * view, with no zooming, the coord_box would be the coordinate bounds of
	 * the Scene, and the pixel_box the size of the NeoCanvas holding the View.
	 * @param coord_box the coordinates of the Scene
	 * @param pixel_box coordinates of the pixel space to which you are mapping.
	 */
	public void fit(Rectangle2D.Double coord_box, Rectangle pixel_box)  {
    fit(coord_box, pixel_box, true, true);
  }

  /**
   * Sets the transform's scales and offsets such that the coord_box's space is
   * mapped to the pixel_box's space.  For example, to map a whole Scene to a
   * view, with no zooming, the coord_box would be the coordinate bounds of
   * the Scene, and the pixel_box the size of the NeoCanvas holding the View.
   * @param coord_box the coordinates of the Scene
   * @param pixel_box coordinates of the pixel space to which you are mapping.
   * @param fitx whether to perform a fit in the x axis
   * @param fity whether to perform a fit in the y axis
   */
  public void fit(Rectangle2D.Double coord_box, Rectangle pixel_box, boolean fitx, boolean fity)  {
    if (fitx) {
  		xscale = (double)pixel_box.width / coord_box.width;
	  	xoffset = (double)pixel_box.x - xscale * coord_box.x;
    }
    if (fity) {
  		yscale = (double)pixel_box.height / coord_box.height;
	  	yoffset = (double)pixel_box.y - yscale * coord_box.y;
    }
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
		if (orientation == NeoConstants.HORIZONTAL) {
			out = in * xscale + xoffset;
		} else if (orientation == NeoConstants.VERTICAL) {
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
		if (orientation == NeoConstants.HORIZONTAL) {
			out = (in - xoffset) / xscale;
		} else if (orientation == NeoConstants.VERTICAL) {
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
	 * @param src the Rectangle2D.Double to be transformed.
	 * @param dst ignored
	 * @return the Souce rectangle transformed.
	 */
	public Rectangle2D.Double transform(Rectangle2D.Double src, Rectangle2D.Double dst) {
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
	 * @param src the Rectangle2D.Double to be transformed.
	 * @param dst ignored
	 * @return the souce rectangle transformed.
	 */
	public Rectangle2D.Double inverseTransform(Rectangle2D.Double src, Rectangle2D.Double dst) {
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
		dst.setLocation(src.getX() * xscale + xoffset, src.getY() * yscale + yoffset);
		return dst;
	}

	/**
	 * Inversely transforms the Point2D.
	 * @param src the Point2D to be transformed.
	 * @param dst ignored
	 * @return the souce Point2D transformed.
	 */
	public Point2D inverseTransform(Point2D src, Point2D dst) {
		dst.setLocation((src.getX() - xoffset) / xscale, (src.getY() - yoffset) / yscale);
		return dst;
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

	public double getTranslateX() {
		return xoffset;
	}

	public double getTranslateY() {
		return yoffset;
	}

	public void setTranslateX(double offset) {
		xoffset = offset;
	}

	public void setTranslateY(double offset) {
		yoffset = offset;
	}

	public boolean equals(LinearTransform lint) {
		if (lint == null) {
			return false;
		}
		return (xscale == lint.getScaleX() &&
				yscale == lint.getScaleY() &&
				xoffset == lint.getTranslateX() &&
				yoffset == lint.getTranslateY());
	}

}
