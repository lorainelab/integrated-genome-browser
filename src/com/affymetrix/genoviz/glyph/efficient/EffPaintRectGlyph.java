/**
*   Copyright (c) 2007-2008 Affymetrix, Inc.
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
 *  A glyph that is drawn as a painted rectangle.
 */
public class EffPaintRectGlyph extends EffSolidGlyph  {
    
  Paint paint = new GradientPaint(0, 0, Color.GREEN, 5, 2, Color.YELLOW, true);
  
  public void setPaint(Paint p) {
    this.paint = p;
  }
  
  @Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);

    Graphics2D g = view.getGraphics();
    
    applyMinimumPixelBounds(pixelbox);
    
    // draw the box
    g.setPaint(paint);
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }


}
