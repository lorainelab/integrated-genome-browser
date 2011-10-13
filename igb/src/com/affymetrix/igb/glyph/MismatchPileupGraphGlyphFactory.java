package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.igb.shared.ExtendedMapViewGlyphFactoryI;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MismatchPileupGlyph;

/**
 *
 * @author hiralv
 */
public class MismatchPileupGraphGlyphFactory extends AbstractMismatchGraphGlyphFactory implements ExtendedMapViewGlyphFactoryI {
	public String getName(){
		return "mismatch pileup";
	}

	@Override
	protected GraphGlyph getGraphGlyph(MisMatchGraphSym gsym, GraphState state) {
		return new MismatchPileupGlyph(gsym, state);
	}
}
