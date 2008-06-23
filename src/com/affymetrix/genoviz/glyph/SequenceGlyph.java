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
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.geom.Rectangle2D;

/**
 * A glyph that shows a sequence of residues.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 * @author Gregg Helt
 */
public class SequenceGlyph extends AbstractResiduesGlyph {
  int parent_seq_beg, parent_seq_end;
  String sequence;
  FillRectGlyph full_rect;
  boolean residuesSet = false;
  Rectangle2D.Double scratchrect;

  private boolean show_background = true;

  private boolean drawingRects = false;
  public final boolean isDrawingRects() {
    return this.drawingRects;
  }

  public SequenceGlyph() {
    this(NeoConstants.Orientation.Horizontal);
  }

  public SequenceGlyph(NeoConstants.Orientation orientation) {
    super(orientation);
    full_rect = new FillRectGlyph();
    scratchrect = new java.awt.geom.Rectangle2D.Double();
  }

  public void setOrientation(NeoConstants.Orientation orientation) {
    this.orient = orientation;
    java.awt.geom.Rectangle2D.Double cb = this.getCoordBox();
    this.setCoords(cb.x, cb.y, cb.width, cb.height);
  }

  public void setCoords(double x, double y, double width, double height) {
    super.setCoords(x, y, width, height);
    full_rect.setCoords(x, y, width, height);
  }

  public void setCoordBox(java.awt.geom.Rectangle2D.Double coordbox) {
    super.setCoordBox(coordbox);
    full_rect.setCoordBox(coordbox);
  }

  public void setResidues(String sequence) {
    this.sequence = sequence;
    residuesSet = true;
  }

  public String getResidues() {
    return sequence;
  }

  public void select(boolean selected) {
    super.setSelected(selected);
  }

  /**
   * Overriding drawTraversal() to affect drawing order.
   * <ol>
   * <li> Draw background rectangle.
   * <li> Draw any child glyphs.
   * <li> Draw residues if at appropriate resolution.
   * </ol>
   *
   * <p> Note that SequenceGlyph only supports visual subselection
   * if Scene.selectionAppearance is set to OUTLINE.
   * If set to FILL, will visually fill whole glyph
   * even if only part of glyph is actually selected.
   */
  public void drawTraversal(ViewI view)  {
    if (coordbox.intersects(view.getCoordBox()) && isVisible) {

      // 1.) draw background rectangle
      if (selected && view.getScene().getSelectionAppearance() == SceneI.SelectType.SELECT_FILL) {
        full_rect.setSelected(true);
        full_rect.drawTraversal(view);
        full_rect.setSelected(false);
      }
      else if (show_background) {
        full_rect.drawTraversal(view);
      }

      // 2.) draw any child glyphs
      if (children != null) {
        drawChildren(view);
      }

      // 3.) draw residues if at appropriate resolution
      if (selected) { drawSelected(view); }
      else  { draw(view); }
    }
  }

  public void draw(ViewI view) {
    if (orient == NeoConstants.Orientation.Horizontal) {
      drawHorizontal( view );
    } else if (orient == NeoConstants.Orientation.Vertical) {
      drawVertical(view);
    }
    super.draw ( view );
  }

