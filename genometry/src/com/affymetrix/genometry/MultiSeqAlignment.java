/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

public interface MultiSeqAlignment extends BioSeq {

  /**
   *  Return the number of sequences in the multiple sequence alignment.
   */
  int getAlignmentCount();

  /**
   *  Returns a the BioSeq with the given index.
   *  <p>
   *  Want to allow the possibility of <em>any</em> BioSeq being used as sequence
   *  in alignment, hence requiring CompositeBioSeq return type.
   */
  CompositeBioSeq getAlignedSeq(int index);

  /**
   *  Retrieve the CompositeBioSeq (essentially a pairwise alignment
   *  of seq to alignment coords) that is based on the specified BioSeq.
   */
  CompositeBioSeq getAlignedSeq(BioSeq seq);
}

