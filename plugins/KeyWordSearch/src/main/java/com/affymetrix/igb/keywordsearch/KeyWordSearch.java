package com.affymetrix.igb.keywordsearch;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.lorainelab.igb.services.search.ISearchMode;
import com.lorainelab.igb.services.search.ISearchModeSym;
import com.lorainelab.igb.services.search.IStatus;
import com.lorainelab.igb.services.search.SearchModeRegistry;
import com.lorainelab.igb.services.search.SearchResults;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author hiralv
 */
@Component(name = KeyWordSearch.COMPONENT_NAME, provide = ISearchMode.class, immediate = true)
public class KeyWordSearch implements ISearchModeSym {

    public static final String COMPONENT_NAME = "KeyWordSearch";
    private static final int SEARCH_ALL_ORDINAL = 1;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("keywordsearch");

    private SearchModeRegistry searchModeRegistry;

    public KeyWordSearch() {

    }

    public String getName() {
        return BUNDLE.getString("searchKeyWord");
    }

    public int searchAllUse() {
        return SEARCH_ALL_ORDINAL;
    }

    public String getTooltip() {
        return BUNDLE.getString("searchTooltip");
    }

    public boolean useGenomeInSeqList() {
        return true;
    }

    public String checkInput(String search_text, BioSeq vseq, String seq) {
        return null;
    }

    @Override
    public SearchResults<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean option) {
        Set<SeqSymmetry> results = new LinkedHashSet<>();
        StringBuilder status = new StringBuilder();
        StatusHolder sh = new StatusHolder(statusHolder);
        for (ISearchModeSym searchMode : SearchModeRegistry.getSearchModeSyms()) {
            if (searchMode != this) {
                statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchSearching"), searchMode.getName(), search_text));
                SearchResults<SeqSymmetry> searchResults = searchMode.search(search_text, chrFilter, sh, option);
                List<SeqSymmetry> res = searchResults != null ? searchResults.getResults() : null;
                if (res != null && !res.isEmpty()) {
                    results.addAll(res);
                }
                status.append(searchMode.getName()).append(" : ").append(sh.getLastStatus()).append(", ");
            }
        }
        statusHolder.setStatus(status.toString());
        return new SearchResults<>(getName(), search_text, chrFilter != null ? chrFilter.getId() : "genome", status.toString(), results.stream().collect(Collectors.toList()));
    }

    @Override
    public List<SeqSymmetry> searchTrack(String search_text, TypeContainerAnnot contSym) {
        List<SeqSymmetry> results = new ArrayList<>();
        for (ISearchModeSym searchMode : SearchModeRegistry.getSearchModeSyms()) {
            if (searchMode != this) {
                List<SeqSymmetry> res = searchMode.searchTrack(search_text, contSym);
                if (res != null && !res.isEmpty()) {
                    results.addAll(res);
                }
            }
        }
        return results;
    }

    private static class StatusHolder implements IStatus {

        private String lastStatus;
        IStatus internalSH;

        public StatusHolder(IStatus internalSH) {
            this.internalSH = internalSH;
        }

        @Override
        public void setStatus(String text) {
            internalSH.setStatus(text);
            lastStatus = text;
        }

        String getLastStatus() {
            return lastStatus;
        }
    }

}
