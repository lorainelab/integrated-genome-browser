package com.affymetrix.igb.util;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.igb.shared.ISearchMode;


public class SearchModeHolder {
	private static SearchModeHolder instance = new SearchModeHolder();
	private SearchModeHolder() {
		super();
	}
	public static SearchModeHolder getInstance() {
		return instance;
	}
	private List<ISearchMode> searchModes = new ArrayList<ISearchMode>();

	public void addSearchMode(ISearchMode searchMode) {
		searchModes.add(searchMode);
	}

	public void removeSearchMode(ISearchMode searchMode) {
		searchModes.remove(searchMode);
	}

	public List<ISearchMode> getSearchModes() {
		return searchModes;
	}
}
