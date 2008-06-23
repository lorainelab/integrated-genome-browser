/**
*   Copyright (c) 1998-2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genoviz.glyph;

import java.awt.*;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 * A solid rectangle glyph with an internal label.
 */
public class LabelledRectGlyph extends FillRectGlyph implements LabelledGlyphI {
  CharSequence text;

  public void setLabel(CharSequence str) {
    this.text = str;
  }

  public CharSequence getLabel() {
    return this.text;
  }

  // CLH: This is the constant that the glyph uses to decide
  //      if it should even bother checking to see if the label
  //      will fit. Below this threshold it just assumes that
  //      it will not fit.
  public static final int min_width_needed_for_text = 32;

  @Override
  public void draw(ViewI view) {
    super.draw( view );
    if( getLabel() != null ) {
      Graphics g = view.getGraphics();

      // CLH: Added a check to make sure there is at least _some_ room
      // before we start getting setting the font and checking metrics.
      // No need to do this on a 1 px wide rectangle!

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
        int text_width = fm.stringWidth(this.text.toString());
        int text_height = fm.getAscent(); // this is just approximate for the height

        int midline = pixelbox.y + pixelbox.height / 2;

        if(text_width <= pixelbox.width && text_height <= pixelbox.height) {
          int mid = pixelbox.x + ( pixelbox.width / 2 ) - ( text_width / 2 );
          // define adjust such that: ascent-adjust = descent+adjust
          int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0);
          g.setColor(this.getForegroundColor());
          g.drawString(this.text.toString(), mid, midline + adjust );
        }
        if (null != savefont) {
          g.setFont(savefont);
        }
      }
    }
  }
}
