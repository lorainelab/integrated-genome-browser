package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface GlyphProcessor {
	public void processGlyph(GlyphI glyph);
	public GraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate);
}
