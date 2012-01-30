package com.affymetrix.genometryImpl.filter;

import java.util.Map;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class SearchResult {
	private final SeqSymmetry sym;
	private final Map<String, String> searchTerms;
	private final int ranking;
	public SearchResult(SeqSymmetry sym, Map<String, String> searchTerms, int ranking) {
		super();
		this.sym = sym;
		this.searchTerms = searchTerms;
		this.ranking = ranking;
	}
	public SeqSymmetry getSym() {
		return sym;
	}
	public Map<String, String> getSearchTerms() {
		return searchTerms;
	}
	public int getRanking() {
		return ranking;
	}
}
