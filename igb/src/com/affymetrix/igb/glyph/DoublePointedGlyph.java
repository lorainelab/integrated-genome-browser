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

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.NeoConstants;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * A glyph with a simple arrow-like point on each end.
 * It fills its containing pixelbox with the exception of the corners
 * on each "end" coord of the primary axis.
 */
public class DoublePointedGlyph extends EfficientSolidGlyph {

  int orientation = HORIZONTAL;
  
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);
    if (pixelbox.width == 0) { pixelbox.width = 1; }
    if (pixelbox.height == 0) { pixelbox.height = 1; }
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());
    int x[] = new int[6];
    int y[] = new int[6];
    int halfThickness = 1;

    if (HORIZONTAL == this.getOrientation()) {
      halfThickness = (pixelbox.height-1)/2;
      if (pixelbox.width > 2 && pixelbox.height > 2) {
        int midway = pixelbox.x + pixelbox.width/2;
        x[0] = Math.min(midway, pixelbox.x + halfThickness);
        x[2] = pixelbox.x + pixelbox.width;
        x[1] = Math.max(midway, x[2] - halfThickness);
        x[3] = x[1];
        x[4] = x[0];
        x[5] = pixelbox.x;
        y[0] = pixelbox.y;
        y[1] = y[0];
        y[2] = y[0] + halfThickness;
        y[3] = y[0] + pixelbox.height;
        y[4] = y[3];
        y[5] = y[2];
        g.fillPolygon(x, y, 6);
      } else {
        g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }
    else if (VERTICAL == this.getOrientation()) {
      halfThickness = (pixelbox.width-1)/2;
      if (pixelbox.height > 4) {
        int midway = pixelbox.y + pixelbox.height/2;
        y[0] = Math.min(midway, pixelbox.y + halfThickness);
        y[2] = pixelbox.y + pixelbox.height;
        y[1] = Math.max(midway, y[2] - halfThickness);
        y[3] = y[1];
        y[4] = y[0];
        y[5] = pixelbox.y;
        x[0] = pixelbox.x;
        x[1] = x[0];
        x[2] = x[0] + halfThickness;
        x[3] = x[0] + pixelbox.width;
        x[4] = x[3];
        x[5] = x[2];
        g.fillPolygon(x, y, 6);
      } else {
        g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }
  }
  
  /** Sets the orientation to one of {@link NeoConstants#HORIZONTAL} or
   *  {@link NeoConstants#VERTICAL}.
   *  @throws IllegalArgumentException if any other argument is given
   */
  public void setOrientation(int or) {
    if (or != HORIZONTAL && or != VERTICAL) throw new IllegalArgumentException();
    this.orientation = or;
  }
  
  public int getOrientation() {
    return this.orientation;
  }
}
