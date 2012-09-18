package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface GlyphProcessor {
	public void processGlyph(GlyphI glyph, BioSeq seq);
}
