package com.affymetrix.igb.search.mode;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.search.IStatus;
import com.affymetrix.igb.search.SearchView;

public class SearchModeProps extends SearchModeIDOrProps implements ISearchMode {
	public SearchModeProps(IGBService igbService) {
		super(igbService);
	}

	@Override
	public SearchResultsTableModel run(String search_text, BioSeq chrFilter, String seq, final boolean remote, IStatus statusHolder, List<GlyphI> glyphs) {
		return run(search_text, chrFilter, seq, true, remote, statusHolder, glyphs);
	}

	@Override
	public String getName() {
		return SearchView.BUNDLE.getString("searchRegexProps");
	}

	@Override
	public String getTooltip() {
		return SearchView.BUNDLE.getString("searchRegexPropsTF");
	}

	@Override
	public boolean useRemote() {
		return false;
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
