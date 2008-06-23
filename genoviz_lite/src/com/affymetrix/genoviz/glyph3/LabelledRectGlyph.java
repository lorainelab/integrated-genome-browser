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

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

public class LabelledRectGlyph extends FillRectGlyph {
  CharSequence label;
  boolean toggle_by_width = true;
  Font fnt;

  public LabelledRectGlyph()  {
    super();
  }

  @Override
  public void draw(ViewI view) {
    super.draw(view);
    if (label != null) {
      view.transformToPixels(coordbox, pixelbox);
      Graphics g = view.getGraphics();
      if (fnt != null) {g.setFont(fnt);}
      g.setColor(getBackgroundColor());
      FontMetrics fm = g.getFontMetrics();
      int text_width = fm.stringWidth(label.toString());
      int text_height = fm.getAscent();
      if ((! toggle_by_width) || (text_width <= pixelbox.width)) {
	int xpos = pixelbox.x + (pixelbox.width/2) - (text_width/2);
	g.drawString(label.toString(), xpos, pixelbox.y);
      }
    }

  }

  @Override
  public Font getFont() { return fnt; }

  @Override
  public void setFont(Font f) { this.fnt = f; }

  public void setLabel(CharSequence str) { this.label = str; }
  public CharSequence getLabel() { return label; }

}
