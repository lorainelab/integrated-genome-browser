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

import java.awt.Graphics;
import java.awt.Rectangle;
import com.affymetrix.genoviz.bioviews.ViewI;

/**
 * A glyph that is drawn as a solid rectangle.
 */
public class FillRectGlyph extends SolidGlyph  {

	public void draw(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());

		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());

		// temp fix for AWT drawing bug when rect gets too big -- GAH 2/6/98
		Rectangle compbox = view.getComponentSizeRect();
		setPixelBox(getPixelBox().intersection(compbox));

		// If the coordbox was specified with negative width or height,
		// convert pixelbox to equivalent one with positive width and height.
		// Constrain abs(width) or abs(height) by min_pixels.
		// Here I'm relying on the fact that min_pixels is positive.
		if (getCoordBox().width < 0) {
			getPixelBox().width = -Math.min(getPixelBox().width, -getMinPixelsWidth());
			getPixelBox().x -= getPixelBox().width;
		}
		else {
			getPixelBox().width = Math.max ( getPixelBox().width, getMinPixelsWidth() );
		}
		if (getCoordBox().height < 0) {
			getPixelBox().height = -Math.min(getPixelBox().height, -getMinPixelsHeight());
			getPixelBox().y -= getPixelBox().height;
		}
		else {
			getPixelBox().height = Math.max ( getPixelBox().height, getMinPixelsHeight() );
		}

		// draw the box
		g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height);

		super.draw(view);
	}
}
