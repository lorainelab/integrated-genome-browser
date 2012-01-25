package com.affymetrix.igb.shared;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface ISearchModeGlyph extends ISearchMode {
	/**
	 * actually perform the search
	 * @param search_text the input text
	 * @param chrFilter the chromosome / seq to search or null for all
	 * @param statusHolder the status display for output messages
	 * @return a list of the glyphs found by the search
	 */
	public List<GlyphI> search(String search_text, final BioSeq chrFilter, IStatus statusHolder);
}
