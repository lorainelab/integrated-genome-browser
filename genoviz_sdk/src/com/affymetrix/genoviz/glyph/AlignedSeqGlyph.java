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

import com.affymetrix.genoviz.bioviews.ResiduePainter;

import java.awt.*;

public class AlignedSeqGlyph extends SequenceGlyph {

  public AlignedSeqGlyph() {
    super();
  }

  public void setResidues( String r ) {
    super.setResidues( r );
    this.bgMapperArray = new Color[r.length()];
    reColor();
  }

  private final Color getColor( int theIndex ) {
    if ( null == this.bgMapperArray ) {
      return getBackgroundColor();
    }
    if ( theIndex < this.bgMapperArray.length ) {
      return this.bgMapperArray[ theIndex ];
    }
    return getBackgroundColor();
  }

  private ResiduePainter painter = new BasicResiduePainter();

  private Color[] bgMapperArray;

  private int seqBeg = 0; // index into consensus
  private String consensus = null;

  public void setConsensus( String theConsensus, int theOffset ) {
    this.consensus = theConsensus;
    this.seqBeg = theOffset;
  }

  public void reColor() {

    Object model = this.getInfo();
    if ( null == model ) {
      this.painter.calculateColors
        ( this.bgMapperArray, getResidues(), this.consensus, 0, this.seqBeg );
    }
    else {
      this.painter.calculateColors
        ( model, this.bgMapperArray, getResidues(), this.consensus, 0, this.seqBeg );
    }
  }

  public void setPainter( ResiduePainter p ) {
    this.painter = p;
    reColor();
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

    int baseline = (this.pixelbox.y+(this.pixelbox.height/2)) + this.fontmet.getAscent()/2 - 1;
    int pixPerBaseRounded = (int)Math.round( pixelsPerBase + 0.5f );

    g.setFont( getResidueFont() );
    g.setColor( getForegroundColor() );
    Color nextColor, lastColor = g.getColor().darker();
    for ( int i = seqBegIndex; i < seqEndIndex; i++ ) {
      nextColor = getColor( i );
      if ( ! nextColor.equals( lastColor ) ) {
        g.setColor( nextColor );
        lastColor = nextColor;
      }
      double backxf = i - seqBegIndex;
      int backx = pixelStart + (int)Math.round( backxf * pixelsPerBase );
      g.fillRect( backx, pixelbox.y, pixPerBaseRounded, pixelbox.height );
    }
    g.setColor( getForegroundColor() );


    if ( this.font_width <= Math.round( pixelsPerBase ) ) { // Ample room to draw residue letters.
      for ( int i = seqBegIndex; i < seqEndIndex; i++ ) {
        double f = i - seqBegIndex;
        g.drawString( String.valueOf(
          residues.charAt( i ) ),
          ( pixelStart + (int) ( f * pixelsPerBase ) ),
          baseline );
      }
    } else { // Not enough room for letters in this font if sequence is dense.
      int h = Math.max( 1, Math.min( this.pixelbox.height, this.fontmet.getAscent() ) );
      int y = Math.min( baseline, ( this.pixelbox.y + this.pixelbox.height ) ) - h;
      for ( int i = seqBegIndex; i < seqEndIndex; i++ ) {
        if ( !Character.isWhitespace( residues.charAt( i ) ) ) {
          int w = ( int ) Math.max( 1, pixelsPerBase - 1 );
          // Make it wider if spaces follow.
          for ( int a=i+1; a < seqEndIndex && ' ' == residues.charAt( a ); a++ ) {
            w += pixelsPerBase;
          }
          double f = i - seqBegIndex;
          if ( w <= this.font_width ) {
            int x = pixelStart + ( int ) ( f * pixelsPerBase );
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

}
