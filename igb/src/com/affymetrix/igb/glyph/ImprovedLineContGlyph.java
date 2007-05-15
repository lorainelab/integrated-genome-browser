/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.GeometryUtils;

import java.awt.*;

/**
 *  A glyph that displays as a centered line and manipulates children to center on the
 *  same line.
 *
 *  LineContainerGlyph is very convenient for representing data that has a range but
 *  has multiple sub-ranges within it, such as genes which have a known intron/exon
 *  structure.
 *
 *  Improves rendering performance by subclassing drawTraversal() and optimizing
 *     to just draw a filled rect if glyph is small, and skip drawing children.
 *
 *
 */
public class ImprovedLineContGlyph extends Glyph  {
  static final boolean optimize_child_draw = true;
  static final boolean DEBUG_OPTIMIZED_FILL = false;
  boolean move_children = true;

  public void drawTraversal(ViewI view)  {
    if (optimize_child_draw) {
      view.transformToPixels(coordbox, pixelbox);
      if (withinView(view) && isVisible) {
        if (pixelbox.width <=3 || pixelbox.height <=3) {
          // still ends up drawing children for selected, but in general
          //    only a few glyphs are ever selected at the same time, so should be fine
          if (selected) { drawSelected(view); }
          else  { fillDraw(view); }
        }
        else {
          super.drawTraversal(view);  // big enough to draw children
        }
      }
    }
    else {
      super.drawTraversal(view);  // no optimization, so draw children
    }
  }

  public void fillDraw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    Graphics g = view.getGraphics();
    if (DEBUG_OPTIMIZED_FILL) {
      g.setColor(Color.white);
    }
    else {
      g.setColor(getBackgroundColor());
    }

    EfficientGlyph.fixAWTBigRectBug(view, pixelbox);

    if (pixelbox.width < 1) { pixelbox.width = 1; }
    if (pixelbox.height < 1) { pixelbox.height = 1; }
    // draw the box
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }

  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    if (pixelbox.width == 0) { pixelbox.width = 1; }
    if (pixelbox.height == 0) { pixelbox.height = 1; }
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());

    EfficientGlyph.fixAWTBigRectBug(view, pixelbox);

    // We use fillRect instead of drawLine, because it may be faster.
    g.fillRect(pixelbox.x, pixelbox.y+pixelbox.height/2, pixelbox.width, 1);

    super.draw(view);
  }

  /**
   *  When adding children, their vertical position will be changed to
   *  center them on the line if {@link #isMoveChildren()} is true.
   */
  public void addChild(GlyphI glyph) {
    if (move_children) {
      // child.cbox.y is modified, but not child.cbox.height)
      // center the children of the LineContainerGlyph on the line
      Rectangle2D cbox = glyph.getCoordBox();
      double ycenter = this.coordbox.y + this.coordbox.height/2;
      cbox.y = ycenter - cbox.height/2;
    }
    super.addChild(glyph);
  }

  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  isVisible ? pixel_hitbox.intersects(pixelbox) : false;
  }

  public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
    return isVisible ? coord_hitbox.intersects(coordbox) : false;
  }

  /**
   * If true, {@link #addChild(GlyphI)} will automatically center the child vertically.
   */
  public boolean isMoveChildren() {
    return this.move_children;
  }

  /**
   * Set whether {@link #addChild(GlyphI)} will automatically center the child vertically.
   */
  public void setMoveChildren(boolean move_children) {
    this.move_children = move_children;
  }

}
