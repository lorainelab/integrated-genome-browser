package com.affymetrix.igb.searchmodeidorprops;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResults;

@Component(name = SearchModeProps.COMPONENT_NAME, provide = ISearchModeSym.class, immediate = true)
public class SearchModeProps extends SearchModeIDOrProps implements ISearchModeSym {

    public static final String COMPONENT_NAME = "SearchModeProps";
    private static final int SEARCH_ALL_ORDINAL = -8000;

    public SearchModeProps() {
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
    public boolean useGenomeInSeqList() {
        return true;
    }

    @Override
    public int searchAllUse() {
        return SEARCH_ALL_ORDINAL;
    }

    @Override
    public SearchResults<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean option) {
        return search(search_text, chrFilter, statusHolder, option, true);
    }
}
