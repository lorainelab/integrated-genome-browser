/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import java.awt.*;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.util.GeometryUtils;

/**
 *  A glyph that is drawn as a solid rectangle.
 */
public class ImprovedFillRectGlyph extends SolidGlyph  {

  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());

    EfficientGlyph.fixAWTBigRectBug(view, pixelbox);

    if (pixelbox.width < 1) { pixelbox.width = 1; }
    if (pixelbox.height < 1) { pixelbox.height = 1; }
    // draw the box
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }
}
