package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

public class StairStepGraphGlyphFactory extends AbstractGraphGlyphFactory {

	@Override
	public String getName() {
		return "stairstepgraph";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym graf, GraphState gstate) {
		return new StairStepGraphGlyph(graf, gstate);
	}
}
