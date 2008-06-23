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

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;

/**
 *  A glyph that is drawn as a solid triangle.
 */
public class TriangleGlyph extends DirectedGlyph  {
  int x[];
  int y[];
  Polygon poly;

  public TriangleGlyph() {
    super();
    x = new int[3];
    y = new int[3];
    poly = new Polygon(x, y, 3);
  }

  public void draw(ViewI view) {
    calcPixels(view);
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());
    g.fillPolygon(x, y, 3);
    super.draw(view);
  }

  /**
   * makes this glyph "point" 90 degrees (rotated clockwise) from its "direction".
   */
  public void calcPixels(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    x = poly.xpoints;
    y = poly.ypoints;
    int xcenter = pixelbox.x + pixelbox.width/2;
    int ycenter = pixelbox.y + pixelbox.height/2;

    switch ( this.getDirection() ) {
    case EAST:
      x[0] = xcenter - 5;
      y[0] = ycenter - 5;
      x[1] = xcenter;
      y[1] = ycenter + 5;
      x[2] = xcenter + 5;
      y[2] = ycenter - 5;
      break;
    case WEST:
      x[0] = xcenter - 5;
      y[0] = ycenter + 5;
      x[1] = xcenter;
      y[1] = ycenter - 5;
      x[2] = xcenter + 5;
      y[2] = ycenter + 5;
      break;
    case SOUTH:
      x[0] = xcenter + 5;
      y[0] = ycenter - 5;
      x[1] = xcenter + 5;
      y[1] = ycenter + 5;
      x[2] = xcenter - 5;
      y[2] = ycenter;
      break;
    case NORTH:
      x[0] = xcenter - 5;
      y[0] = ycenter - 5;
      x[1] = xcenter - 5;
      y[1] = ycenter + 5;
      x[2] = xcenter + 5;
      y[2] = ycenter;
      break;
    }
  }

}
