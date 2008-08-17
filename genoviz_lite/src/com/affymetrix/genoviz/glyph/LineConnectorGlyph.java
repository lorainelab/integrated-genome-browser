/**
*   Copyright (c) 1998-2007 Affymetrix, Inc.
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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * Draws a line between the centers of two glyphs.
 */
public class LineConnectorGlyph extends Glyph {

  private ArrayList<GlyphI> twoglyphs = new ArrayList<GlyphI>(2);

  /**
   * Only two glyphs can be connected with a LineConnectorGlyph,
   * any more additions will be ignored except for an error message.
   */
  public void addGlyph( GlyphI glyph ) {
    if ( !twoglyphs.contains ( glyph ) ) {
      if ( twoglyphs.size() > 2 ) {
        System.err.println( "ConnectorGlyph: already have two glyphs.");
        return;
      }
      twoglyphs.add( glyph );
    }
  }

  public void addGlyphs ( GlyphI glyph1, GlyphI glyph2 ) {
    addGlyph ( glyph1 );
    addGlyph ( glyph2 );
  }

  /** @return the number of glyphs that have already been added. */
  public int getGlyphCount () {
    return twoglyphs.size();
  }

  @Override
  public void draw ( ViewI view ) {

    if ( twoglyphs.size() != 2 ) {
      return;
    }
    Graphics g = view.getGraphics();
    g.setColor ( getForegroundColor() );

    double x1, x2;
    GlyphI leftGlyph, rightGlyph;

    x1 =  twoglyphs.get( 0 ).getCoordBox().x;
    x2 =  twoglyphs.get( 1 ).getCoordBox().x;

    if ( x1 < x2 ) {
      leftGlyph = twoglyphs.get ( 0 );
      rightGlyph = twoglyphs.get ( 1 );
    } else {
      leftGlyph = twoglyphs.get ( 1 );
      rightGlyph = twoglyphs.get ( 0 );
    }

    Rectangle2D.Double left, right;
    left = leftGlyph.getCoordBox();
    right = rightGlyph.getCoordBox();
    coordbox.x = left.x + left.width / 2;
    coordbox.y = left.y + left.height / 2;
    coordbox.width = ( right.x + right.width / 2) - coordbox.x;
    coordbox.height = (right.y + right.height / 2) - coordbox.y;

    view.transformToPixels ( coordbox, pixelbox );
    g.drawLine ( pixelbox.x, pixelbox.y, pixelbox.x + pixelbox.width, pixelbox.y + pixelbox.height );
    super.draw ( view );

  }

}
