package org.lorainelab.igb.igb.services.search;

import com.affymetrix.genometry.BioSeq;

/**
 * represents a method of searching the selected features given an input string
 * and options
 */
public interface ISearchMode {

    /**
     * @return the name of the search mode
     */
    public String getName();

    /**
     * @return the search mode order to be used in combined searches, negative
     * means don't use
     */
    public int searchAllUse();

    /**
     * @return the tooltip text
     */
    public String getTooltip();

    /**
     * @return if whole genome is allowed instead of selecting a chromosome
     */
    public boolean useGenomeInSeqList();

    /**
     * verify the user input for this search
     *
     * @param search_text the input text
     * @param vseq the chromosome / seq to search
     * @param seq the chromosome / seq to search
     * @return the error message or null for no error
     */
    public String checkInput(String search_text, BioSeq vseq, String seq);
}
