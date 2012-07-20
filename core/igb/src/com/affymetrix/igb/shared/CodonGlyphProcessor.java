package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.*;
import com.affymetrix.genoviz.bioviews.GlyphI;

public class CodonGlyphProcessor implements GlyphProcessor {
	private CodonGlyph saveCodonGlyph;
	@Override
	/*
	 * note - if the transcript has UTR, it will have two glyphs, one with the UTR and one without.
	 * the one without should be used, but it is created first, so it would be covered by the one
	 * with. So this kludge causes the one without to not be drawn on its turn, but the one with
	 * will draw the one without on its turn.
	 */
	public void processGlyph(GlyphI glyph) {
		if (glyph.getParent() != null && 
				(glyph.getParent().getInfo() instanceof UcscGeneSym || glyph.getParent().getInfo() instanceof UcscBedSym)) {
			CodonGlyph codonGlyph = new CodonGlyph();
			if (hasUTR((SymSpanWithCds)glyph.getParent().getInfo(), (SeqSymmetry)glyph.getInfo())) {
				if (saveCodonGlyph != null) {
					codonGlyph.setDrawCodonGlyph(saveCodonGlyph);
				}
				saveCodonGlyph = null;
			}
			else {
				saveCodonGlyph = codonGlyph;
			}
			glyph.addChild(codonGlyph);
		}
	}
	private boolean hasUTR(SymSpanWithCds parentSym, SeqSymmetry exonSym) {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		SeqSpan cdsSpan = parentSym.getCdsSpan();
		if (cdsSpan != null) {
		if (parentSym.isForward() && exonSym.getSpan(seq) != null &&
			(cdsSpan.getStart() > exonSym.getSpan(seq).getStart() ||
			 cdsSpan.getEnd() < exonSym.getSpan(seq).getEnd())) {
				return true;
		}
		if (!parentSym.isForward() && exonSym.getSpan(seq) != null &&
			(cdsSpan.getStart() < exonSym.getSpan(seq).getStart() ||
			 cdsSpan.getEnd() > exonSym.getSpan(seq).getEnd())) {
				return true;
			}
		}
		return false;
	}
	@Override
	public AbstractGraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate) {
		return null;
	}
}
