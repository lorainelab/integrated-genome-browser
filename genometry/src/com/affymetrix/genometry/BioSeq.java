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
  public String getID();
  // public int getMin();
  //  public int getMax();
  public int getLength();
  //  public int getLengthDouble();
  public String getResidues();
  public String getResidues(int start, int end);
  public boolean isComplete();
  public boolean isComplete(int start, int end);
}
