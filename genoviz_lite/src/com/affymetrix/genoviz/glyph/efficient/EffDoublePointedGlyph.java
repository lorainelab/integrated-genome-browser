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

package com.affymetrix.genoviz.glyph.efficient;

import com.affymetrix.genoviz.bioviews.ViewI;

import com.affymetrix.genoviz.util.NeoConstants.Orientation;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * A glyph with a simple arrow-like point on each end.
 * It fills its containing pixelbox with the exception of the corners
 * on each "end" coord of the primary axis.
 */
public class EffDoublePointedGlyph extends EffSolidGlyph {

  Orientation orientation = Orientation.Horizontal;
  
    @Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);
    
    applyMinimumPixelBounds(pixelbox);

    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());
    int xx[] = new int[6];
    int yy[] = new int[6];
    int halfThickness = 1;

    if (Orientation.Horizontal == getOrientation()) {
      halfThickness = (pixelbox.height-1)/2;
      if (pixelbox.width > 2 && pixelbox.height > 2) {
        int midway = pixelbox.x + pixelbox.width/2;
        xx[0] = Math.min(midway, pixelbox.x + halfThickness);
        xx[2] = pixelbox.x + pixelbox.width;
        xx[1] = Math.max(midway, xx[2] - halfThickness);
        xx[3] = xx[1];
        xx[4] = xx[0];
        xx[5] = pixelbox.x;
        yy[0] = pixelbox.y;
        yy[1] = yy[0];
        yy[2] = yy[0] + halfThickness;
        yy[3] = yy[0] + pixelbox.height;
        yy[4] = yy[3];
        yy[5] = yy[2];
        g.fillPolygon(xx, yy, 6);
      } else {
        g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }
    else if (Orientation.Vertical == getOrientation()) {
      halfThickness = (pixelbox.width-1)/2;
      if (pixelbox.height > 4) {
        int midway = pixelbox.y + pixelbox.height/2;
        yy[0] = Math.min(midway, pixelbox.y + halfThickness);
        yy[2] = pixelbox.y + pixelbox.height;
        yy[1] = Math.max(midway, yy[2] - halfThickness);
        yy[3] = yy[1];
        yy[4] = yy[0];
        yy[5] = pixelbox.y;
        xx[0] = pixelbox.x;
        xx[1] = xx[0];
        xx[2] = xx[0] + halfThickness;
        xx[3] = xx[0] + pixelbox.width;
        xx[4] = xx[3];
        xx[5] = xx[2];
        g.fillPolygon(xx, yy, 6);
      } else {
        g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }
  }
  
  public void setOrientation(Orientation or) {
    this.orientation = or;
  }
  
  public Orientation getOrientation() {
    return orientation;
  }
}
