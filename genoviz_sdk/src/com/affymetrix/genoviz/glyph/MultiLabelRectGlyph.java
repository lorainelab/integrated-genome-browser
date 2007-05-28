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

import java.awt.*;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 * Given a collection of label strings sorted by size,
 * adds the longest label that fits to a solid rectangle glyph.
 */
public class MultiLabelRectGlyph extends FillRectGlyph implements MultiLabelledGlyphI {

  //the label currently being drawn
  String text;

  //the collection of labels to be drawn - these should be
  //sorted by String length, shortest to longest
  private String[] label_strings = null;

  //label widths - for drawing
  private int[] label_sizes = null;

  //whether or not label strings have already been measured
  private boolean sized_labels = false;

  /**
   * Tell this MultLabelRectGlyph what Strings to draw on itself.
   * @param strs - should be sorted by size - shortest to longest.
   *
   * <pre>
   *      String[] strs = new String[];
   *      strs[0] = "Exon: ";
   *      strs[1] = "Exon: 2";
   *      strs[2] = "Exon: 2 1 - 100";
   *      MultiLabelRectGlyph m_g = new MultLabelRectGlyph();
   *      m_g.setLabelStrings(strs);
   * </pre>
   *
   * @throws NullPointerException if any String in the array is null.
   * This is preferable to throwing an exception on the graphics painting thread later.
   */
  public void setLabelStrings(String[] strs) {
    this.label_strings = strs;
    this.label_sizes = new int[strs.length];
    for (int i = 0; i<strs.length; i++) {
      if (strs[i] == null) throw new NullPointerException();
    }
  }

  /**
   * Return the sorted array of Strings this MultiLabelRectGlyph
   * draws on itself.
   */
  public String[] getLabelStrings() {
    return label_strings;
  }

  /**
   * Tell this MultiLabelRectGlyph what Font to use for drawing
   * it's labels.  If @param font differs from the current Font,
   * then remeasure our label strings, storing the results in
   * a private array of ints.
   */
  public void setFont(Font font) {
    if ( ! getFont().equals(font) ) {
      sized_labels = false;
    }
    super.setFont(font);
  }

  /**
   * Returns the String currently being drawn on this
   * MultiLabelRectGlyph.
   */
  public String getLabelText() {
    return this.text;
  }

  // find the index for the longest label that fits inside
  // width (pixels) when drawn on the screen
  // assumes that the labels are sorted - shortest to longest
  private int getLabelIndex(int width) {
    int num_labels = label_sizes.length;
    for (int i = num_labels - 1 ; i >= 0 ; i--) {
      if (width >= label_sizes[i]) {
        return i;
      }
    }
    return -1;
  }

  // use FontMetrics m to re-calculate the widths for
  // the label strings stored in label_strings
  // stash the results in label_sizes
  private void calculateWidths(FontMetrics m) {
    int num_labels = label_sizes.length;
    for (int i = 0 ; i < num_labels ; i++) {
      label_sizes[i] = m.stringWidth(label_strings[i]);
    }
  }

  // CLH: This is the constant that the glyph uses to decide
  //      if it should even bother checking to see if the label
  //      will fit. Below this threshold it just assumest that
  //      it will not fit.
  public static final int min_width_needed_for_text = 32;

  /**
   * Draw a rectangle.  If {@link #getLabelStrings()} returns a value,
   * then draw the largest possible label on the rectangle.  If
   * none of the labels fits, then draw a horizontal line instead.
   * @see MultiLabelRoundRectGlyph#draw
   * @see LabelledRectGlyph#draw
   */
  public void draw(ViewI view) {
    super.draw( view );
    if( getLabelStrings() != null ) {
      Graphics g = view.getGraphics();

      // CLH: Added a check to make sure there is at least _some_ room
      // before we start getting setting the font and checking metrics.
      // No need to do this on a 1 px rectangle!

      if (pixelbox.width >= min_width_needed_for_text) {
        Font savefont = g.getFont();
        Font f2 = this.getFont();
        if (f2 != savefont) {
          g.setFont(f2);
        } else {
          // If they are equal, there's no need to restore the font
          // down below.
          savefont = null;
        }
        FontMetrics fm = g.getFontMetrics();

        if (!sized_labels) {
          calculateWidths(fm);
        }

        //if no labels can fit, returns -1
        int index = getLabelIndex(pixelbox.width);

        g.setColor(this.getForegroundColor());
        int midline = pixelbox.y + pixelbox.height / 2;

        if( index != -1 ) {
          int text_width = label_sizes[index];
          text = getLabelStrings()[index];
          int mid = pixelbox.x + ( pixelbox.width / 2 ) - ( text_width / 2 );
          // define adjust such that: ascent-adjust = descent+adjust
          if (null != fm) {
            int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0);
            g.drawString( text, mid, midline + adjust );
          }
        }
        if (null != savefont) {
          g.setFont(savefont);
        }
      }
    }
  }


}
