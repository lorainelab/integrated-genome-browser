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

package com.affymetrix.genoviz.widget.neoqualler;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class QualityRect extends Glyph
{
	@Override
	public void draw(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());
		// shrinks slightly to differentiate from possible neighbors

		Graphics g = view.getGraphics();

		if (getPixelBox().height == 0) { getPixelBox().height = 1; }

		if (getPixelBox().width >=1 ) {
			if (getPixelBox().width > 2) {
				getPixelBox().x += 1;
				getPixelBox().width -=2;
			}
			else if (getPixelBox().width == 2) {
				getPixelBox().x++;
				getPixelBox().width--;
			}
			g.setColor(getBackgroundColor());
			g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height);
		}
		else {
			g.setColor(getBackgroundColor());
			g.fillRect(getPixelBox().x, getPixelBox().y, 2, 2);
		}
		super.draw(view);
	}

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return  pixel_hitbox.intersects(getPixelBox());
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return coord_hitbox.intersects(getCoordBox());
	}

}
