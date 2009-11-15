package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class LinearTransform extends AffineTransform  {
	public LinearTransform() {
		super();
	}

	/**
	 * Creates a new transform with the same scales and offsets
	 * as the LinearTransform passed in.
	 */
	public void copyTransform(LinearTransform LT) {
		this.setTransform(LT);
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
		this.setTransform((double)pixel_box.width / coord_box.width, 0, 0,
				this.getScaleY(), (double)pixel_box.x - this.getScaleX() * coord_box.x, this.getTranslateY());
    }
    if (fity) {
		this.setTransform(this.getScaleX(), 0, 0,
				(double)pixel_box.height / coord_box.height, this.getTranslateX(), (double)pixel_box.y - this.getScaleY() * coord_box.y);
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
			out = in * this.getScaleX() + this.getTranslateX();
		} else if (orientation == NeoConstants.VERTICAL) {
			out = in * this.getScaleY() + this.getTranslateY();
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
			out = (in - this.getTranslateX()) / this.getScaleX();
		} else if (orientation == NeoConstants.VERTICAL) {
			out = (in - this.getTranslateY()) / this.getScaleY();
		}
		return out;
	}

	/** 
	 * Sets the scale of the transform directly.
	 * @param x X scale
	 * @param y Y scale
	 */
	public void setScale(double x, double y) {
		this.setTransform(x,0,0,y,this.getTranslateX(),this.getTranslateY());
	}

	/**
	 * Sets the offsets directly.
	 * @param x X offset
	 * @param y Y offset
	 */
	public void setTranslation(double x, double y) {
		this.setTransform(this.getScaleX(), 0, 0, this.getScaleY(), x, y);
	}

	/**
	 * Transforms the source rectangle.
	 * @param src the Rectangle2D.Double to be transformed.
	 * @param dst ignored
	 * @return the Souce rectangle transformed.
	 */
	public Rectangle2D.Double transform(Rectangle2D.Double src, Rectangle2D.Double dst) {
		dst.x = src.x * this.getScaleX() + this.getTranslateX();
		dst.y = src.y * this.getScaleY() + this.getTranslateY();
		dst.width = src.width * this.getScaleX();
		dst.height = src.height * this.getScaleY();
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
		dst.x = (src.x - this.getTranslateX()) / this.getScaleX();
		dst.y = (src.y - this.getTranslateY()) / this.getScaleY();
		dst.width = src.width / this.getScaleX();
		dst.height = src.height / this.getScaleY();

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

	public void setScaleX(double scale) {
		this.setTransform(scale,0,0,this.getScaleY(),this.getTranslateX(),this.getTranslateY());
	}

	public void setScaleY(double scale) {
		this.setTransform(this.getScaleX(),0,0,scale,this.getTranslateX(),this.getTranslateY());
	}

	public void setTranslateX(double offset) {
		this.setTransform(this.getScaleX(), 0, 0, this.getScaleY(), offset, this.getTranslateY());
	}

	public void setTranslateY(double offset) {
		this.setTransform(this.getScaleX(), 0, 0, this.getScaleY(), this.getTranslateX(), offset);
	}

	public boolean equals(LinearTransform lint) {
		if (lint == null) {
			return false;
		}
		return super.equals(lint);
	}

}
