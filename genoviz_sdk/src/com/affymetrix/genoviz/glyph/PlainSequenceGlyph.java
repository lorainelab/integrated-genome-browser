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

/**
 * A glyph that only displays sequence residue letters,
 * and only at the appropriate resolution.
 * @deprecated Use SequenceGlyph with no background color.
 */
public class PlainSequenceGlyph extends AbstractResiduesGlyph {
  // these are relative to the sequence of the parent glyph
  int parent_seq_beg, parent_seq_end;

  protected Color letter_color = Color.black;
  String sequence;

  boolean setSequence = false;

  Rectangle2D scratchrect;

  public PlainSequenceGlyph() {
    super();
    scratchrect = new Rectangle2D();
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

  public void draw(ViewI view)
  {
    double pixels_per_base, bases_per_pixel;
    pixels_per_base = ((LinearTransform)view.getTransform()).getScaleX();
    bases_per_pixel = 1/pixels_per_base;

    if (((double)((int)pixels_per_base) == pixels_per_base) &&
        ((int)pixels_per_base == font_width))
    {
      int i, pixelstart, pixelwidth;
      double doublestart;
      if (sequence != null)
      {
        Rectangle pixelclipbox = view.getPixelBox();
        Rectangle2D coordclipbox = view.getCoordBox();
        Graphics g = view.getGraphics();

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

        scratchrect.reshape(visible_seq_beg,  coordbox.y,
                            visible_seq_span, coordbox.height);
        view.transformToPixels(scratchrect, pixelbox);
        int seq_pixel_offset = pixelbox.x;
        int seq_pixel_width =  pixelbox.width;

        doublestart = (double)seq_pixel_offset;
        pixelstart = (int)doublestart;
        int baseline = (pixelbox.y+(pixelbox.height/2)) +
                        fontmet.getAscent()/2;
        g.setFont(getResidueFont());
        g.setColor(getForegroundColor());

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

  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  isVisible?pixel_hitbox.intersects(pixelbox):false;
  }

  public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
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
