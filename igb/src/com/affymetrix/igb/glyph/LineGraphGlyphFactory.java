package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

public class LineGraphGlyphFactory extends AbstractGraphGlyphFactory {

	@Override
	public String getName() {
		return "linegraph";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym graf, GraphState gstate) {
		return new LineGraphGlyph(graf, gstate);
	}
}
