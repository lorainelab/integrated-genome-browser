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

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

public final class LabelledRectGlyph extends FillRectGlyph {
  private String label;
  private Font fnt;

  public LabelledRectGlyph()  {
    super();
  }

  public void draw(ViewI view) {
    super.draw(view);
    if (label != null) {
      view.transformToPixels(coordbox, pixelbox);
      Graphics g = view.getGraphics();
      if (fnt != null) {g.setFont(fnt);}
      g.setColor(getBackgroundColor());
      FontMetrics fm = g.getFontMetrics();
      int text_width = fm.stringWidth(label);
      if (text_width <= pixelbox.width) {
	int xpos = pixelbox.x + (pixelbox.width/2) - (text_width/2);
	g.drawString(label, xpos, pixelbox.y);
      }
    }

  }

	@Override
  public Font getFont() { return fnt; }
	@Override
  public void setFont(Font f) { this.fnt = f; }
  public void setLabel(String str) { this.label = str; }
  public String getLabel() { return label; }

}
