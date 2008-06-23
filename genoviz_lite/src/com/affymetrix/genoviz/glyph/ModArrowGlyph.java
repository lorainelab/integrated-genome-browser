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

import com.affymetrix.genoviz.bioviews.ViewI;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * another arrow glyph which adjusts to it's coord bounds.
 * This does not extend DirectedGlyph
 * and, hence, wont work on vertical maps.
 */
public class ModArrowGlyph extends SolidGlyph {

    private boolean forward;
    private int stemwidth;
    private Rectangle scratchrect = new Rectangle();

    /** sets the start and end of the left arrow in coordinate space */
    public boolean isForward () { return forward; }
    public int getStemWidth () { return stemwidth; }

    public void setStemWidth ( int stemwidth ) {
      this.stemwidth = stemwidth;
    }

    public void setForward ( boolean forward ) {
      this.forward = forward;
    }

    public void select(boolean selected) {
      setSelected(selected);
    }

  /*
    I'm overriding these next two methods so that I can have the glyph have a minimum size,
    so it really draws outside of its coordbox, and yet still functions with correct selection rules.
    I'm not sure if this is good design or not, but it draws what I want, so...  See
    pickTraversal() in glyph to get a better idea of why/how I did what is below.
    -JMM 12/2/99
   */

  public boolean hit(java.awt.geom.Rectangle2D.Double coord_hitbox, ViewI view)  {
    view.transformToPixels ( coordbox, pixelbox );
    if ( pixelbox.width > pixelbox.height ) return super.hit( coord_hitbox, view );
    view.transformToPixels ( coord_hitbox, scratchrect );
    if ( forward ) pixelbox.x += pixelbox.width - pixelbox.height;
    pixelbox.width = pixelbox.height;
    return ( pixelbox.intersects ( scratchrect ) );
  }

  public boolean intersects(java.awt.geom.Rectangle2D.Double rect, ViewI view)  {
    view.transformToPixels ( coordbox, pixelbox );
    if ( pixelbox.width > pixelbox.height ) return super.hit ( rect, view );
    view.transformToPixels ( rect, scratchrect );
    if ( forward ) pixelbox.x += pixelbox.width - pixelbox.height;
    pixelbox.width = pixelbox.height;
    return ( pixelbox.intersects ( scratchrect ) );
  }

  protected void drawSelectedOutline ( ViewI view ) {
    draw(view);
    view.transformToPixels ( coordbox, pixelbox );
    if ( pixelbox.width > pixelbox.height ) {
      super.drawSelectedOutline(view);
      return;
    }
    Graphics g = view.getGraphics();
    g.setColor(view.getScene().getSelectionColor());
    if ( forward )
      g.drawRect (pixelbox.x + pixelbox.width - pixelbox.height - 2,
          pixelbox.y - 2, pixelbox.height + 3, pixelbox.height + 3);
    else
      g.drawRect (pixelbox.x - 2,
          pixelbox.y - 2, pixelbox.height + 3, pixelbox.height + 3);
  }

    public void draw ( ViewI view ) {
        Graphics g = view.getGraphics();
        g.setPaintMode();
        view.transformToPixels ( coordbox, pixelbox );
        drawArrow ( g, pixelbox, forward );
    }

    // Height of the pixelbox is used for both height and width of the arrowheads.

    private void drawArrow ( Graphics g, Rectangle r, boolean forward ) {
      int centery = r.y + r.height / 2;
      g.setColor ( getBackgroundColor() );
      if ( forward ) {
        g.fillRect ( r.x, centery - stemwidth/2 + 1, r.width - r.height, stemwidth );
        drawArrowHead ( g, r.x + r.width - r.height, r.y, r.height, r.height , forward );
      }
      else {
        g.fillRect ( r.x + r.height, centery - stemwidth/2 + 1, r.width - r.height, stemwidth );
        drawArrowHead ( g, r.x, r.y, r.height, r.height , forward );
      }
      pixelbox.x = Math.min ( pixelbox.x, r.x );
    }

    private void drawArrowHead(Graphics g, int x, int y, int width, int height, boolean forward) {
      boolean fillArrowHead = true;
      int[] arrx = new int[3];
      int[] arry =  new int[3];
      if (forward) {
        arrx[0] = x;
        arry[0] = y;
        arrx[1] = x;
        arry[1] = y + height;
        arrx[2] = x + width;
        arry[2] = y + height / 2;
      }
      else {
        arrx[0] = x + width;
        arry[0] = y;
        arrx[1] = x + width;
        arry[1] = y + height;
        arrx[2] = x;
        arry[2] = y + height / 2;
      }
      if (fillArrowHead) {
        g.fillPolygon(arrx, arry, 3);
      }
      else {
        g.drawPolygon(arrx, arry, 3);
      }
    }

}
