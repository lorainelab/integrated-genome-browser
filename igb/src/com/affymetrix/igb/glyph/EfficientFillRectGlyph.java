package com.affymetrix.igb.glyph;

import java.awt.*;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A glyph that is drawn as a solid rectangle.
 */
public final class EfficientFillRectGlyph extends EfficientSolidGlyph  {
	@Override
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
