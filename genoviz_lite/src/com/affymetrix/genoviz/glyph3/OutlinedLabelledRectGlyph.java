/**
*   Copyright (c) 2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.glyph3;

import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LabelledGlyphI;
import java.awt.*;


/**
 * Adds an internal label string to solid rectangle glyph.
 */
public class OutlinedLabelledRectGlyph extends SolidGlyph implements LabelledGlyphI {
  CharSequence text = "";

  public void setLabel(CharSequence str) {
    this.text = str;
  }
  public CharSequence getLabel() {
    return this.text;
  }

  // Below this threshold it just assumest that text will not fit.
  public static final int min_width_needed_for_text = 32;

  @Override
  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    if (pixelbox.width <= 2) { pixelbox.width = 2; }
    if (pixelbox.height <= 2) { pixelbox.height = 2; }

    Graphics g = view.getGraphics();
    g.setColor(this.getForegroundColor());
    g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    
    
    if( getLabel() != null ) {
      if (pixelbox.width >= min_width_needed_for_text) {
        Font savefont = g.getFont();
        Font f2 = this.getFont();
        if (f2 != savefont) {
          g.setFont(f2);
        } else {
          // If they are equal, there's no need to restore the font
          // down below.
          savefont = null;
        }
        FontMetrics fm = g.getFontMetrics();
        int text_width = fm.stringWidth(getLabel().toString());

        int midline = pixelbox.y + pixelbox.height / 2;

        if(text_width <= pixelbox.width ) {
          int mid = pixelbox.x + ( pixelbox.width / 2 ) - ( text_width / 2 );
          // define adjust such that: ascent-adjust = descent+adjust
          int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0);
//          g.setColor(this.getForegroundColor());
          g.drawString(getLabel().toString(), mid, midline + adjust );
        }
        if (null != savefont) {
          g.setFont(savefont);
        }
      }
    }
  }
}
