package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class LinearTransform extends AffineTransform  {
	public LinearTransform() {
		super();
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
	 * Transforms the source rectangle.
	 * @param src the Rectangle2D.Double to be transformed.
	 * @param dst ignored
	 * @return the Souce rectangle transformed.
	 */
	public final Rectangle2D.Double transform(Rectangle2D.Double src, Rectangle2D.Double dst) {
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
	public static final Rectangle2D.Double inverseTransform(AffineTransform t, Rectangle2D.Double src, Rectangle2D.Double dst) {
		dst.x = (src.x - t.getTranslateX()) / t.getScaleX();
		dst.y = (src.y - t.getTranslateY()) / t.getScaleY();
		dst.width = src.width / t.getScaleX();
		dst.height = src.height / t.getScaleY();

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

	public static final void setScaleX(AffineTransform t, double scale) {
		t.setTransform(scale,0,0,t.getScaleY(),t.getTranslateX(),t.getTranslateY());
	}

	public static final void setScaleY(AffineTransform t, double scale) {
		t.setTransform(t.getScaleX(),0,0,scale,t.getTranslateX(),t.getTranslateY());
	}

	public static final void setTranslateX(AffineTransform t, double offset) {
		t.setTransform(t.getScaleX(), 0, 0, t.getScaleY(), offset, t.getTranslateY());
	}

	public static final void setTranslateY(AffineTransform t, double offset) {
		t.setTransform(t.getScaleX(), 0, 0, t.getScaleY(), t.getTranslateX(), offset);
	}

	public boolean equals(LinearTransform lint) {
		if (lint == null) {
			return false;
		}
		return super.equals(lint);
	}

}
