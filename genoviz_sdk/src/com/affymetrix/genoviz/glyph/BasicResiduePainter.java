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
import com.affymetrix.genoviz.datamodel.SequenceI;
import java.awt.Color;

/**
 * paints residues.
 * This tries to do a simple job in the common situations.
 * It can paint nucleic acid or amino acid residues by residue
 * if there is no consensus or reference sequence.
 * If there is a consensus,
 * then it can paint based on agreement with that consensus.
 * The painting is simple and a bit arbitrary.
 * Coloring amino acid residues by hydrophobicity, for example,
 * is left to some other residue painter.
 */
public class BasicResiduePainter implements ResiduePainter {

  public static final int NAPAINTS = 0;
  public static final int AAPAINTS = 1;

  private int paints = this.NAPAINTS;

  public BasicResiduePainter( int paints ) {
    super();
    if ( this.NAPAINTS != paints && this.AAPAINTS != paints ) {
      throw new IllegalArgumentException
        ( "BasicResiduePainter: only NAPAINTS and AAPAINTS supported." );
    }
    this.paints = paints;
  }

  public BasicResiduePainter() {
    this( BasicResiduePainter.NAPAINTS );
  }

  /**
   * reports a "standard" color for a Nucleic Acid.
   * These colors roughly match those in a trace.
   * We use pink in place of red for Thymine and Uracile.
   * We still need to come up with colors for the abiguity codes.
   * For now light gray is returned.
   */
  private Color naColor( char theNucleotide ) {
    switch ( theNucleotide ) {
    case 'A': case 'a': return Color.green;
    case 'C': case 'c': return Color.cyan;
    case 'G': case 'g': return Color.yellow;
    case 'T': case 't': case 'U': case 'u': return Color.pink;
    }
    return Color.lightGray;
  }

  /**
   * reports a "standard" color for an Amino Acid.
   * <p> Need to come up with some "better" colors.
   */
  private Color aaColor( char theAminoAcid ) {
    switch ( theAminoAcid ) {
    case 'A': case 'a': return Color.green;
    case 'C': case 'c': return Color.green.brighter().brighter();
    case 'B': case 'b': // Almost the same as D.
    case 'D': case 'd': return Color.cyan;
    case 'Z': case 'z': // Almost the same as E.
    case 'E': case 'e': return Color.magenta;
    case 'F': case 'f': return Color.yellow;
    case 'G': case 'g': return Color.pink;
    case 'H': case 'h': return Color.green.brighter();
    case 'I': case 'i': return Color.cyan.brighter();
    case 'K': case 'k': return Color.magenta.brighter();
    case 'L': case 'l': return Color.yellow.brighter();
    case 'M': case 'm': return Color.pink.brighter();
    case 'N': case 'n': return Color.orange;
    case 'P': case 'p': return Color.orange.brighter();
    case 'Q': case 'q': return Color.blue.brighter();
    case 'R': case 'r': return Color.blue.brighter().brighter();
    case 'S': case 's': return Color.red.brighter();
    case 'T': case 't': return Color.red.brighter().brighter();
    case 'V': case 'v': return Color.orange.brighter().brighter();
    case 'W': case 'w': return Color.magenta.brighter();
    case 'Y': case 'y': return Color.magenta.brighter().brighter();
    }
    return Color.lightGray;
  }

  protected void calculateColors( Color[] colorArray, String residues, int residueStart ) {
    switch ( this.paints ) {
    case BasicResiduePainter.NAPAINTS:
      for ( int i = 0, j = residueStart; i < colorArray.length && j < residues.length(); i++, j++ ) {
        colorArray[ i ] = naColor( residues.charAt( j ) );
      }
      break;
    case BasicResiduePainter.AAPAINTS:
      for ( int i = 0, j = residueStart; i < colorArray.length && j < residues.length(); i++, j++ ) {
        char c = residues.charAt( j );
        colorArray[ i ] = aaColor( c );
        if ( 0 < i && ' ' == c ) {
          colorArray[i] = colorArray[i-1];
        }
      }
      break;
    }
  }

  public void calculateColors
    ( Object dataModel,
      Color[] theColors,
      String theResidues,
      String theConsensus,
      int residueStart,
      int consensusStart )
  {

    if ( null == dataModel ) {
      calculateColors
        ( theColors,
          theResidues,
          theConsensus,
          residueStart,
          consensusStart );
      return;
    }

    if ( dataModel instanceof SequenceI ) {
      SequenceI seq = ( SequenceI ) dataModel;
      String s = seq.getResidues( residueStart, seq.getLength() );
      if ( seq instanceof com.affymetrix.genoviz.datamodel.NASequence ) {
        this.paints = this.NAPAINTS;
      }
      else if ( seq instanceof com.affymetrix.genoviz.datamodel.AASequence ) {
        this.paints = this.AAPAINTS;
      }
      calculateColors( theColors, s, theConsensus, 0, consensusStart );
    }

  }

  public void calculateColors
    ( Color[] theColors,
      String theResidues,
      String theConsensus,
      int residueStart,
      int consensusStart )
  {

    if ( null == theConsensus ) { // Nothing to compare with.
      // Just give each residue it's standard color.
      calculateColors( theColors, theResidues, residueStart );
      return;
    }

    // Color white for a match and light gray for a mismatch.
    int i = 0, j;
    try {
    for ( i = 0, j = residueStart;
          i < theColors.length && j < theResidues.length();
          i++, j++ ) {
      theColors[i] = Color.lightGray;
      int ci = consensusStart + i;
      if ( ci < theConsensus.length() ) {
        if ( Character.toUpperCase( theConsensus.charAt( ci ) )
            == Character.toUpperCase( theResidues.charAt( j ) ) )
        {
          theColors[i] = Color.white;
        }
      }
    }
    } catch ( StringIndexOutOfBoundsException e ) {
      e.printStackTrace();
    }
    for ( ; i < theColors.length; i++ ) {
      theColors[i] = Color.lightGray;
    }

  }

}
