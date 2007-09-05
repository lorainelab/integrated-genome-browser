/**
*   Copyright (c) 2007 Affymetrix, Inc.
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

import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LabelledGlyphI;
import java.awt.*;


/**
 * Adds an internal label string to solid rectangle glyph.
 */
public class OutlinedLabelledRectGlyph extends SolidGlyph implements LabelledGlyphI {
  String text = "";

  public void setText(String str) {
    this.text = str;
  }
  public String getText() {
    return this.text;
  }

  // Below this threshold it just assumest that text will not fit.
  public static final int min_width_needed_for_text = 32;

  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    if (pixelbox.width <= 2) { pixelbox.width = 2; }
    if (pixelbox.height <= 2) { pixelbox.height = 2; }

    Graphics g = view.getGraphics();
    g.setColor(this.getForegroundColor());
    g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    
    
    if( getText() != null ) {
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
        int text_width = fm.stringWidth(getText());

        int midline = pixelbox.y + pixelbox.height / 2;

        if(text_width <= pixelbox.width ) {
          int mid = pixelbox.x + ( pixelbox.width / 2 ) - ( text_width / 2 );
          // define adjust such that: ascent-adjust = descent+adjust
          int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0);
//          g.setColor(this.getForegroundColor());
          g.drawString(getText(), mid, midline + adjust );
        }
        if (null != savefont) {
          g.setFont(savefont);
        }
      }
    }
  }
}
