package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;

public class SequenceGlyphFactory extends MapTierGlyphFactoryA {

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq) {
        if (sym != null) {
            TierGlyph tierGlyph = smv.getTrack(style, TierGlyph.Direction.NONE);
            tierGlyph.setTierType(TierGlyph.TierType.SEQUENCE);
            tierGlyph.setDirection(TierGlyph.Direction.NONE);
            tierGlyph.setInfo(sym);
            for (int i = 0; i < sym.getChildCount(); i++) {
                if (!(sym.getChild(i) instanceof SimpleSymWithResidues)) {
                    continue;
                }
                SimpleSymWithResidues childSym = (SimpleSymWithResidues) sym.getChild(i);
                SeqSpan pspan = smv.getViewSeqSpan(childSym);
                if (pspan == null || pspan.getLength() == 0) {
                    return;
                }  // if no span corresponding to seq, then return;	
                GlyphI residueGlyph = getAlignedResiduesGlyph(childSym, smv.getAnnotatedSeq(), false);
                if (residueGlyph != null) {
                    FillRectGlyph childGlyph = new FillRectGlyph();
                    childGlyph.setCoords(pspan.getMin(), 0, pspan.getLength(), style.getHeight() + 0.0001);
                    childGlyph.setColor(style.getForeground());
                    residueGlyph.setCoords(pspan.getMin(), 0, pspan.getLength(), style.getHeight() + 0.0001);
                    tierGlyph.setDataModelFromOriginalSym(childGlyph, childSym);
                    childGlyph.addChild(residueGlyph);
                    tierGlyph.addChild(childGlyph);
                }
            }
            doMiddlegroundShading(tierGlyph, smv, seq);
        }
    }

    @Override
    public String getName() {
        return "sequence";
    }

}
