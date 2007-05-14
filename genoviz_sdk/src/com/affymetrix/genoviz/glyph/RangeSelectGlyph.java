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

import java.util.Vector;
import java.awt.*;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;

/**
 * Glyph originally intended for representing partially loaded data along an
 * axis.  Primary selections represent ranges that have been selected but not
 * loaded, seconary selections are ranges that have been loaded already.  Text
 * drawing mechanism is borrowed from LabelledRectGlyph.  As of 11/10/00, this
 * glyph is representint partial loading for Genisys in the class PartialLoader,
 * and for TIGR in ChromosomeViewer.
 */
public class RangeSelectGlyph extends FillRectGlyph implements LabelledGlyphI {

  Vector primary_selections = new Vector(5, 10);
  Vector secondary_selections = new Vector (5, 10);
  boolean clip_new_ranges = false;
  Color second_color = Color.gray,
    text_color = Color.black;
  String text = "";
  java.awt.Font font;
  public static final int min_width_needed_for_text = 32;

  /**
   * Selects a range in coordiate space.  The range will be drawn in the foreground color.  Ranges converted
   * to a secondary selection will be drawn in the color set with setSeconaryColor (default gray).
   */
  public void selectRange ( int[] range ) {
    if ( range.length != 2 ) throw new IllegalArgumentException ( "argument for selectRange must contain two and only two ints" );
    if ( range[0] > range[1] ) {  // flip ranges if necessary so all have start < end
      int temp = range[0];
      range[0] = range[1];
      range[1] = temp;
    }
    if ( range[0] < coordbox.x ) range[0] = (int)coordbox.x;
    if ( range[1] > coordbox.x + coordbox.width ) range[1] = (int)(coordbox.x + coordbox.width);
    mergeRange ( range );
  }

  /**
   * Sets whether ranges added via selectRange() will be split and/or clipped to avoid
   * previous secondary ranges, or if they will simply overwrite them.
   */
  public void clipNewRanges ( boolean clip_new_ranges ) {
    this.clip_new_ranges = clip_new_ranges;
  }

  @SuppressWarnings("unchecked")
  private void mergeRange( int[] new_range ) {
    // first, make sure that the new range doesn't overlap with old secondary selections.  Clip and separate as necessary to prevent redundancy.
    int[] old_range;
    if ( clip_new_ranges ) {
      for ( int i = 0; i < secondary_selections.size(); i++ ) {
        old_range = (int[])secondary_selections.elementAt(i);
        /* There are four possible relationships between the old range and the new range:
         * 1) They don't intersect, so continue.
         * 2) The new range is contained entirely within the old range, return and don't add the new range
         * 3) The old range is contained entirely within the new range, split the new range into two parts and call this method recursively with them
         * 4) They do intersect, but only on one side of the old range: truncate the new range to exlclude the old range
         */

        // case 1:
        if ( old_range[1] < new_range[0] || old_range[0] > new_range[1] ) continue;
        // case 2:
        if ( new_range[0] > old_range[0] && new_range[1] < old_range[1] ) return;
        // case 3:
        if ( new_range[0] < old_range[0] && new_range[1] > old_range[1] ) {
          mergeRange ( new int[] { new_range[0], old_range[0] } );
          mergeRange ( new int[] { old_range[1], new_range[1] } );
          return;
        }
        // case 4:
        if ( new_range[0] > old_range[0] && new_range[1] > old_range[1] ) new_range[0] = old_range[1];
        if ( new_range[0] < old_range[0] && new_range[1] < old_range[1] ) new_range[1] = old_range[0];
      }
    }
    /* ok, so that's the end of avoiding collsisons with secondary selections.
     * the next task is to merge the new primary selection with any other primary
     * selections that overlap the same area.  Again, there are four possibilities,
     * only they are dealt with slightly differently.
     */
    for ( int i = 0; i < primary_selections.size(); i++ ) {
      old_range = (int[])primary_selections.elementAt(i);
      // case 1:
      if ( old_range[1] < new_range[0] || old_range[0] > new_range[1] ) continue;
      // case 2:
      if ( new_range[0] > old_range[0] && new_range[1] < old_range[1] ) return;
      // case 3:
      if ( new_range[0] < old_range[0] && new_range[1] > old_range[1] ) {
        primary_selections.removeElementAt ( i );
        primary_selections.addElement( new_range );
        return;
      }
      // case 4:
      if ( new_range[0] > old_range[0] && new_range[1] > old_range[1] ) {
        old_range[1] = new_range[1];
        return;
      }
      if ( new_range[0] < old_range[0] && new_range[1] < old_range[1] ) {
        old_range[0] = new_range[0];
        return;
      }
    }
    primary_selections.addElement ( new_range );
  }

