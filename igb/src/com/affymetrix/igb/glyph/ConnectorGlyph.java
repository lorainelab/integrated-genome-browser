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

import java.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.SolidGlyph;

public class ConnectorGlyph extends SolidGlyph {
  boolean FILL_CONNECTION = true;
  boolean OUTLINE_CONNECTION = true;
  GlyphI gla;
  GlyphI glb;
  NeoMap mapa;
  NeoMap mapb;
  boolean invert;
  int[] xpoints = new int[4];
  int[] ypoints = new int[4];

  public ConnectorGlyph(GlyphI ga, GlyphI gb, NeoMap ma, NeoMap mb, boolean inv) {
    this.gla = ga;
    this.glb = gb;
    this.mapa = ma;
    this.mapb = mb;
    this.invert = inv;
  }

  public void draw(ViewI conview) {
    Graphics g = conview.getGraphics();
    ViewI aview = mapa.getView();
    ViewI bview = mapb.getView();
    if ((gla.getCoordBox().intersects(aview.getCoordBox())) && 
	(glb.getCoordBox().intersects(bview.getCoordBox())) ) {

      aview.transformToPixels(gla.getCoordBox(), pixelbox);

      int astart = pixelbox.x;
      int aend = pixelbox.x + pixelbox.width;
      bview.transformToPixels(glb.getCoordBox(), pixelbox);
      int bstart = pixelbox.x;
      int bend = pixelbox.x + pixelbox.width;
      conview.transformToPixels(coordbox, pixelbox);
      g.setColor(getBackgroundColor());
      if (FILL_CONNECTION) {
	if (invert) {
	  xpoints[0] = astart;
	  ypoints[0] = pixelbox.y;
	  xpoints[1] = bend;
	  ypoints[1] = pixelbox.y + pixelbox.height;
	  xpoints[2] = bstart;
	  ypoints[2] = pixelbox.y + pixelbox.height;
	  xpoints[3] = aend;
	  ypoints[3] = pixelbox.y;
	}
	else {
	  xpoints[0] = astart;
	  ypoints[0] = pixelbox.y;
	  xpoints[1] = bstart;
	  ypoints[1] = pixelbox.y + pixelbox.height;
	  xpoints[2] = bend;
	  ypoints[2] = pixelbox.y + pixelbox.height;
	  xpoints[3] = aend;
	  ypoints[3] = pixelbox.y;
	}
	g.fillPolygon(xpoints, ypoints, 4);
      }
      if (OUTLINE_CONNECTION) {
	g.setColor(getForegroundColor());
	if (invert) {
	  g.drawLine(astart, pixelbox.y, bend, pixelbox.y + pixelbox.height);
	  g.drawLine(aend, pixelbox.y, bstart, pixelbox.y + pixelbox.height);
	}
	else {
	  g.drawLine(astart, pixelbox.y, bstart, pixelbox.y + pixelbox.height);
	  g.drawLine(aend, pixelbox.y, bend, pixelbox.y + pixelbox.height);
	}
      }
    }
    super.draw(conview);
  }

}
