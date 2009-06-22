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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.geom.Rectangle2D;
//import com.affymetrix.genoviz.bioviews.*;

public final class GridGlyph extends Glyph {
  double grid_spacing = 0;
  Rectangle gridbox_pix = new Rectangle();
  Rectangle2D.Double gridbox_coords = new Rectangle2D.Double();

  public void setGridSpacing(double spacing) {
    grid_spacing = spacing;
  }
  
  public double getGridSpacing() { return grid_spacing; }

	@Override
  public void drawTraversal(ViewI view) {
    coordbox.setRect(view.getCoordBox());
    super.drawTraversal(view);
  }

	@Override
  public void draw(ViewI view) {
    // don't draw unless grid spacing has been set to > 0
    if (grid_spacing <0) { return; }

    // always fit GridGlyph to be same coords as parent
    //    coordbox.setRect(getParent().getCoordBox());
    coordbox.setRect(view.getCoordBox());

    Rectangle2D.Double vbox = view.getCoordBox();

    double xmin = vbox.x;
    double xmax = vbox.x + vbox.width;
    double xlength = vbox.width;
    double num_lines = xlength / grid_spacing;
    // if lines get too dense, don't draw
    //    System.out.println("num lines: " + num_lines);
    if (num_lines > 200) { return; }
		     
    Rectangle pixelbox = view.getScratchPixBox();
    Graphics g = view.getGraphics();

    //    g.setColor(Color.lightGray);
    g.setColor(this.getColor());
    double cur_xcoord = ((xmin % grid_spacing) * grid_spacing) + grid_spacing;
    //    System.out.println("cur_xcoord: " + cur_xcoord);
    while (cur_xcoord <= xmax) {
      gridbox_coords.setRect(cur_xcoord, vbox.y, 1, vbox.height);
      view.transformToPixels(gridbox_coords, gridbox_pix);
      g.fillRect(gridbox_pix.x, gridbox_pix.y, 2, gridbox_pix.height);
      cur_xcoord += grid_spacing;
    }
   }

}
