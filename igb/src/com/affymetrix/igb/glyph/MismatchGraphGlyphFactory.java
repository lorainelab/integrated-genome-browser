package com.affymetrix.igb.glyph;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.MisMatchGraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;

/**
 *
 * @author hiralv
 */
public class MismatchGraphGlyphFactory extends AbstractMismatchGraphGlyphFactory implements MapViewGlyphFactoryI {
	public String getName(){
		return "mismatch";
	}

	@Override
	protected GraphGlyph getGraphGlyph(MisMatchGraphSym gsym, GraphState state) {
		return new GraphGlyph(gsym, state);
	}

	@Override
	protected MisMatchGraphSym getMismatchGraph(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int start, int end) {
		return SeqSymSummarizer.getMismatchGraph(syms, seq, false, id, start, end, false);
	}
}