  protected void drawVertical(ViewI view) {
    java.awt.geom.Rectangle2D.Double coordclipbox = view.getCoordBox();
    Graphics g = view.getGraphics();
    g.setFont( getResidueFont() );
    FontMetrics fntMet = g.getFontMetrics();
    double pixels_per_base;
    int visible_ref_beg, visible_ref_end,
        visible_seq_beg, visible_seq_end, visible_seq_span,
        seq_beg_index, seq_end_index;
    visible_ref_beg = (int)coordclipbox.y;
    visible_ref_end =  (int)(coordclipbox.y + coordclipbox.height);
    // adding 1 to visible ref_end to make sure base is drawn if only
    // part of it is visible
    visible_ref_end = visible_ref_end+1;

    // ******** determine first base and last base displayed ********
    visible_seq_beg = (seq_beg < visible_ref_beg) ? visible_ref_beg : seq_beg;
    visible_seq_end = (seq_end > visible_ref_end) ? visible_ref_end : seq_end;
    visible_seq_span = visible_seq_end - visible_seq_beg;
    seq_beg_index = visible_seq_beg - seq_beg;
    seq_end_index = visible_seq_end - seq_beg;

    if (null != sequence && seq_beg_index <= sequence.length()) {

      if (seq_end_index > sequence.length()) {
        seq_end_index = sequence.length();
      }

      scratchrect.setRect(coordbox.x, visible_seq_beg,
          coordbox.width, visible_seq_span);
      view.transformToPixels(scratchrect, pixelbox);
      pixels_per_base = ((LinearTransform)view.getTransform()).getScaleY();

      // ***** background already drawn in drawTraversal(), so just return if
      // ***** scale is < 1 pixel per base
      if ( pixels_per_base < 1 || !residuesSet) {
        return;
      }

      // ***** otherwise semantic zooming to show more detail *****
      else {
        if ( visible_seq_span > 0 ) {
          int i, pixelstart;
          double doublestart;
          // ***** draw the sequence string for visible bases if possible ****
          // Should the DNA string be drawn?
          //  already tested for setDNA
          //  test for scale being natural number (integer double)
          //    and for scale matching font size
          if (((double)((int)pixels_per_base) == pixels_per_base) &&
              ((int)pixels_per_base >= fntMet.getHeight() - 4) &&
              pixelbox.width >= (fntMet.charWidth('C'))) {
            doublestart = (double)pixelbox.y;
            int asc = fntMet.getAscent();
            pixelstart = (int)doublestart + asc;
            int midline = (pixelbox.x+(pixelbox.width/2)) -
              fntMet.charWidth('G')/2;

            g.setColor( getForegroundColor() );

            for (i = seq_beg_index; i < seq_end_index; i++) {
              if ((sequence.length() != 0) && (i+1 <= sequence.length())) {
                g.drawString(sequence.substring(i,i+1), midline, pixelstart);
                pixelstart += asc;
              }
            }
          }
        }
      }
    }
  }

  protected void drawHorizontal(ViewI view) {
    java.awt.geom.Rectangle2D.Double coordclipbox = view.getCoordBox();
    Graphics g = view.getGraphics();
    double pixels_per_base;
    int visible_ref_beg, visible_ref_end,
        visible_seq_beg, visible_seq_end, visible_seq_span,
        seq_beg_index, seq_end_index;
    visible_ref_beg = (int)coordclipbox.x;
    visible_ref_end =  (int)(coordclipbox.x + coordclipbox.width);
    // adding 1 to visible ref_end to make sure base is drawn if only
    // part of it is visible
    visible_ref_end = visible_ref_end+1;

    // ******** determine first base and last base displayed ********
    visible_seq_beg = (seq_beg < visible_ref_beg) ? visible_ref_beg : seq_beg;
    visible_seq_end = (seq_end > visible_ref_end) ? visible_ref_end : seq_end;
    visible_seq_span = visible_seq_end - visible_seq_beg;
    seq_beg_index = visible_seq_beg - seq_beg;
    seq_end_index = visible_seq_end - seq_beg;

    if (null != sequence && seq_beg_index <= sequence.length()) {

      if (seq_end_index > sequence.length()) {
        seq_end_index = sequence.length();
      }

      scratchrect.setRect(visible_seq_beg,  coordbox.y,
          visible_seq_span, coordbox.height);
      view.transformToPixels(scratchrect, pixelbox);
      pixels_per_base = ((LinearTransform)view.getTransform()).getScaleX();

      int seq_pixel_offset = pixelbox.x;

      // ***** background already drawn in drawTraversal(), so just return if
      // ***** scale is < 1 pixel per base
      if ( pixels_per_base < 1 || !residuesSet) {
        return;
      }

      // ***** otherwise semantic zooming to show more detail *****
      else {
        if ( visible_seq_span > 0 ) {
          drawHorizontalResidues
            ( g, pixels_per_base, this.sequence, seq_beg_index, seq_end_index, seq_pixel_offset );
        }
      }
    }
  }

