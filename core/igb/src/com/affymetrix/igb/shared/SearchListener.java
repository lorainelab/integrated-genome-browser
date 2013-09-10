package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface SearchListener {
	public void searchResults(SearchResults<SeqSymmetry> searchResults);
}
