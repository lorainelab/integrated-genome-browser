package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

public class HeatMapGraphGlyphFactory extends AbstractGraphGlyphFactory {

	@Override
	public String getName() {
		return "heatmapgraph";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym graf, GraphState gstate) {
		return new HeatMapGraphGlyph(graf, gstate);
	}
}
