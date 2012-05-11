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

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.awt.geom.Rectangle2D;

/**
 *  A glyph that paints a specified Image when it draws.
 *  BasicImageGlyph is still somewhat experimental.
 *  In particular hit detection needs improvement.
 */
public class BasicImageGlyph extends Glyph {

	protected Image img;
	protected int img_width, img_height;

	/**
	 * indicates how the image should fill the pixel box.
	 */
	protected int primaryFill = NONE;
	protected int secondaryFill = NONE;
	public static final int NONE = 0;
	public static final int STRETCHED = 1;
	public static final int TILED = 2;
	// possible future values: BRICKED.


	/**
	 * sets the fill technique for the glyph
	 * along the primary axis.
	 *
	 * @param theTechnique one of NONE, STRETCHED, or TILED.
	 */
	public void setPrimaryFill(int theTechnique) {
		switch (theTechnique) {
			case NONE:
			case STRETCHED:
			case TILED:
				this.primaryFill = theTechnique;
				break;
			default:
				throw new RuntimeException("technique must be NONE, STRETCHED, or TILED");
		}
	}

	/**
	 * gets the fill technique for the glyph
	 * along the primary axis.
	 *
	 * @return NONE, STRETCHED, or TILED.
	 */
	public int getPrimaryFill() {
		return this.primaryFill;
	}

	/**
	 * sets the fill technique for the glyph
	 * along the secondary axis.
	 *
	 * @param theTechnique one of NONE, STRETCHED, or TILED.
	 */
	public void setSecondaryFill(int theTechnique) {
		switch (theTechnique) {
			case NONE:
			case STRETCHED:
			case TILED:
				this.secondaryFill = theTechnique;
				break;
			default:
				throw new RuntimeException("technique must be NONE or STRETCHED");
		}
	}

	/**
	 * gets the fill technique for the glyph
	 * along the secondary axis.
	 *
	 * @return NONE, STRETCHED, or TILED.
	 */
	public int getSecondaryFill() {
		return this.secondaryFill;
	}


	public void draw(ViewI view) {

		ImageObserver observer = view.getComponent();
		if (this.img_width == -1 || this.img_height == -1) {
			this.img_width = this.img.getWidth(observer);
			this.img_height = this.img.getHeight(observer);
		}

		calcPixels(view);

		Graphics g = view.getGraphics();
		if (TILED == this.primaryFill || TILED == this.secondaryFill) {
			Image i = ((Component)observer).createImage(
					this.getPixelBox().width, this.getPixelBox().height);
			Graphics g2 = i.getGraphics();
			g2.drawImage(img, 0, 0, this.img_width, this.img_height, observer);
			if (TILED == this.primaryFill) {
				for (int xd = this.img_width; xd < this.getPixelBox().width;
						xd += this.img_width) {
					g2.copyArea(0, 0, this.img_width, this.img_height, xd, 0);
						}
			}
			if (TILED == this.secondaryFill) {
				for (int yd = this.img_height; yd < this.getPixelBox().height;
						yd += this.img_height) {
					g2.copyArea(0, 0, this.getPixelBox().width, this.img_height, 0, yd);
						}
			}
			g.drawImage(i, this.getPixelBox().x, this.getPixelBox().y, observer);
		} else {
			g.drawImage(img, this.getPixelBox().x, this.getPixelBox().y,
					this.getPixelBox().width, this.getPixelBox().height, observer);
		}

	}


	/**
	 * sets the Image to display in the glyph.
	 *
	 * @param img the image to display in the glyph.
	 * @param observer an ImageObserver to oversee image methods.
	 */
	public void setImage(Image img, ImageObserver observer) {
		((Component)observer).prepareImage(img, observer);
		this.img = img;
		img_width = this.img.getWidth(observer);
		img_height = this.img.getHeight(observer);
	}

	/**
	 * calculates the pixelbox for this glyph.
	 *
	 */
	public void calcPixels (ViewI view)  {

		super.calcPixels(view);

		ImageObserver observer = view.getComponent();
		if (this.img_width == -1 || this.img_height == -1) {
			this.img_width = this.img.getWidth(observer);
			this.img_height = this.img.getHeight(observer);
		}

		if (NONE == this.primaryFill) {
			// Center the image.
			getPixelBox().x = getPixelBox().x + getPixelBox().width/2 - this.img_width/2;
			getPixelBox().width = this.img_width;
		}
		if (NONE == this.secondaryFill) {
			// Center the image.
			getPixelBox().y = getPixelBox().y + getPixelBox().height/2 - this.img_height/2;
			getPixelBox().height = this.img_height;
		}

	}

	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return  isVisible() && pixel_hitbox.intersects(getPixelBox());
	}

	// this isn't going to be very accurate -- really need to back-calculate
	// apparent coord box based on center coord, img_width, img_height
	// GAH  3-26-98
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return isVisible() && coord_hitbox.intersects(getCoordBox());
	}


}
