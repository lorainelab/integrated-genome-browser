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

package com.affymetrix.genoviz.glyph3;

import java.awt.*;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;

/**
 *  A glyph that is drawn as a solid rectangle.
 */
//TODO: Delete this class.  It is not an efficient glyph
public class ImprovedFillRectGlyph extends SolidGlyph  {

  @Override
  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());
    //g.setColor(color);
    
    if (pixelbox.width < 1) { pixelbox.width = 1; }
    if (pixelbox.height < 1) { pixelbox.height = 1; }
    // draw the box
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }
}
