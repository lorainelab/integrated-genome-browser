/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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
import com.affymetrix.genoviz.bioviews.*;

/**
 * Just like FillRectGlyph, but extends out an extra pixel on left side.
 * This can help with problems in doubleing point calculation of glyph edges.
 */
public class OverFillRect extends SolidGlyph {
  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    if (pixelbox.width == 0) { pixelbox.width = 1; }
    if (pixelbox.height == 0) { pixelbox.height = 1; }
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());
    g.fillRect(pixelbox.x-1, pixelbox.y, pixelbox.width+1, pixelbox.height);
    super.draw(view);
  }
}
