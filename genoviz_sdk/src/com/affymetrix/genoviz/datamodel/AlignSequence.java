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

/**
 * models a sequence aligned to another sequence.
 * To align to the other sequence,
 * this sequence can contain gaps.
 */
public class AlignSequence extends Sequence  {

  private int alignEnd;
  private static final int GAP_CHAR = MultiSeqAlign.GAP_CHAR;

  public void setAlignEnd(int alignEnd) {
    this.alignEnd = alignEnd;
  }

  public int getAlignEnd() {
    return alignEnd;
  }

  /**
   * gets the n'th residue of the sequence.
   *
   * @return the n'th residue of the underlying sequence
   *         when n is not in the initial or final "gap".
   *         the gap character otherwise.
   */
  public char getResidue (int n) {
    if (n >= start && n < this.start + this.length) {
      return super.getResidue(n);
    } else {
      return (char) GAP_CHAR;
    }
  }

  /**
   * sets residues
   * and adjusts the start and end
   * to avoid leading and trailing gap characters.
   *
   * @param new_residues a string representing residues and
   * and gaps.
   */
  public void setResidues(String new_residues) {
    int i=0;
    int j=new_residues.length();

    while (i < new_residues.length() && GAP_CHAR == new_residues.charAt(i)) {
      i++;
    }
    while (j > i && GAP_CHAR == new_residues.charAt(j - 1)) {
      j--;
    }

    super.setResidues(new_residues.substring(i, j));
    setStart(i);
    setAlignEnd(new_residues.length());
  }

  /**
   * appends residues and adjusts the start and end,
   * if needed.
   *
   * @param new_residues a string representing residues and
   * and gaps to be added to the end of this sequence.
   */
  public void appendResidues(String new_residues) {

    int i=0;
    int j=new_residues.length();

    if (this.length == 0) {
      while (i < j && GAP_CHAR == new_residues.charAt(i)) {
        i++;
      }
      this.start += i;
    } else {
      while (j > 0 && GAP_CHAR == new_residues.charAt(j - 1)) {
        j--;
      }
    }

    if (i < j) {
      int k = this.start + this.length;
      while (k < alignEnd) {
        k++;
        super.appendResidue(' ');
      }
      super.appendResidues(new_residues.substring(i, j));
    }
    alignEnd += new_residues.length();
  }

  /**
   * inserts residues into the middle of this sequence.
   * <em>Note:</em> not yet implemented.
   *
   * @param start indicates where the new residues are to be inserted.
   * @param new_residues a string representing residues and
   * and gaps to be inserted in this sequence.
   */
  public void insertResidues(int start, String new_residues) {
    throw new IllegalArgumentException("can't insert yet!");
  }

  /** @return a string representing this AlignSequence. */
  public String toString() {
    String s = super.toString() + getResidues();
    return s;
  }


}
