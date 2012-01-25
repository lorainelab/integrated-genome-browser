package com.affymetrix.igb.shared;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;

/**
 * represents a method of searching the selected features
 * given an input string and options
 */
public interface ISearchMode {
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
	 * called when the search is done
	 * @param vseq the seq
	 */
	public void finished(BioSeq vseq);
	public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan);
}
