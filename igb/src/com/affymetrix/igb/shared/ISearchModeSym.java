package com.affymetrix.igb.shared;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.filter.SearchResult;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface ISearchModeSym extends ISearchMode {
	/**
	 * actually perform the search
	 * @param search_text the input text
	 * @param chrFilter the chromosome / seq to search or null for all
	 * @param statusHolder the status display for output messages
	 * @param option the value of an option for this mode
	 * @return a list of the syms found by the search
	 */
	public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean option);
	/**
	 * actually perform the search
	 * @param search_text the input text
	 * @param chrFilter the chromosome / seq to search or null for all
	 * @param statusHolder the status display for output messages
	 * @param option the value of an option for this mode
	 * @return a list of the search results found by the search
	 */
	public List<SearchResult> searchTrack(String search_text, final BioSeq chrFilter, TypeContainerAnnot contSym, IStatus statusHolder, boolean option);
	/**
	 * called when the user selects a row in the table
	 * @param sym the sym selected
	 */
	public void valueChanged(SeqSymmetry sym);
}
