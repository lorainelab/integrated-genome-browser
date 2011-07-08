package com.affymetrix.igb.search.mode;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.search.IStatus;
import com.affymetrix.igb.search.SearchView;

public class SearchModeID extends SearchModeIDOrProps implements ISearchMode {
	public SearchModeID(IGBService igbService) {
		super(igbService);
	}

	@Override
	public SearchResultsTableModel run(String search_text, BioSeq chrFilter, String seq, final boolean remote, IStatus statusHolder, List<GlyphI> glyphs) {
		return run(search_text, chrFilter, seq, false, remote, statusHolder, glyphs);
	}

	@Override
	public String getName() {
		return SearchView.BUNDLE.getString("searchRegexIDOrName");
	}

	@Override
	public String getTooltip() {
		return SearchView.BUNDLE.getString("searchRegexIDOrNameTF");
	}

	@Override
	public boolean useRemote() {
		return true;
	}

	@Override
	public boolean useDisplaySelected() {
		return true;
	}

	@Override
	public boolean useGenomeInSeqList() {
		return true;
	}
}
