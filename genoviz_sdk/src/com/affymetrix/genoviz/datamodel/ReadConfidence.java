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

package com.affymetrix.genoviz.datamodel;

import com.affymetrix.genoviz.util.Debug;

import java.lang.String;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * models a set of scores indicating the confidence
 * of each base called in a sequence
 * by a base-calling program like phred.
 */
public class ReadConfidence
{

  private Vector<BaseConfidence> qualityVector;
  private int maxConfidence = 0;

  /**
   * creates an empty ReadConfidence.
   */
  public ReadConfidence() {
    this.qualityVector = new Vector<BaseConfidence>();
  }

  /** @return the number of bases called (read). */
  public int getReadLength() {
    return this.qualityVector.size();
  }

  /** @return the confidence of the base called with the most confidence. */
  public int getMaxValue() {
    return this.maxConfidence;
  }

  /**
   * adds a BaseConfidence to this ReadConfidence.
   */
  public void addBaseConfidence( BaseConfidence theBase ) {
    this.qualityVector.addElement( theBase );
    this.maxConfidence = Math.max( this.maxConfidence, theBase.getConfidence() );
  }

  /**
   * sets the bases.
   * The quality scores should have already been set.
   * A base is set for each already set quality score.
   * If the array of bases is too short
   * i.e. there are more of them than there are quality scores,
   * then the rest of the bases are set to "-".
   * If the array of bases is too long,
   * then the extra bases are ignored.
   *
   * @param theNewBases the array of bases to assign.
   */
  public void setBaseArray(char[] theNewBases) {
    Debug.test( null != qualityVector,
      "Tried to set bases without quality scores.");
    int ourBases = Math.min( theNewBases.length, this.qualityVector.size() );
    if ( theNewBases.length < this.qualityVector.size() ) {
      System.err.println("setBaseArray: Not enough bases. Filling in with \"-\".");
      System.err.println("              "
        + theNewBases.length + " < " + this.qualityVector.size() );
    }
    if (this.qualityVector.size() < theNewBases.length ) {
      System.err.println( "setBaseArray: Too many bases. Ignoring the extras." );
      System.err.println( "              "
        + this.qualityVector.size() + " < " + theNewBases.length );
    }
    try {
      for (int i = 0; i < ourBases; i++ ) {
        BaseConfidence bc = getBaseConfidenceAt( i );
        bc.setBase( theNewBases[i] );
      }
      for (int i = ourBases; i < this.qualityVector.size(); i++) {
        BaseConfidence bc = getBaseConfidenceAt( i );
        bc.setBase( '-' );
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
      System.err.println("There are more bases than quality scores.");
      System.err.println("  Ignoring extra bases.");
    }
  }

  /** @return an array of the bases called. */
  public char[] getBaseArray() {
    char[] b = new char[this.qualityVector.size()];
    for ( int i = 0; i < b.length; i++ ) {
      b[i] = ((BaseConfidence)this.qualityVector.elementAt(i)).getBase();
    }
    return b;
  }

  /** @return the bases called. */
  public String getBaseString() {
    char[] b = this.getBaseArray();
    return new String( b );
  }

  /** @return the quality scores. */
  public int[] getQualArray() {
    int[] q = new int[this.qualityVector.size()];
    for ( int i = 0; i < q.length; i++ ) {
      q[i] = ((BaseConfidence)this.qualityVector.elementAt(i)).getConfidence();
    }
    return q;
  }

  /**
   * @param index of the base called.
   * @return a BaseConfidence (including score) of that base.
   */
  public BaseConfidence getBaseConfidenceAt(int index) {
    return (BaseConfidence) this.qualityVector.elementAt(index);
  }

  /**
   * @return an array of base calls
   */
  public BaseCall[] getBaseCalls() {
    BaseCall[] array = new BaseCall[this.qualityVector.size()];
    this.qualityVector.copyInto( array );
    return array;
  }

}
