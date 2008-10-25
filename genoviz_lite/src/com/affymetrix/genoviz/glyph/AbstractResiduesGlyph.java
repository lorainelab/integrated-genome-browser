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

import java.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.NeoConstants;

/**
 *  An abstract base class for several different biological sequence glyphs.
 */
public abstract class AbstractResiduesGlyph extends Glyph implements ResiduesGlyphI {
  protected static final Font default_font = new Font("Courier", Font.BOLD, 12);
  protected static final Color default_residue_color = Color.black;

  Glyph sel_glyph;

  /**
   * seq_beg and seq_end are the sequence start and end positions
   * relative to the reference coordinate system.
   */
  protected int seq_beg, seq_end;
  protected NeoConstants.Orientation orient = NeoConstants.Orientation.Horizontal;

  /**
   * creates a horizontal glyph.
   */
  public AbstractResiduesGlyph() {
    this(NeoConstants.Orientation.Horizontal);
  }

  /**
   * creates an abstract residues glyph.
   *
   * @param orientation should be HORIZONTAL or VERTICAL
   */
  public AbstractResiduesGlyph(NeoConstants.Orientation orientation) {
    orient = orientation;
    //    setResidueFont(default_font);
    //setResidueColor(default_residue_color);
    setResidueFont( default_font );
    setForegroundColor( default_residue_color );
  }

  /** Get the max width required for drawing the characters 'A', 'C', 'G' or 'T'. */
  public static int getMaxCharacterWidth(FontMetrics fm) {
    int width = fm.charWidth('C');
    width = Math.max(width, fm.charWidth('A'));
    width = Math.max(width, fm.charWidth('G'));
    width = Math.max(width, fm.charWidth('T'));
    return width;
  }

  @Override
  public void setResidueFont(Font fnt) {
    // change font
    this.style = stylefactory.getStyle( style.getForegroundColor(), style.getBackgroundColor(), fnt );

    if (children != null) {
      for (GlyphI child : children) {
        if (child instanceof ResiduesGlyphI) {
          ((ResiduesGlyphI) child).setResidueFont(fnt);
        }
      }
    }
  }

  @Override
  public Font getResidueFont() { return style.getFont(); }

  /**
   * Calls super.setCoords and resets the reference space.
   * Also resets the coords for all the children.
   */
  @Override
  public void setCoords(double x, double y, double width, double height) {
    super.setCoords(x, y, width, height);
    if (orient == NeoConstants.Orientation.Horizontal) {
      seq_beg = (int)(coordbox.x);
      seq_end = (int)(coordbox.x + coordbox.width);
    } else if (orient == NeoConstants.Orientation.Vertical) {
      seq_beg = (int)(coordbox.y);
      seq_end = (int)(coordbox.y + coordbox.height);
    }
    if (children != null) {
      int i;
      GlyphI child;
      java.awt.geom.Rectangle2D.Double childbox;
      for (i=0; i<children.size(); i++) {
        child = children.get(i);
        childbox = child.getCoordBox();
        child.setCoords(childbox.x, y, childbox.width, height);
      }
    }
    if (sel_glyph != null) {
      java.awt.geom.Rectangle2D.Double selbox = sel_glyph.getCoordBox();
      sel_glyph.setCoords(selbox.x, y, selbox.width, height);
    }
  }

  /**
   * This turns around and calls setCoords.
   */
  @Override
  public void setCoordBox( java.awt.geom.Rectangle2D.Double theBox ) {
    setCoords( theBox.x, theBox.y, theBox.width, theBox.height );
  }

  /**
   * @see #select(int, int)
   */
  public void select (double start, double end) {
    select((int)start, (int)end);
  }

  /**
   * Selects a range of residues.
   *
   * @param start the first residue to be selected.
   * @param end the last residue to be selected.
   */
  public void select(int start, int end) {
    selected = true;
    if (end >= start) { end += 1; }
    else { start += 1; }
    if (sel_glyph == null) {
      sel_glyph = new OutlineRectGlyph();
    }
    if (orient == NeoConstants.Orientation.Horizontal) {
      if (start <= end) {
        if (start < coordbox.x) {
          start = (int)coordbox.x; }
        if (end > (coordbox.x + coordbox.width)) {
          end = (int)(coordbox.x + coordbox.width); }
      }
      else {
        if (end < coordbox.x) {
          end = (int)coordbox.x; }
        if (start > (coordbox.x + coordbox.width)) {
          start = (int)(coordbox.x + coordbox.width); }
      }
      sel_glyph.setCoords(start, coordbox.y, end-start, coordbox.height);
    } else if (orient == NeoConstants.Orientation.Vertical) {
      if (start <= end) {
        if (start < coordbox.y) {
          start = (int)coordbox.y; }
        if (end > (coordbox.y + coordbox.height)) {
          end = (int)(coordbox.y + coordbox.height); }
      }
      else {
        if (end < coordbox.y) {
          end = (int)coordbox.y; }
        if (start > (coordbox.y + coordbox.height)) {
          start = (int)(coordbox.y + coordbox.height); }
      }
      sel_glyph.setCoords(coordbox.x, start, coordbox.width, end-start);
    }
  }

  @Override
  public void setSelected(boolean selected) {
    super.setSelected(selected);
    if ( ! isSelected() ) {
      sel_glyph = null;
    }
  }

  @Override
  protected void drawSelectedFill(ViewI view) {
    super.drawSelectedFill(view);
  }

  @Override
  protected void drawSelectedOutline(ViewI view) {
    if (sel_glyph != null)  {
      draw(view);
      sel_glyph.setForegroundColor(view.getScene().getSelectionColor());
      sel_glyph.drawTraversal(view);
    }
    else {
      super.drawSelectedOutline(view);
    }
  }

}
