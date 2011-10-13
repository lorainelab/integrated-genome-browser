package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.igb.shared.ExtendedMapViewGlyphFactoryI;
import com.affymetrix.igb.shared.GraphGlyph;

/**
 *
 * @author hiralv
 */
public class MismatchGraphGlyphFactory extends AbstractMismatchGraphGlyphFactory  implements ExtendedMapViewGlyphFactoryI {
	public String getName(){
		return "mismatch";
	}

	@Override
	protected GraphGlyph getGraphGlyph(MisMatchGraphSym gsym, GraphState state) {
		return new GraphGlyph(gsym, state);
	}
}
