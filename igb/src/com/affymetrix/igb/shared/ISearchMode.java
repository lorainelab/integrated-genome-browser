package com.affymetrix.igb.shared;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 * represents a method of searching the selected features
 * given an input string and options
 */
public interface ISearchMode {
	public static final int NO_ZOOM_SPOT = -1;
	/**
	 * @return the name of the search mode
	 */
	public String getName();
	/**
	 * @return the search mode order to be used in combined searches, negative means don't use
 	 */
	public int searchAllUse();
	/**
	 * @return the tooltip text
	 */
	public String getTooltip();
	/**
	 * @param i the index of the option
	 * @return the name of the option at the specified index
	 */
	public String getOptionName(int i);
	/**
	 * @param i the index of the option
	 * @return the tooltip of the option at the specified index
	 */
	public String getOptionTooltip(int i);
	/**
	 * @param i the index of the option
	 * @return if the option at the specified index is enabled
	 */
	public boolean getOptionEnable(int i);
	/**
	 * @return if options are used for this search mode
	 */
	public boolean useOption();
	/**
	 * clear results
	 */
	public void clear();
	public boolean useDisplaySelected();
	/**
	 * @return if whole genome is allowed instead of selecting a chromosome
	 */
	public boolean useGenomeInSeqList();
	/**
	 * verify the user input for this search
	 * @param search_text the input text
	 * @param vseq the chromosome / seq to search
	 * @param seq the chromosome / seq to search
	 * @return the error message or null for no error
	 */
	public String checkInput(String search_text, BioSeq vseq, String seq);
	/**
	 * @return an empty result table
	 */
	public SearchResultsTableModel getEmptyTableModel();
	/**
	 * actually perform the search
	 * @param search_text the input text
	 * @param chrFilter the chromosome / seq to search or null for all
	 * @param statusHolder the status display for output messages
	 * @return a list of the syms found by the search
	 */
	public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder);
	/**
	 * run the search and fill the output table
	 * @param search_text the input text
	 * @param chrFilter the chromosome / seq to search or null for all
	 * @param seq the seq (redundant)
	 * @param remote if this search has remote selected
	 * @param statusHolder the status display for output messages
	 * @return the display table of results
	 */
	public SearchResultsTableModel run(String search_text, BioSeq chrFilter, String seq, boolean remote, IStatus statusHolder);
	/**
	 * called when the search is done
	 * @param vseq the seq
	 */
	public void finished(BioSeq vseq);
	/**
	 * called when the user selects a row in the table
	 * @param model the table model
	 * @param srow the selected
	 */
	public void valueChanged(SearchResultsTableModel model, int srow);
	public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan);
	public int getZoomSpot(String search_text);
}
