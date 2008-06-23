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
import java.util.*;
import java.io.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.GeneralUtils;

/**
 * A glyph that only displays sequence residue letters,
 * and only at the appropriate resolution.
 * @deprecated Use SequenceGlyph with no background color.
 */
@Deprecated
public class PlainSequenceGlyph extends AbstractResiduesGlyph {
  // these are relative to the sequence of the parent glyph
  int parent_seq_beg, parent_seq_end;

  protected Color letter_color = Color.black;
  String sequence;

  boolean setSequence = false;

  java.awt.geom.Rectangle2D.Double scratchrect;

  public PlainSequenceGlyph() {
    super();
    scratchrect = new java.awt.geom.Rectangle2D.Double();
  }

  public void setResidues(String sequence) {
    this.sequence = sequence;
    setSequence = true;
  }

  public String getResidues() {
    return sequence;
  }

  /**
   *  Deprecated -- use getResidues() instead
   */
  public String getSequence() {
    return sequence;
  }

  @Override
  public void draw(ViewI view) {
    Graphics2D g = view.getGraphics();
    FontMetrics fm = g.getFontMetrics(getResidueFont());

    double pixels_per_base = ((LinearTransform)view.getTransform()).getScaleX();

    int font_width = AbstractResiduesGlyph.getMaxCharacterWidth(fm);
    if (((double)((int)pixels_per_base) == pixels_per_base) &&
        ((int)pixels_per_base == font_width)) {
      int pixelstart;
      double doublestart;
      if (sequence != null) {
        java.awt.geom.Rectangle2D.Double coordclipbox = view.getCoordBox();
        g.setFont(getResidueFont());
        g.setColor(getForegroundColor());

        int visible_ref_beg, visible_ref_end, visible_seq_beg,
            visible_seq_end, visible_seq_span,
            seq_beg_index, seq_end_index;
        visible_ref_beg = (int)coordclipbox.x;
        visible_ref_end =  (int)(coordclipbox.x + coordclipbox.width);

        // ******** determine first base and last base displayed ********
        visible_seq_beg = (seq_beg < visible_ref_beg) ? visible_ref_beg : seq_beg;
        visible_seq_end = (seq_end > visible_ref_end) ? visible_ref_end : seq_end;
        visible_seq_span = visible_seq_end - visible_seq_beg;
        seq_beg_index = visible_seq_beg - seq_beg;
        seq_end_index = visible_seq_end - seq_beg;

        scratchrect.setRect(visible_seq_beg,  coordbox.y,
                            visible_seq_span, coordbox.height);
        view.transformToPixels(scratchrect, pixelbox);
        int seq_pixel_offset = pixelbox.x;

        doublestart = (double)seq_pixel_offset;
        pixelstart = (int)doublestart;
        int baseline = (pixelbox.y+(pixelbox.height/2)) + fm.getAscent()/2;

        if ((sequence.length() != 0) && (seq_end_index <= sequence.length()))
          g.drawString(sequence.substring(seq_beg_index,seq_end_index),
                       pixelstart, baseline);
        else
          System.err.println("ERROR : Either the sequence is NULL OR endIndex of sequence substring is greater than the sequence length");
      }
    }
    // VERY INEFFICIENT for drawing selection -- need to optimize soon
    super.draw(view);
  }

  @Override
  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  isVisible?pixel_hitbox.intersects(pixelbox):false;
  }

  @Override
  public boolean hit(java.awt.geom.Rectangle2D.Double coord_hitbox, ViewI view)  {
    return isVisible?coord_hitbox.intersects(coordbox):false;
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

}
