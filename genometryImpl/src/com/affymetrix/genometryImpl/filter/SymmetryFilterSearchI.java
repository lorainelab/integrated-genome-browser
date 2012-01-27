package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface SymmetryFilterSearchI extends SymmetryFilterI {
	public SearchResult searchSymmetry(SeqSymmetry sym);
}
