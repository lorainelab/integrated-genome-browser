package com.affymetrix.igb.shared;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface ISearchModeSym extends ISearchMode {
	/**
	 * actually perform the search
	 * @param search_text the input text
	 * @param chrFilter the chromosome / seq to search or null for all
	 * @param statusHolder the status display for output messages
	 * @return a list of the syms found by the search
	 */
	public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder);
}
