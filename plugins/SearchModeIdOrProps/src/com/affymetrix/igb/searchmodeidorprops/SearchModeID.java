package com.affymetrix.igb.searchmodeidorprops;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResultsTableModel;

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
		return BUNDLE.getString("searchRegexIDOrName");
	}

	@Override
	public String getTooltip() {
		return BUNDLE.getString("searchRegexIDOrNameTF");
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

	@Override
	public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
		return findSpans(findLocalSyms(search_text, null, igbService.getGenomeSeqId(), false, DUMMY_STATUS));
	}
}
