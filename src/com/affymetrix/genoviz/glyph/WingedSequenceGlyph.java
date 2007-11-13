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
import java.io.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.NeoConstants;

/**
 * Extends SequnceGlyph, but puts an ellipsis in the middle of the
 * sequence if it doesn't fit.  Only supports horizontal drawing at this
 * time.
 */

public class WingedSequenceGlyph extends SequenceGlyph
  implements NeoConstants {

  Font variable_font;

  public WingedSequenceGlyph() {

    this(HORIZONTAL);
  }

  public WingedSequenceGlyph(int orientation) {
    super(orientation);
    full_rect = new FillRectGlyph();
    scratchrect = new Rectangle2D();
    //setVariableFont ( new Font ( "SansSerif", Font.PLAIN, 14 ) );
  }


  protected void drawHorizontal(ViewI view) {
    Rectangle pixelclipbox = view.getPixelBox();
    Rectangle2D coordclipbox = view.getCoordBox();
    Graphics g = view.getGraphics();
    double pixels_per_base, bases_per_pixel;
    int visible_ref_beg, visible_ref_end;
    int visible_seq_beg, visible_seq_end, visible_seq_span;
    int seq_beg_index, seq_end_index;
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

      scratchrect.reshape(visible_seq_beg,  coordbox.y,
          visible_seq_span, coordbox.height);
      view.transformToPixels(scratchrect, pixelbox);
      pixels_per_base = ((LinearTransform)view.getTransform()).getScaleX();
      bases_per_pixel = 1/pixels_per_base;
      int seq_pixel_offset = pixelbox.x;
      int seq_pixel_width =  pixelbox.width;
      // ***** background already drawn in drawTraversal(), so just return if
      // ***** scale is < 1 pixel per base
      if (!residuesSet) return;
      int i, pixelstart, pixelwidth;
      double doublestart;
      doublestart = (double)seq_pixel_offset;
      pixelstart = (int)doublestart;
      Font fnt = getResidueFont();
      FontMetrics fntMet = g.getFontMetrics(fnt);
      int fntWidth = fntMet.charWidth('M');
      int baseline = (pixelbox.y+(pixelbox.height/2)) +
        fntMet.getAscent()/2 - 1;
      g.setFont( getResidueFont() );
      g.setColor( getForegroundColor() );

      int fullwidth;
      fullwidth = (int) ( ( (LinearTransform)view.getTransform() ).getScaleX() * coordbox.width );
      if ( pixels_per_base < 1 ) {  return;
      }

      // ***** otherwise semantic zooming to show more detail *****
      else {
        if ( visible_seq_span > 0 ) {

          // ***** draw the sequence string for visible bases if possible ****
          // Should the DNA string be drawn?
          //  already tested for setDNA
          //  test for scale being natural number (integer double)
          //    and for scale matching font size
          //      System.out.println(pixels_per_base);
          if (((double)((int)pixels_per_base) == pixels_per_base) &&
              ((int)pixels_per_base >= fntWidth) ) {

            g.drawString(sequence.substring(seq_beg_index,seq_end_index),
                pixelstart, baseline);
          }

          else if ( fntMet.stringWidth ( sequence ) < fullwidth )
            g.drawString(sequence.substring(seq_beg_index,seq_end_index),
                pixelstart, baseline);

          else if ( (pixelbox.width / fntWidth) > 3 ) {
            //draw two segments of bases w/ ellipsis
            int bases_per_side = ( ( pixelbox.width / fntWidth ) - 3 ) / 2 ;
            StringBuffer s = new StringBuffer();
            s.append ( sequence.substring ( seq_beg_index, seq_beg_index + bases_per_side) );
            s.append ( "\u00b7" );
            s.append ( "\u00b7" );
            s.append ( "\u00b7" );
            s.append ( sequence.substring ( seq_end_index - bases_per_side, seq_end_index ) );
            int centering_space = (pixelbox.width - ( s.length() * fntWidth) ) / 2;
            g.drawString ( s.toString(), pixelstart + centering_space, baseline );
            g.drawRect ( pixelbox.x, pixelbox.y, pixelbox.width,  pixelbox.height );
          }
        }
      }
    }
  }

}
