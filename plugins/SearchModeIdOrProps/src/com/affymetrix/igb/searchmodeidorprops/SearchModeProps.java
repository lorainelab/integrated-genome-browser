package com.affymetrix.igb.searchmodeidorprops;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;

public class SearchModeProps extends SearchModeIDOrProps implements ISearchModeSym {
	private static final int SEARCH_ALL_ORDINAL = -8000;
	public SearchModeProps(IGBService igbService) {
		super(igbService);
	}

	@Override
	public String getName() {
		return BUNDLE.getString("searchRegexProps");
	}

	@Override
	public String getTooltip() {
		return BUNDLE.getString("searchRegexPropsTF");
	}

	@Override
	public String getOptionName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getOptionTooltip() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public boolean getOptionEnable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public void setOptionState(boolean selected){
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public boolean getOptionState(){
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
	public int searchAllUse() {
		return SEARCH_ALL_ORDINAL;
	}

	@Override
	public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean option) {
		return search(search_text, chrFilter, statusHolder, option, true);
	}
}
