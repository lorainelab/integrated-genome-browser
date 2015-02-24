package com.affymetrix.igb.shared;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

public interface SearchListener {

    public void searchResults(SearchResults<SeqSymmetry> searchResults);
}
