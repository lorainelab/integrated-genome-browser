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

import com.affymetrix.genoviz.glyph.LabelledGlyph2;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;

import com.affymetrix.genoviz.util.NeoConstants.Placement;

public class LabelledLineContGlyph extends ImprovedLineContGlyph implements LabelledGlyph2 {

  CharSequence label;
  boolean toggle_by_width = true;
  Font fnt;
  static final int pixel_separation = 3;
  Placement label_loc = Placement.ABOVE;
  boolean showLabel = true;

  @Override
  public void draw(ViewI view) {
    super.draw(view);
    if (label != null && showLabel) {
      view.transformToPixels(coordbox, pixelbox);
      Graphics g = view.getGraphics();
      g.setFont(fnt);
      g.setColor(getBackgroundColor());
      FontMetrics fm = g.getFontMetrics();
      int text_width = fm.stringWidth(label.toString());
      int text_height = fm.getAscent();
      if ((! toggle_by_width) || (text_width <= pixelbox.width)) {
	int xpos = pixelbox.x + (pixelbox.width/2) - (text_width/2);
	if (Placement.ABOVE.equals(label_loc)) {
	  g.drawString(label.toString(), xpos, pixelbox.y - pixel_separation);
	}
	else if (Placement.BELOW.equals(label_loc)) {
	  g.drawString(label.toString(), xpos, pixelbox.y + pixelbox.height + text_height + pixel_separation);
	}
      }
    }

  }

  public Placement getLabelLocation() { return label_loc; }
  public void setLabelLocation(Placement loc) { label_loc = loc; }

  @Override public Font getFont() { return fnt; }
  @Override public void setFont(Font f) { this.fnt = f; }

  public void setLabel(CharSequence str) { this.label = str; }
  public CharSequence getLabel() { return label; }

  public boolean getShowLabel() {
    return showLabel;
  }

  public void setShowLabel(boolean b) {
    showLabel = b;
  }
}
