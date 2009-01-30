/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.genometry;

/**
 * Implementations model a biological sequence.
 * These would be things like DNA, RNA, and proteins.
 * The residues are letter codes for nucleotides or amino
 * acids.
 *
 */
public interface BioSeq {

  /**
   * Returns a string identifier of the sequence or null.  This identifier
   * is not guaranteed to be unique.
   *
   * @return a String identifier of the sequence or null
   */
  public String getID();

  // public int getMin();
  //  public int getMax();

  /**
   * Returns the number of residues in the sequence.  The return value is
   * undefined if the number of residues is greater than Integer.MAX_VALUE.
   *
   * @return the number of residues in the sequence
   */
  public int getLength();

  //  public int getLengthDouble();

  /**
   * Returns all residues on the sequence.
   *
   * @return a String containing all residues on the sequence
   */
  public String getResidues();

  /**
   * Returns the residues on the sequence between start and end.  Unknown if
   * implementations of this function are inclusive or exclusive on start and
   * end.
   *
   * @param  start the start index (inclusive?)
   * @param  end   the end index (exclusive?)
   * @return       a String containing residues between start and end
   */
  public String getResidues(int start, int end);
  
  /**
   * Returns true if all residues on the sequence are available.
   * 
   * @return true if all residues on the sequence are available
   */
  public boolean isComplete();
  /**
   * Returns true if all residues between start and end are available.  Unknown
   * if implementations of this function are inclusive or exclusive on start
   * and end.
   *
   * @param  start the start index (inclusive?)
   * @param  end   the end index (exclusive?)
   * @return       true if all residues betwen start and end are available
   */
  public boolean isComplete(int start, int end);
}
