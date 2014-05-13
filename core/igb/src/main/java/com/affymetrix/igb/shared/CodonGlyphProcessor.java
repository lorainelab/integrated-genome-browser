package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymSpanWithCds;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.Application;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class CodonGlyphProcessor {
	private int codeSize;
	
	PreferenceChangeListener prefs = new PreferenceChangeListener(){

		@Override
		public void preferenceChange(PreferenceChangeEvent pce) {
			if (!pce.getNode().equals(PreferenceUtils.getTopNode())) {
				return;
			}
			if (pce.getKey().equals(CodonGlyph.CODON_GLYPH_CODE_SIZE)) {
				int prevCodeSize = codeSize;
				codeSize = PreferenceUtils.getIntParam(CodonGlyph.CODON_GLYPH_CODE_SIZE, CodonGlyph.default_codon_glyph_code_size);
				if(prevCodeSize != codeSize){
					ThreadUtils.runOnEventQueue(new Runnable(){
						@Override
						public void run() {
							Application.getSingleton().getMapView().setAnnotatedSeq(Application.getSingleton().getMapView().getAnnotatedSeq(), true, true);
						}
					});
				}
			}
		}
	};
	
	public CodonGlyphProcessor(){
		codeSize = PreferenceUtils.getIntParam(CodonGlyph.CODON_GLYPH_CODE_SIZE, CodonGlyph.default_codon_glyph_code_size);
		PreferenceUtils.getTopNode().addPreferenceChangeListener(prefs);
	}
	
	/*
	 * TODO: Remove dependency on glyph parent.
	 * 
	 * note - if the transcript has UTR, it will have two glyphs, one with the UTR and one without.
	 * the one without should be used, but it is created first, so it would be covered by the one
	 * with. So this kludge causes the one without to not be drawn on its turn, but the one with
	 * will draw the one without on its turn.
	 */
	public void processGlyph(GlyphI glyph, SeqSymmetry exonSym, BioSeq seq) {
		if (glyph.getParent() != null && glyph.getParent().getInfo() instanceof SymSpanWithCds && codeSize != 0) {	
			if (!hasUTR((SymSpanWithCds)glyph.getParent().getInfo(), exonSym, seq)) {
				CodonGlyph codonGlyph = new CodonGlyph(codeSize);
				codonGlyph.setHitable(false);
				codonGlyph.setInfo(exonSym);
				codonGlyph.setCoordBox(glyph.getCoordBox());
				codonGlyph.setBackgroundColor(glyph.getBackgroundColor());
				glyph.addChild(codonGlyph);
			}
		}
	}
	
	private static boolean hasUTR(SymSpanWithCds parentSym, SeqSymmetry exonSym, BioSeq seq) {
		SeqSpan cdsSpan = parentSym.getCdsSpan();
		if (cdsSpan != null) {
			if (parentSym.isForward() && exonSym.getSpan(seq) != null
					&& (cdsSpan.getStart() > exonSym.getSpan(seq).getStart()
					|| cdsSpan.getEnd() < exonSym.getSpan(seq).getEnd())) {
				return true;
			}
			if (!parentSym.isForward() && exonSym.getSpan(seq) != null
					&& (cdsSpan.getStart() < exonSym.getSpan(seq).getStart()
					|| cdsSpan.getEnd() > exonSym.getSpan(seq).getEnd())) {
				return true;
			}
		}
		return parentSym.isCdsStartStopSame();
	}

}
