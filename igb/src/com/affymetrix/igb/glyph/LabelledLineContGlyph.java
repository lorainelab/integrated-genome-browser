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

public class LabelledLineContGlyph extends ImprovedLineContGlyph {

  String label;
  boolean toggle_by_width = true;
  Font fnt;
  static final int pixel_separation = 3;
  int label_loc = NORTH;

  public void draw(ViewI view) {
    super.draw(view);
    if (label != null) {
      view.transformToPixels(coordbox, pixelbox);
      Graphics g = view.getGraphics();
      g.setFont(fnt);
      g.setColor(getBackgroundColor());
      FontMetrics fm = g.getFontMetrics();
      int text_width = fm.stringWidth(label);
      int text_height = fm.getAscent();
      if ((! toggle_by_width) || (text_width <= pixelbox.width)) {
	int xpos = pixelbox.x + (pixelbox.width/2) - (text_width/2);
	if (label_loc == NORTH) {
	  g.drawString(label, xpos, pixelbox.y - pixel_separation);
	}
	else if (label_loc == SOUTH) {
	  g.drawString(label, xpos, pixelbox.y + pixelbox.height + text_height + pixel_separation);
	}
      }
    }

  }

  public int getLabelLocation() { return label_loc; }
  public void setLabelLocation(int loc) { label_loc = loc; }
  public Font getFont() { return fnt; }
  public void setFont(Font f) { this.fnt = f; }
  public void setLabel(String str) { this.label = str; }
  public String getLabel() { return label; }

}
