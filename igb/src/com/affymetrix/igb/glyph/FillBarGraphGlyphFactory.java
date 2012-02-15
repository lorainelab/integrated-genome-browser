package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

public class FillBarGraphGlyphFactory extends AbstractGraphGlyphFactory {

	@Override
	public String getName() {
		return "fillbargraph";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym graf, GraphState gstate) {
		return new FillBarGraphGlyph(graf, gstate);
	}
}
