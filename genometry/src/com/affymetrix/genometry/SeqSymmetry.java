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
 * Implementations model a collection of {@link SeqSpan}s.
 * SeqSymmetries are also a collection of SeqSymmetries.
 * Each one can have zero or more children.
 */
public interface SeqSymmetry {
  public String getID();
  public int getSpanCount();
  public SeqSpan getSpan(BioSeq seq);
  public SeqSpan getSpan(int index);
  public boolean getSpan(BioSeq seq, MutableSeqSpan span);
  public boolean getSpan(int index, MutableSeqSpan span);
  public BioSeq getSpanSeq(int index);
  public int getChildCount();
  public SeqSymmetry getChild(int index);

  // Removed getParent() to allow for possibility of same symmetry being used in multiple 
  //      hiearchies (having multiple parents)
  //   public SeqSymmetry getParent();

  // Methods to improve efficiency for "compressed" SeqSymmetry implementations
  //  public boolean getChildSpan(int child_index, BioSeq seq, MutableSeqSpan span);
  //  public boolean getChildSpan(int child_index, int child_span_index, MutableSeqSpan span);


}
