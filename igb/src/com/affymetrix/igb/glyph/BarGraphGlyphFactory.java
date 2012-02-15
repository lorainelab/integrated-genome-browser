package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

public class BarGraphGlyphFactory extends AbstractGraphGlyphFactory {

	@Override
	public String getName() {
		return "bargraph";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym graf, GraphState gstate) {
		return new BarGraphGlyph(graf, gstate);
	}
}
