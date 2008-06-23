/**
*   Copyright (c) 2001-2008 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.glyph.efficient;

import java.awt.*;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A glyph that is drawn as a solid rectangle.
 */
public class EffFillRectGlyph extends EffSolidGlyph  {
  @Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);
    
    applyMinimumPixelBounds(pixelbox);
    
    // draw the box
    Graphics2D g = view.getGraphics();
    g.setColor(getBackgroundColor());
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }


}
