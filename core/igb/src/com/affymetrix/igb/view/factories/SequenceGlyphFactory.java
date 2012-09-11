package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;

public class SequenceGlyphFactory extends MapTierGlyphFactoryA {
			
	@Override
	public void createGlyphs(SeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv) {
		if (sym != null) {
			TierGlyph tierGlyph = smv.getTrack(style, Direction.NONE);
			SimpleSymWithResidues childSym = (SimpleSymWithResidues) sym.getChild(0);
			SeqSpan pspan = smv.getViewSeqSpan(childSym);
			if (pspan == null || pspan.getLength() == 0) {
				return;
			}  // if no span corresponding to seq, then return;
			tierGlyph.setTierType(TierGlyph.TierType.ANNOTATION);
			tierGlyph.setDirection(Direction.NONE);
			tierGlyph.setInfo(sym);	
			GlyphI residueGlyph = getAlignedResiduesGlyph(childSym, smv.getAnnotatedSeq(), false);
			if(residueGlyph != null){
				FillRectGlyph childGlyph = new FillRectGlyph();
				childGlyph.setCoords(pspan.getMin(), 0, pspan.getLength(), style.getHeight() + 0.0001);
				childGlyph.setColor(style.getForeground());
				residueGlyph.setCoords(pspan.getMin(), 0, pspan.getLength(), style.getHeight() + 0.0001);
				smv.setDataModelFromOriginalSym(childGlyph, childSym);
				childGlyph.addChild(residueGlyph);
				tierGlyph.addChild(childGlyph);
			}
		}
	}
	
	@Override
	public String getName() {
		return "sequence";
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return category == FileTypeCategory.Sequence;
	}
}
