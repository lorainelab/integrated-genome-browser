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
import java.util.List;

import com.affymetrix.genoviz.bioviews.ViewI;
import java.util.ArrayList;

/**
 * Glyph originally intended for representing partially loaded data along an
 * axis.  Primary selections represent ranges that have been selected but not
 * loaded, seconary selections are ranges that have been loaded already.  Text
 * drawing mechanism is borrowed from LabelledRectGlyph.  As of 11/10/00, this
 * glyph is representing partial loading for Genisys in the class PartialLoader,
 * and for TIGR in ChromosomeViewer.
 */
public class RangeSelectGlyph extends FillRectGlyph implements LabelledGlyphI {

  List<Range> primary_selections = new ArrayList<Range>();
  List<Range> secondary_selections = new ArrayList<Range>();
  boolean clip_new_ranges = false;
  Color second_color = Color.gray,
    text_color = Color.black;
  CharSequence text = "";
  java.awt.Font font;
  public static final int min_width_needed_for_text = 32;

  /**
   * Selects a range in coordiate space.  The range will be drawn in the foreground color.  Ranges converted
   * to a secondary selection will be drawn in the color set with setSeconaryColor (default gray).
   */
  public void selectRange ( Range range ) {
    if ( range.min < coordbox.x ) {
      range.min = (int) coordbox.x;
    }
    if ( range.max > coordbox.x + coordbox.width ) {
      range.max = (int) (coordbox.x + coordbox.width);
    }
    mergeRange ( range );
  }

  /**
   * Sets whether ranges added via selectRange() will be split and/or clipped to avoid
   * previous secondary ranges, or if they will simply overwrite them.
   */
  public void clipNewRanges ( boolean clip_new_ranges ) {
    this.clip_new_ranges = clip_new_ranges;
  }

  private void mergeRange( Range new_range ) {
    // first, make sure that the new range doesn't overlap with old secondary selections.  Clip and separate as necessary to prevent redundancy.
    Range old_range;
    if ( clip_new_ranges ) {
      for ( int i = 0; i < secondary_selections.size(); i++ ) {
        old_range = secondary_selections.get(i);
        /* There are four possible relationships between the old range and the new range:
         * 1) They don't intersect, so continue.
         * 2) The new range is contained entirely within the old range, return and don't add the new range
         * 3) The old range is contained entirely within the new range, split the new range into two parts and call this method recursively with them
         * 4) They do intersect, but only on one side of the old range: truncate the new range to exlclude the old range
         */

        // case 1:
        if ( old_range.max < new_range.min || old_range.min > new_range.max ) {
          continue;
        }
        // case 2:
        if ( new_range.min > old_range.min && new_range.max < old_range.max ) {
          return;
        }
        // case 3:
        if ( new_range.min < old_range.min && new_range.max > old_range.max ) {
          mergeRange( new Range(new_range.min, old_range.min) );
          mergeRange( new Range(old_range.max, new_range.max) );
          return;
        }
        // case 4:
        if ( new_range.min > old_range.min && new_range.max > old_range.max ) {
          new_range.min = old_range.max;
        }
        if ( new_range.min < old_range.min && new_range.max < old_range.max ) {
          new_range.max = old_range.min;
        }
      }
    }
    /* ok, so that's the end of avoiding collsisons with secondary selections.
     * the next task is to merge the new primary selection with any other primary
     * selections that overlap the same area.  Again, there are four possibilities,
     * only they are dealt with slightly differently.
     */
    for ( int i = 0; i < primary_selections.size(); i++ ) {
      old_range = primary_selections.get(i);
      // case 1:
      if ( old_range.max < new_range.min || old_range.min > new_range.max ) {
        continue;
      }
      // case 2:
      if ( new_range.min > old_range.min && new_range.max < old_range.max ) {
        return;
      }
      // case 3:
      if ( new_range.min < old_range.min && new_range.max > old_range.max ) {
        primary_selections.remove( i );
        primary_selections.add( new_range );
        return;
      }
      // case 4:
      if ( new_range.min > old_range.min && new_range.max > old_range.max ) {
        old_range.max = new_range.max;
        return;
      }
      if ( new_range.min < old_range.min && new_range.max < old_range.max ) {
        old_range.min = new_range.min;
        return;
      }
    }
    primary_selections.add( new_range );
  }

  /**
   * @return a List of Range's that represent the coordinates (in coordinate space,
   * not pixel space) of the primary selections
   */
  public List<Range> getPrimarySelections() {
    return primary_selections;
  }

  /**
   * @return a List of Range's that represent the coordinates (in coordinate space,
   * not pixel space) of the secondary selections
   */
  public List<Range> getSecondarySelections() {
    return secondary_selections;
  }

  /**
   * Converts all primary selections to secondary selections.
   */
  @SuppressWarnings("unchecked")
  public void convertRanges () {
    secondary_selections.addAll(primary_selections);
    primary_selections.clear();
  }

  /**
   * Clears all primary selections
   */
  public void clearPrimarySelections() {
    primary_selections.clear();
  }

  /**
   * Deselects all primary/secondary ranges set with selectRange()
   */
  public void clearRanges() {
    primary_selections.clear();
    secondary_selections.clear();
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

  @Override
  public void draw ( ViewI view ) {
    super.draw ( view );
    Graphics g = view.getGraphics();
    java.awt.geom.Rectangle2D.Double select_rect = new java.awt.geom.Rectangle2D.Double();
    Range range;
    select_rect.y = coordbox.y;
    select_rect.height = coordbox.height;
    g.setColor ( second_color );
    for ( int i = 0; i < secondary_selections.size(); i++ ) {
      range = secondary_selections.get(i);
      select_rect.x = range.min;
      select_rect.width = range.max - range.min;
      view.transformToPixels ( select_rect, pixelbox );
      g.fillRect ( pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height );
    }
    g.setColor ( getForegroundColor() );
    for ( int i = 0; i < primary_selections.size(); i++ ) {
      range = primary_selections.get(i);
      select_rect.x = range.min;
      select_rect.width = range.max - range.min;
      view.transformToPixels ( select_rect, pixelbox );
      // if select_rect is at least 1 coordinate value wide,
      // then make sure the pixelbox is at least 1 pixel wide
      // so that its location can be seen.
      if (pixelbox.width < 1 && select_rect.width >= 0) {
        pixelbox.width = 1;
      }
      g.fillRect ( pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height );
    }
    // draw label, if text has been set.
    if( getLabel() != null ) {
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
        int text_width = fm.stringWidth(this.text.toString());
        int midline = pixelbox.y + pixelbox.height / 2;
        if(text_width <= pixelbox.width ) {
          int mid = pixelbox.x + ( pixelbox.width / 2 ) - ( text_width / 2 );
          // define adjust such that: ascent-adjust = descent+adjust
          int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0);
          g.drawString(this.text.toString(), mid, midline + adjust );
        }
        if (null != savefont) {
          g.setFont(savefont);
        }
      }
    }
  }

  public CharSequence getLabel() { return text; }

  @Override
  public void setFont(Font f) { font = f; }

  public void setLabel(CharSequence str) { text = str; }

}
