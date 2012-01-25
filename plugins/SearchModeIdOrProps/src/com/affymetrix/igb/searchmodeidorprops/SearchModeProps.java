package com.affymetrix.igb.searchmodeidorprops;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResultsTableModel;

public class SearchModeProps extends SearchModeIDOrProps implements ISearchMode {
	private static final int SEARCH_ALL_ORDINAL = 3000;
	public SearchModeProps(IGBService igbService) {
		super(igbService);
	}

	@Override
	public SearchResultsTableModel run(String search_text, BioSeq chrFilter, String seq, final boolean remote, IStatus statusHolder) {
		return run(search_text, chrFilter, seq, true, remote, statusHolder);
	}

	@Override
	public String getName() {
		return BUNDLE.getString("searchRegexProps");
	}

	@Override
	public String getTooltip() {
		return BUNDLE.getString("searchRegexPropsTF");
	}

	public String getOptionName(int i) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getOptionTooltip(int i) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	public boolean getOptionEnable(int i) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public boolean useOption() {
		return false;
	}

	@Override
	public boolean useGenomeInSeqList() {
		return true;
	}

	@Override
	public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
		return findSpans(findLocalSyms(search_text, null, Constants.GENOME_SEQ_ID, true, DUMMY_STATUS));
	}

	@Override
	public int searchAllUse() {
		return SEARCH_ALL_ORDINAL;
	}

	@Override
	public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder) {
		return findLocalSyms(search_text, chrFilter, (chrFilter == null) ? "genome" : chrFilter.getID(), true, statusHolder);
	}
}
