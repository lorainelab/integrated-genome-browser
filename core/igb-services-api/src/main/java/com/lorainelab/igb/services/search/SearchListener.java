package com.lorainelab.igb.services.search;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

public interface SearchListener {

    public void searchResults(SearchResults<SeqSymmetry> searchResults);
}