  /**
   * @return a Vector of int[]'s that represent the coordinates (in coordinate space,
   * not pixel space) of the primary selections
   */
  public Vector getPrimarySelections() {
    return primary_selections;
  }

  /**
   * @return a Vector of int[]'s that represent the coordinates (in coordinate space,
   * not pixel space) of the secondary selections
   */
  public Vector getSecondarySelections() {
    return secondary_selections;
  }

  /**
   * Converts all primary selections to secondary selections.
   */
  @SuppressWarnings("unchecked")
  public void convertRanges () {
    secondary_selections.addAll(primary_selections);
    primary_selections.removeAllElements();
  }

  /**
   * Clears all primary selections
   */
  public void clearPrimarySelections() {
    primary_selections.removeAllElements();
  }

  /**
   * Deselects all primary/secondary ranges set with selectRange()
   */
  public void clearRanges() {
    primary_selections.removeAllElements();
    secondary_selections.removeAllElements();
  }

  /**
   * Sets the color that secondary selections will be drawn in.
   */
  public void setSecondaryColor ( Color secondary ) {
    second_color = secondary;
  }

  public void setTextColor ( Color c ) {
    text_color = c;
  }

  public Color getTextColor () {
    return text_color;
  }

  public void draw ( ViewI view ) {
    super.draw ( view );
    Graphics g = view.getGraphics();
    Rectangle2D select_rect = new Rectangle2D();
    int[] range;
    select_rect.y = coordbox.y;
    select_rect.height = coordbox.height;
    g.setColor ( second_color );
    for ( int i = 0; i < secondary_selections.size(); i++ ) {
      range = (int[])secondary_selections.elementAt ( i );
      select_rect.x = range[0];
      select_rect.width = range[1] - range[0];
      view.transformToPixels ( select_rect, pixelbox );
      g.fillRect ( pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height );
    }
    g.setColor ( getForegroundColor() );
    for ( int i = 0; i < primary_selections.size(); i++ ) {
      range = (int[])primary_selections.elementAt ( i );
      select_rect.x = range[0];
      select_rect.width = range[1] - range[0];
      view.transformToPixels ( select_rect, pixelbox );
      // if select_rect is at least 1 coordinate value wide,
      // then make sure the pixelbox is at least 1 pixel wide
      // so that its location can be seen.
      if (pixelbox.width < 1 && select_rect.width >= 0)
        pixelbox.width = 1;
      g.fillRect ( pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height );
    }
    // draw label, if text has been set.
    if( getText() != null ) {
      calcPixels(view);
      if (pixelbox.width >= min_width_needed_for_text) {
        Font savefont = g.getFont();
        Font f2 = this.getFont();
        g.setColor ( getTextColor() );
        if (f2 != savefont) {
          g.setFont(f2);
        } else {
          // If they are equal, there's no need to restore the font
          // down below.
          savefont = null;
        }
        FontMetrics fm = g.getFontMetrics();
        int text_width = fm.stringWidth(this.text);
        int midline = pixelbox.y + pixelbox.height / 2;
        if(text_width <= pixelbox.width ) {
          int mid = pixelbox.x + ( pixelbox.width / 2 ) - ( text_width / 2 );
          // define adjust such that: ascent-adjust = descent+adjust
          int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0);
          g.drawString(this.text, mid, midline + adjust );
        }
        if (null != savefont) {
          g.setFont(savefont);
        }
      }
    }
  }

  public String getText() { return text; }

  public void setFont(java.awt.Font f) { font = f; }

  public void setText(java.lang.String str) { text = str; }

}
