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
	 * @param option the value of an option for this mode
	 * @return a list of the glyphs found by the search
	 */
	public List<GlyphI> search(String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean option);
	/**
	 * called when the user selects a row in the table
	 * @param glyph the glyph selected
	 * @param seq the seq
	 */
	public void valueChanged(GlyphI glyph, String seq);
}
