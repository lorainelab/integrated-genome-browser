package com.affymetrix.igb.searchmodeidorprops;

import java.text.MessageFormat;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;

public class SearchModeID extends SearchModeIDOrProps implements ISearchModeSym {
	private static final int SEARCH_ALL_ORDINAL = -8000;
	private static final String REMOTESERVERSEARCH = BUNDLE.getString("optionCheckBox");
	private static final String REMOTESERVERSEARCHTOOLTIP = BUNDLE.getString("optionCheckBoxTT");
	private static final String REMOTESERVERSEARCHSINGULAR = BUNDLE.getString("remoteServerSearchSingular");
	private static final String REMOTESERVERSEARCHPLURAL = BUNDLE.getString("remoteServerSearchPlural");
	
	public SearchModeID(IGBService igbService) {
		super(igbService);
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
	public String getOptionName(int i) {
		String remoteServerPluralText = i == 1 ? REMOTESERVERSEARCHSINGULAR : REMOTESERVERSEARCHPLURAL;
		return MessageFormat.format(REMOTESERVERSEARCH, "" + i, remoteServerPluralText);
	}

	@Override
	public String getOptionTooltip(int i) {
		String remoteServerPluralText = i == 1 ? REMOTESERVERSEARCHSINGULAR : REMOTESERVERSEARCHPLURAL;
		return MessageFormat.format(REMOTESERVERSEARCHTOOLTIP, "" + i, remoteServerPluralText);
	}
	
	@Override
	public boolean getOptionEnable(int i) {
		return i > 0;
	}
	
	@Override
	public boolean useOption() {
		return true;
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
		return search(search_text, chrFilter, statusHolder, option, false);
	}
}
