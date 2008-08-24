/**
*   Copyright (c) 1998-2008 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genoviz.glyph;

import java.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.ExpandBehavior;
import com.affymetrix.genoviz.widget.XY;
import java.awt.geom.Rectangle2D;

/**
 * RootGlyph should not be used directly.
 * It is for internal use by the NeoWidgets.
 *
 * RootGlyph is used internally by NeoWidgets
 * as the root glyph of the widget's (and scene's) glyph hierarchy.
 */
public class RootGlyph extends StretchContainerGlyph {

  protected ExpandBehavior[] expansion_behavior = { ExpandBehavior.NO_EXPAND, ExpandBehavior.NO_EXPAND };
  //  protected Rectangle2D testbox = new Rectangle2D();
  protected boolean show_outline = false;

  public void setExpansionBehavior(XY axis, ExpandBehavior behavior) {
    expansion_behavior[axis.ordinal()] = behavior;
  }

  public ExpandBehavior getExpansionBehavior(XY axis) {
    return expansion_behavior[axis.ordinal()];
  }

  @Override
  public void propagateStretch(GlyphI child) {
    if (expansion_behavior[XY.X.ordinal()] == ExpandBehavior.EXPAND && 
      expansion_behavior[XY.Y.ordinal()] == ExpandBehavior.EXPAND) {
      super.propagateStretch(child);
      return;
    }
    Rectangle2D.Double childbox = child.getCoordBox();
    if (expansion_behavior[XY.X.ordinal()] == ExpandBehavior.EXPAND) {
      double xbeg = Math.min(childbox.x, coordbox.x);
      double xend = Math.max(childbox.x + childbox.width,
          coordbox.x + coordbox.width);
      coordbox.x = xbeg;
      coordbox.width = xend - xbeg;
    }
    else if (expansion_behavior[XY.Y.ordinal()] == ExpandBehavior.EXPAND) {
      double ybeg = Math.min(childbox.y, coordbox.y);
      double yend = Math.max(childbox.y + childbox.height,
          coordbox.y + coordbox.height);
      coordbox.y = ybeg;
      coordbox.height = yend - ybeg;
    }
  }

  @Override
  public void drawTraversal(ViewI view) {
    super.drawTraversal(view);
    if (show_outline) {
      view.transformToPixels(coordbox, pixelbox);
      Graphics g= view.getGraphics();
      g.setColor(Color.green);
      g.drawRect(pixelbox.x+2, pixelbox.y+2,
          pixelbox.width-4, pixelbox.height-4);
    }
  }

  /**
   * Calculates the pixel box
   * and delegates the rest to the super class.
   *
   * @param view into the scene of which this is the root glyph
   */
  @Override
  public void draw(ViewI view) {

    /* The reason this is done is so that pickTraversalByPixel will work.
     * Otherwise, the root glyph's pixel box is always empty.
     * Hence, since all glyphs are children of the root glyph,
     * no glyphs can get hit. -- Eric 1998-12-12
     */
    view.transformToPixels(coordbox, pixelbox);
    super.draw(view);
  }


  public void setShowOutline(boolean show_outline) {
    this.show_outline = show_outline;
  }

  public boolean getShowOutline() {
    return show_outline;
  }
}