  /**
   * Draw the sequence string for visible bases if possible.
   *
   * <p> We are showing letters regardless of the height constraints on the glyph.
   * This is temporary until we get more intelligent font management for sequence glyphs.
   */
  protected void drawHorizontalResidues
    ( Graphics g,
      double pixelsPerBase,
      String residues,
      int seqBegIndex,
      int seqEndIndex,
      int pixelStart ) {
    g.setFont( getResidueFont() );
    int fntWidth = g.getFontMetrics().charWidth('M');
    int baseline = (this.pixelbox.y+(this.pixelbox.height/2)) + g.getFontMetrics().getAscent()/2 - 1;
    g.setColor( getForegroundColor() );
    if ( fntWidth < pixelsPerBase ) { // Ample room to draw residue letters.
      for ( int i = seqBegIndex; i < seqEndIndex; i++ ) {
        double f = i - seqBegIndex;
        g.drawString( String.valueOf(
          residues.charAt( i ) ),
          ( pixelStart + (int) ( f * pixelsPerBase ) ),
          baseline );
      }
    }
    else if (
      ( (double)( (int) pixelsPerBase ) == pixelsPerBase ) // Make sure it's an integral number of pixels per base.
      && ( fntWidth == pixelsPerBase )
      )
    { // pixelsPerBase matches the font width.
      // Draw the whole string in one go.
      g.drawString(residues.substring(seqBegIndex,seqEndIndex),
        pixelStart, baseline);
    }
    else { // Not enough room for letters in this font if sequence is dense.
      int h = Math.max( 1, Math.min( this.pixelbox.height, g.getFontMetrics().getAscent() ) );
      int y = Math.min( baseline, ( this.pixelbox.y + this.pixelbox.height ) ) - h;
      for ( int i = seqBegIndex; i < seqEndIndex; i++ ) {
        if ( !Character.isWhitespace( residues.charAt( i ) ) ) {
          int w = ( int ) Math.max( 1, pixelsPerBase - 1 );
          // Make it wider if spaces follow.
          for ( int a=i+1; a < seqEndIndex && ' ' == residues.charAt( a ); a++ ) {
            w += pixelsPerBase;
          }
          double f = i - seqBegIndex;
          int x = pixelStart + ( int ) ( f * pixelsPerBase );
          if ( w <= fntWidth ) {
            if ( this.isDrawingRects() ) {
              g.drawRect( x, y, w-1, h-1 );
            }
          }
          else { // There is enough room for residue letter.
            g.drawString(
              String.valueOf( residues.charAt( i ) ),
              ( pixelStart + (int) ( f * pixelsPerBase ) ),
              baseline );
          }
        }
      }
    }
  }

  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  isVisible?pixel_hitbox.intersects(pixelbox):false;
  }

  public boolean hit(java.awt.geom.Rectangle2D.Double coord_hitbox, ViewI view)  {
    return isVisible?coord_hitbox.intersects(coordbox):false;
  }

  /**
   * overriding moveRelative to make sure full_rect glyph also gets moved.
   * (We shouldn't need to do this for moveAbsolute,
   *  since it calls moveRelative.)
   */
  public void moveRelative(double diffx, double diffy) {
    full_rect.moveRelative(diffx, diffy);
    super.moveRelative(diffx, diffy);
  }

  public void addChild(GlyphI child, int position) {
    super.addChild(child, position);
    child.setCoords(child.getCoordBox().x, this.coordbox.y,
        child.getCoordBox().width, this.coordbox.height);
  }

  public void addChild(GlyphI child) {
    super.addChild(child);
    child.setCoords(child.getCoordBox().x, this.coordbox.y,
        child.getCoordBox().width, this.coordbox.height);
  }

  public void setParentSeqStart(int beg)  {
    parent_seq_beg = beg;
  }

  public void setParentSeqEnd(int end)  {
    parent_seq_end = end;
  }

  public int getParentSeqStart() {
    return parent_seq_beg;
  }

  public int getParentSeqEnd() {
    return parent_seq_end;
  }

  public void setBackgroundColor(Color c) {
    super.setBackgroundColor(c);
    full_rect.setBackgroundColor(c);
  }

  /**
   * Need to override setScene()
   * to make sure arrowglyph gets its scene set properly.
   */
  public void setScene(Scene s) {
    super.setScene(s);
    full_rect.setScene(s);
  }

  /** Set whether or not the background will be filled-in
   *  with solid color.  If false, background is transparent,
   *  except if the selection mode is FILL and the glyph is selected.
   *  Default is true.
   */
  public void setShowBackground(boolean show) {
    show_background = show;
  }

  /** Whether or not the background will be filled-in
   *  with solid color.  If false, background is transparent,
   *  except if the selection mode is FILL and the glyph is selected.
   *  Default is true.
   */
  public final boolean getShowBackground() {
    return show_background;
  }

}
