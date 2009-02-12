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

/**
 * Extensions to the BioSeq interface which stores the residues as smaller
 * sequences internally.
 */
public interface CompositeBioSeq extends BioSeq {

	/**
	 * Returns the SeqSymmetry which represents the residues in the sequence.
	 *
	 * @return the SeqSymmetry which represents the residues in the sequence
	 */
	public SeqSymmetry getComposition();

	/**
	 * Sets the SeqSymmetry which represents the residues in the sequence.
	 *
	 * @param comp the SeqSymmetry which represents the residues in the sequence
	 */
	public void setComposition(SeqSymmetry comp);
}
