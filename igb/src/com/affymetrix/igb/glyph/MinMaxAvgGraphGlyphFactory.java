package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

public class MinMaxAvgGraphGlyphFactory extends AbstractGraphGlyphFactory {

	@Override
	public String getName() {
		return "minmaxavggraph";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym graf, GraphState gstate) {
		return new MinMaxAvgGraphGlyph(graf, gstate);
	}
}
