/**
*   Copyright (c) 2007-2008 Affymetrix, Inc.
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
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

/**
 * A glyph that is drawn as a outlined rounded rectangle with a transparent
 * center in the shape of a rounded rectangle, through which the 
 * children can be seen.  Outside of the rounded rectangle, all children
 * are clipped (and thus invisible).  This can be used, for instance, as
 * a glyph representing one arm of a chromosome, with the cytobands
 * as children.
 */
public class EffRoundRectMaskGlyph extends EffGlyph  {

  static BasicStroke stroke = new BasicStroke(2);
  RoundRectangle2D rr2d = new RoundRectangle2D.Double();
  Color fillColor = Color.WHITE;
  
  public EffRoundRectMaskGlyph(Color fillColor) {
    super();
    this.fillColor = fillColor;
    this.setDrawOrder(DrawOrder.DrawChildrenFirst);
  }
  
  RoundRectangle2D getShapeInPixels(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this, pixelbox);

    applyMinimumPixelBounds(pixelbox);
    Graphics2D g = view.getGraphics();
    g.setColor(getBackgroundColor());

    //Point arcSize = view.transformToPixels(new Point2D(arcWidth, arcHeight), new Point());
    int arcSize = Math.min(pixelbox.width, pixelbox.height);
    rr2d.setRoundRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height,
      arcSize, arcSize);
    
    return rr2d;
  }

  /** Clears the region behind the glyph (a rectangle minus a rounded rectangle, 
   *  then draws the outline using a BasicStroke.  This is designed such that
   *  drawChildren() should be called before draw.
   */
  @Override
  public void draw(ViewI view) {    
    RoundRectangle2D s = getShapeInPixels(view);
    Area a = new Area(s.getFrame());
    a.subtract(new Area(s));

    Graphics2D g2 = view.getGraphics();
    g2.setColor(fillColor);
    g2.fill(a);

    g2.setColor(getColor());
    g2.setStroke(stroke);
    g2.draw(s);    
  }
}
