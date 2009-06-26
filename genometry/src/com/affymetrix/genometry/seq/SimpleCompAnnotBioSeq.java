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

package com.affymetrix.genometry.seq;

import com.affymetrix.genometry.util.DNAUtils;



//import checkers.nullness.quals.*;

public abstract class SimpleCompAnnotBioSeq
	extends CompositeNegSeq
	 {
	// GAH 8-14-2002: need a residues field in case residues need to be cached
	// (rather than derived from composition), or if we choose to store residues here
	// instead of in composition seqs in case we actually want to compose/cache
	// all residues...
	protected String residues;


	public SimpleCompAnnotBioSeq(String id, int length)  {
		super(id, length);
	}

	@Override
	public String getResidues(int start, int end, char fillchar) {
		int residue_length = this.getLength();
		if (start < 0 || residue_length <= 0) {
			throw new IllegalArgumentException("start: " + start + " residues: " + this.getResidues());
		}

		// Sanity checks on argument size.
		start = Math.min(start, residue_length);
		end = Math.min(end, residue_length);
		if (start <= end) {
			end = Math.min(end, start+residue_length);
		}
		else {
			start = Math.min(start, end+residue_length);
		}

		if (residues == null) {
			return super.getResidues(start, end, fillchar);
		}

		if (start <= end) {
			return residues.substring(start, end);
		}

		// start > end -- that means reverse complement.
		return DNAUtils.reverseComplement(residues.substring(end, start));
	}

	@Override
	public boolean isComplete(int start, int end) {
		if (residues != null) { return true; }
		else  { return super.isComplete(start, end); }
	}
}


