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

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A useful glyph for representing a point feature with bounded uncertainty.
 */
public class BoundedPointGlyph extends SolidGlyph  {

	public void draw(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());
		if (getPixelBox().width == 0) { getPixelBox().width = 1; }
		if (getPixelBox().height == 0) { getPixelBox().height = 1; }
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());
		g.fillRect(getPixelBox().x, getPixelBox().y+getPixelBox().height/2, getPixelBox().width, 1);
		g.fillRect(getPixelBox().x+getPixelBox().width/2-1, getPixelBox().y, 3, getPixelBox().height);
		g.fillRect(getPixelBox().x, getPixelBox().y, 1, getPixelBox().height);
		g.fillRect(getPixelBox().x+getPixelBox().width, getPixelBox().y, 1, getPixelBox().height);
		super.draw(view);
	}

}
