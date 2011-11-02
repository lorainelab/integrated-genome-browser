package com.affymetrix.igb.glyph;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.MisMatchGraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
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

	@Override
	protected MisMatchGraphSym getMismatchGraph(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int start, int end) {
		return SeqSymSummarizer.getMismatchGraph(syms, seq, false, id, start, end, true);
	}
}
