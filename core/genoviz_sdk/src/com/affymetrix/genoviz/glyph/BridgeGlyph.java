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

import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Graphics;


/**
 * A glyph appropriate for disulfide bridge in peptide sequences.
 */
public class BridgeGlyph extends SolidGlyph {

	// Override this to put the surface of the bridge
	// at a different position.
	// Must be between 0 and 1 inclusive.
	protected static final double POS = 0.0f;

	public void draw(ViewI theView) {
		theView.transformToPixels(getCoordBox(), getPixelBox());
		if (0 == getPixelBox().width) {
			getPixelBox().width = 1;
		}
		if (0 == getPixelBox().height) {
			getPixelBox().height = 1;
		}
		Graphics g = theView.getGraphics();
		g.setColor(getBackgroundColor());
		g.fillRect(getPixelBox().x, getPixelBox().y+(int)Math.round(getPixelBox().height*POS),
				getPixelBox().width, 1);// surface
		g.fillRect(getPixelBox().x, getPixelBox().y, 1, getPixelBox().height); // left edge
		g.fillRect(getPixelBox().x+getPixelBox().width, getPixelBox().y, 1, getPixelBox().height); // right edge
		super.draw(theView);
	}

}
