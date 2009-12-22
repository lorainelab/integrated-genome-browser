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

import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * The PixelFloaterGlyph is meant to be a container / wrapper for glyphs that wish to 
 *   hold their position in pixel space.
 * Any descendants in a PixelFloaterGlyph will be drawn as if the view maps directly 
 *   to pixels in X, Y, or both -- in other words the view's transform is the identity transform 
 *   in one or both dimensions.
 * The PixelFloaterGlyph also reimplements intersects, etc., so that the 
 *   PixelFloaterGlyph is always intersected by anything that intersects the 
 *   current view (the View's viewbox).
 */
public final class PixelFloaterGlyph extends Glyph  {
  LinearTransform childtrans = new LinearTransform();
  Rectangle2D.Double view_pix_box = new Rectangle2D.Double();
  boolean OUTLINE_BOUNDS = false;
  boolean XPIXEL_FLOAT = false;
  boolean YPIXEL_FLOAT = true;

  /**
   *  Should only have to modify view to set Y part of transform to identity 
   *     transform.
   *  not sure if need to set view's coord box...
   */
  public void drawTraversal(ViewI view) {
    LinearTransform vtrans = view.getTransform();
    Rectangle2D.Double vbox = view.getCoordBox();
    Rectangle pbox = view.getPixelBox();
    setChildTransform(view);
    view_pix_box.setRect(vbox.x, (double)pbox.y,
    			 vbox.width, (double)pbox.height);
    view.setTransform(childtrans);
    view.setCoordBox(view_pix_box);
    super.drawTraversal(view);
    view.setTransform(vtrans);
    view.setCoordBox(vbox);
  }

  public void draw(ViewI view) {
    if (OUTLINE_BOUNDS) {
      Graphics g = view.getGraphics();
      g.setColor(Color.yellow);
      Rectangle2D.Double cbox = this.getCoordBox();
      g.drawRect(view.getPixelBox().x + 1, (int)cbox.y, 
		 view.getPixelBox().width - 2, (int)cbox.height);
    }
  }

  protected void setChildTransform(ViewI view)  {
    LinearTransform vtrans = view.getTransform();
    if (YPIXEL_FLOAT) {
      childtrans.setScaleY(1.0);
      childtrans.setTranslateY(0.0);
    }
    else {
      childtrans.setScaleY(vtrans.getScaleY());
      childtrans.setTranslateY(vtrans.getTranslateY());
    }
    if (XPIXEL_FLOAT) {
      childtrans.setScaleX(1.0);
      childtrans.setTranslateX(0.0);
    }
    else {
      childtrans.setScaleX(vtrans.getScaleX());
      childtrans.setTranslateX(vtrans.getTranslateX());
    }
  }

  /*public boolean intersects(Rectangle rect)  {
    return isVisible;
  }*/
  public boolean intersects(Rectangle2D.Double rect, ViewI view)  {
    return isVisible;
  }
  public boolean withinView(ViewI view) {
    return true;
  }

  Rectangle scratchRect = new Rectangle();
  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
                            ViewI view)  {
    LinearTransform vtrans = view.getTransform();
    double cached_y = pickRect.y;
    double cached_height = pickRect.height;
    Rectangle2D.Double vbox = view.getCoordBox();
    Rectangle pbox = view.getPixelBox();
    view_pix_box.setRect(vbox.x, (double)pbox.y,
    			 vbox.width, (double)pbox.height);

    view.transformToPixels(pickRect, scratchRect);
    pickRect.y = (double)scratchRect.y;
    pickRect.height = (double)scratchRect.height;

    setChildTransform(view);
    view.setTransform(childtrans);

    view.setCoordBox(view_pix_box);

    super.pickTraversal(pickRect, pickList, view);

    pickRect.y = cached_y;
    pickRect.height = cached_height;
    view.setTransform(vtrans);
    view.setCoordBox(vbox);
    
  }

  public void getChildTransform(ViewI view, LinearTransform trans) {
    setChildTransform(view);
    trans.setTransform(childtrans);
  }

}
