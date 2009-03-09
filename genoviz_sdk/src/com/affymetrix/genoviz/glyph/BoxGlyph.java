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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;

/**
 * A glyph that contains children within an outline.
 *
 * This is convenient for representing data
 * that has multiple sub-ranges within its range.
 * e.g. genes which have a known intron/exon structure.
 */
public class BoxGlyph extends OutlineRectGlyph  {

	private int yy = 1;

	/**
	 * add a child glyph centering it vertically within this glyph.
	 *
	 * @param glyph the child.
	 */
	public void addChild(GlyphI glyph) {
		// child.cbox.y is modified, but not child.cbox.height
		Rectangle2D cbox = glyph.getCoordBox();
		double yPosition = this.coordbox.y + yy * this.coordbox.height / 3;
		if ( 1 == yy ) {
			yy = 2;
		}
		else {
			yy = 1;
		}
		cbox.y = yPosition - cbox.height/2;
		super.addChild(glyph);
	}

}
