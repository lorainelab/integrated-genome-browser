/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

/**
 *  A glyph that is drawn as a solid rectangle.
 */
public class EfficientFillRectGlyph extends EfficientSolidGlyph  {

  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());
    
    fixAWTBigRectBug(view, pixelbox);
    
    pixelbox.width = Math.max ( pixelbox.width, min_pixels_width );
    pixelbox.height = Math.max ( pixelbox.height, min_pixels_height );
    
    // draw the box
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }
}
