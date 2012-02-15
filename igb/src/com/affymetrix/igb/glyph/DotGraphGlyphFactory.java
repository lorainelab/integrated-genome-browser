package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

public class DotGraphGlyphFactory extends AbstractGraphGlyphFactory {

	@Override
	public String getName() {
		return "dotgraph";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym graf, GraphState gstate) {
		return new DotGraphGlyph(graf, gstate);
	}
}
