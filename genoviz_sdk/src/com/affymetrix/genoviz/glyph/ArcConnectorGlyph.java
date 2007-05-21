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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.util.*;

/**
 * Draws an arc line between the closest corners of two glyphs.
 * The glyphs should be on the same y coordinate.
 */
public class ArcConnectorGlyph extends Glyph {

  private java.util.List<GlyphI> twoglyphs = new ArrayList<GlyphI>(2);
  private int spans;

  private boolean aboveaxis = false;

  /** If this number is greater than 0,
   * it is drawn at the center of the arc,
   * to designate the number of spans that bridge the gap between the glyphs.
   */
  public void setSpans (int i ) {
    spans = i;
  }

  public void incrementSpans () {
    spans++;
  }

  public void decrementSpans () {
    spans--;
  }

  /**
   * Only two glyphs can be connected with an ArcConnectorGlyph,
   * any more additions will be ignored except for an error message.
   */
  public void addGlyph ( GlyphI glyph ) {
    if ( !twoglyphs.contains ( glyph ) ) {
      if ( twoglyphs.size() > 2 ) {
        System.err.println ( "ConnectorGlyph: already have two glyphs.");
        return;
      }
      twoglyphs.add( glyph );
    }
  }

  public void addGlyphs ( GlyphI glyph1, GlyphI glyph2 ) {
    addGlyph ( glyph1 );
    addGlyph ( glyph2 );
  }

  /** Returns the number of glyphs that have already been added. */
  public int getGlyphCount () {
    return twoglyphs.size();
  }

  public void draw ( ViewI view ) {

    if ( twoglyphs.size() != 2 ) {
      System.err.println ("ArcConnectorGlyph: Do not have two glyphs to connect, not drawing.");
      return;
    }
    Graphics g = view.getGraphics();
    g.setColor ( getForegroundColor() );

    int startAngle, arcAngle;
    double x1, x2;
    GlyphI leftGlyph, rightGlyph;
    Rectangle2D left, right;
    left = new Rectangle2D();
    right = new Rectangle2D();

    x1 =  twoglyphs.get( 0 ).getCoordBox().x;
    x2 =  twoglyphs.get( 1 ).getCoordBox().x;

    if ( x1 < x2 ) {
      leftGlyph = twoglyphs.get( 0 );
      rightGlyph = twoglyphs.get( 1 );
    } else {
      leftGlyph = twoglyphs.get( 1 );
      rightGlyph = twoglyphs.get( 0 );
    }

    left = leftGlyph.getCoordBox();
    right = rightGlyph.getCoordBox();
    coordbox.x = left.x + left.width;
    coordbox.y = left.y;
    if ( coordbox.y < 0 ) coordbox.y = left.y - ( left.height / 2 ); //above axis
    else  coordbox.y = left.y - (left.height / 8);                   //below axis

    coordbox.width = right.x - ( left.x + left.width );
    coordbox.height = (left.height * 1.5);

    if ( coordbox.y < 0 ) arcAngle = -180; //above axis
    else arcAngle = 180;                   //below axis

    view.transformToPixels ( coordbox, pixelbox );
    if ( spans > 0 ) g.drawString ( Integer.toString (spans), ( pixelbox.x + pixelbox.width/2 -3  ), pixelbox.y + pixelbox.height /2 );
    g.drawArc ( pixelbox.x - 2, pixelbox.y, pixelbox.width + 3, pixelbox.height, 180, arcAngle );
    g.drawArc ( pixelbox.x - 2, pixelbox.y+1, pixelbox.width + 3, pixelbox.height, 180, arcAngle );

    super.draw ( view );
  }

}
