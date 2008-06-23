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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A glyph that is drawn as a solid, outlined rectangle.
 */
public class EffOutlinedRectGlyph extends EffOutlineContGlyph  {
  Color bgcolor = Color.white;
  static boolean optimize_child_draw = true; //TODO: hides super-class field
  
  @Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);
    
    applyMinimumPixelBounds(pixelbox);
    
    Graphics g = view.getGraphics();
    g.setColor(getColor());
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    if (pixelbox.width > 2 && pixelbox.height > 2) {
      g.setColor(bgcolor);
      g.fillRect(pixelbox.x+1, pixelbox.y+1, pixelbox.width-2, pixelbox.height-2);
    }

    super.draw(view);
  }
  
  /** Sets the outline color; the fill color is automatically calculated as  
   *  a darker shade. 
   */
  @Override
  public void setColor(Color c) {
    super.setColor(c);
    bgcolor = c.darker();
  }
}
