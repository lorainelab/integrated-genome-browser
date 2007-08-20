/////////////////////////////////////////////////////////////////////
// (C) Copyright 2007, Affymetrix, Inc.
// All rights reserved. Confidential. Except as pursuant
// to a written agreement with Affymetrix, this software may
// not be used or distributed. This software may be covered
// by one or more patents.
//
// "GeneChip", "Affymetrix" and the Affymetrix logos, and
// Affymetrix user interfaces are trademarks used by Affymetrix.
//////////////////////////////////////////////////////////////////////*

package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Graphics;
import java.awt.Rectangle;

public class XGlyph extends EfficientOutlineContGlyph {
  
  /** Creates a new instance of XGlyph */
  public XGlyph() {
  }

  public void draw(ViewI view) {
    super.draw(view);

    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);
    if (0 == pixelbox.width || 0 == pixelbox.height) {
      return;
    }
    Graphics g = view.getGraphics();
    
    pixelbox = fixAWTBigRectBug(view, pixelbox);
    
    g.drawLine(pixelbox.x, pixelbox.y, pixelbox.x + pixelbox.width, pixelbox.y + pixelbox.height);
    g.drawLine(pixelbox.x + pixelbox.width, pixelbox.y, pixelbox.x, pixelbox.y + pixelbox.height);
  }
  
}
