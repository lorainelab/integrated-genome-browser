package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface GlyphProcessor {
	public void processGlyph(GlyphI glyph);
	public AbstractGraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate);
}
