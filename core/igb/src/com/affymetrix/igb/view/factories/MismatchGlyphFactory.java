package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MisMatchPileupGraphSym;
import com.affymetrix.igb.graphTypes.MismatchGraphType;
import com.affymetrix.igb.graphTypes.MismatchPileupType;
import com.affymetrix.igb.shared.GraphGlyph;

/**
 *
 * @author hiralv
 */
public class MismatchGlyphFactory extends GraphGlyphFactory {

	@Override
	public String getName() {
		return "mismatch";
	}

	@Override
	protected void setGraphType(GraphSym newgraf, GraphState gstate, GraphGlyph graphGlyph) {
		graphGlyph.setGraphStyle(newgraf instanceof MisMatchPileupGraphSym ? new MismatchPileupType(graphGlyph) : new MismatchGraphType(graphGlyph));
	}
	
}
