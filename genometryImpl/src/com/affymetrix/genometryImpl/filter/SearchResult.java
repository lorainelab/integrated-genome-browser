package com.affymetrix.genometryImpl.filter;

import java.util.List;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class SearchResult {
	private final SeqSymmetry sym;
	private final List<String> searchTerms;
	private final int ranking;
	public SearchResult(SeqSymmetry sym, List<String> searchTerms, int ranking) {
		super();
		this.sym = sym;
		this.searchTerms = searchTerms;
		this.ranking = ranking;
	}
	public SeqSymmetry getSym() {
		return sym;
	}
	public List<String> getSearchTerms() {
		return searchTerms;
	}
	public int getRanking() {
		return ranking;
	}
}
