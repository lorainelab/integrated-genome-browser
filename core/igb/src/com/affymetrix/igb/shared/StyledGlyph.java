package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;

/**
 *
 * @author hiralv
 */
public interface StyledGlyph extends GlyphI {
	public ITrackStyleExtended getAnnotStyle();
	public FileTypeCategory getFileTypeCategory();
}
