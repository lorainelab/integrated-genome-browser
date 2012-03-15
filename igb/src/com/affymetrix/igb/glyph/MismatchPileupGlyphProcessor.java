package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MisMatchPileupGraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.GlyphProcessor;
import com.affymetrix.igb.shared.MismatchPileupGlyph;

public class MismatchPileupGlyphProcessor implements GlyphProcessor {
	public static final String PILEUP_IDENTIFIER = "mismatch pileup";

	@Override
	public void processGlyph(GlyphI glyph) {}

	@Override
	public AbstractGraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate) {
		if (sym instanceof MisMatchPileupGraphSym) {
			gstate.setGraphStyle(GraphType.STAIRSTEP_GRAPH);
			return new MismatchPileupGlyph(sym, gstate);
		}
		return null;
	}
}

