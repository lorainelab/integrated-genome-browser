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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;

/**
 * Draws an L-shaped marker glyph to denote the outline of a solid glyph
 * which represents a feature.
 * Use {@link Glyph#setForegroundColor} to set the main glyph color.
 * The "L" can appear in four distinct orientations, depending on the
 * values of booleans aboveAxis and forward.
 *
 * <p>The glyph can be drawn with a patch of a second color to
 * represent special cases.  To do so, use {@link #setPatchColor}.
 */

public class CornerGlyph extends DirectedGlyph {

  private int x1,y1;
  private Color patch_color;
  protected boolean draw_patch = false;
  private boolean aboveAxis=true;

  /**
   * Corner glyphs appear as upside-down L's when above the axis,
   * but as right-side-up L's below the axis.
   * When the orientation of the map is VERTICAL, the L's will be on their
   * sides, and aboveAxis means "to the right of the axis".
   */
  public void setAboveAxis(boolean b) {aboveAxis=b;}

  /**
   * Corner glyphs appear as upside-down L's when above the axis,
   * but as right-side-up L's below the axis.
   * When the orientation of the map is VERTICAL, the L's will be on their
   * sides, and aboveAxis means "to the right of the axis".
   */
  public boolean isAboveAxis(boolean b) {return aboveAxis;}

  private static final int MIN_PIXELS = 2;

  public void draw(ViewI view)
  {
    calcPixels(view);  //transform to pixels is done by calcPixels
    if (pixelbox.width <MIN_PIXELS)  pixelbox.width = MIN_PIXELS;
    if (pixelbox.height <MIN_PIXELS) pixelbox.height = MIN_PIXELS;
    Graphics g = view.getGraphics();

    if (getOrientation()==HORIZONTAL) {

      if ( isForward() ) {
        x1 = pixelbox.x;
      }
      else {
        x1 = pixelbox.x + pixelbox.width -1;
      }
      if ( aboveAxis ) {
        y1 = pixelbox.y+pixelbox.height -1;
      }
      else {
        y1 = pixelbox.y;
      }

      g.setColor(getForegroundColor());
      g.drawLine(x1, pixelbox.y,
                 x1, pixelbox.y + pixelbox.height-1);
      g.drawLine(pixelbox.x, y1, pixelbox.x + pixelbox.width-1, y1);

      if (draw_patch) {
        if (patch_color !=  getForegroundColor()) g.setColor(patch_color);
        int one_half = Math.max(pixelbox.width/2, MIN_PIXELS);
        if (isForward()) g.fillRect(x1-one_half, pixelbox.y + pixelbox.height/3,
                                    one_half,    pixelbox.height/3);
        else             g.fillRect(x1,          pixelbox.y + pixelbox.height/3,
                                    one_half,    pixelbox.height/3);
      }

    } else { // orientation==VERTICAL (aboveAxis now means to the right of axis)

      if ( isForward() ) {
        y1 = pixelbox.y + pixelbox.height -1;
      }
      else {
        y1 = pixelbox.y;
      }
      if ( aboveAxis ) {
        x1 = pixelbox.y+pixelbox.height -1;
      }
      else {
        x1 = pixelbox.y;
      }

      g.setColor(getForegroundColor());
      g.drawLine(pixelbox.x,                  y1,
                 pixelbox.x + pixelbox.width-1, y1);
      g.drawLine(x1, pixelbox.y, x1, pixelbox.y+pixelbox.height-1);

      if (draw_patch) {
        if (patch_color !=  getForegroundColor()) g.setColor(patch_color);
        int one_half = Math.max(pixelbox.height/2, MIN_PIXELS);
        if (isForward()) g.fillRect(pixelbox.x + pixelbox.width/3, y1,
                                    pixelbox.width/3,              one_half);
        else             g.fillRect(pixelbox.x + pixelbox.width/3, y1,
                                    pixelbox.width/3,              one_half);
      }
    }

    super.draw(view);
  }

  /**
   * Sets the 2nd color to draw a patch of the glyph
   * different from the foreground color.
   */
  public void setPatchColor( Color c ) {
    draw_patch = true;
    patch_color = c;
  }

  /**
   * Gets the 2nd color used to draw the patch.
   */
  public java.awt.Color getPatchColor() {
    return patch_color;
  }

  /**
   * @deprecated use {@link #setForegroundColor}.
   */
  @Deprecated
  public void setColor( Color c ) {
    setForegroundColor( c );
  }

  /**
   * @deprecated use {@link #getForegroundColor}.
   */
  @Deprecated
  public Color getColor() {
    return getForegroundColor();
  }

}
