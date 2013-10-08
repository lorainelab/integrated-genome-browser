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

package com.affymetrix.genoviz.glyph;

import java.awt.Graphics;
import java.awt.Rectangle;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A glyph that is drawn as a solid, outlined rectangle.
 */
public final class EfficientOutlinedRectGlyph extends EfficientOutlineContGlyph  {
  //private Color bgcolor = Color.white;
  
	@Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this.getCoordBox(), pixelbox);
    
    pixelbox = fixAWTBigRectBug(view, pixelbox);
    
    pixelbox.width = Math.max ( pixelbox.width, getMinPixelsWidth() );
    pixelbox.height = Math.max ( pixelbox.height, getMinPixelsHeight() );
    
    Graphics g = view.getGraphics();
    g.setColor(getColor());
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
//    if (pixelbox.width > 3 && pixelbox.height > 3) {
//      g.setColor(bgcolor);
//      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
//    }
  }
  
	
//  /** Sets the outline color; the fill color is automatically calculated as  
//   *  a darker shade. 
//   */
//	@Override
//  public void setColor(Color c) {
//	super.setColor(c);
//	bgcolor = c.darker();
//  }
}
