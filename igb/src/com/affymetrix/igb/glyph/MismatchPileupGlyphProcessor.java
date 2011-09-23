package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.GlyphProcessor;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MismatchPileupGlyph;

/**
 *
 * @author lfrohman
 */
public class MismatchPileupGlyphProcessor implements GlyphProcessor {
	public static final String PILEUP_IDENTIFIER = "mismatch pileup";

	@Override
	public void processGlyph(GlyphI glyph) {}

	@Override
	public GraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate) {
		if (sym instanceof MisMatchGraphSym && sym.getID().startsWith(PILEUP_IDENTIFIER)) {
			gstate.setGraphStyle(GraphType.STAIRSTEP_GRAPH);
			return new MismatchPileupGlyph(sym, gstate);
		}
		return null;
	}
}

