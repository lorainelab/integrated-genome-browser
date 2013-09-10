package com.affymetrix.igb.shared;

import java.util.List;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface SearchListener {
	public void searchResults(String searchText, List<SeqSymmetry> symList);
}
