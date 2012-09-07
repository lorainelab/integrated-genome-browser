package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.shared.AlignedResidueGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryA;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;

public class SequenceGlyphFactory extends MapViewGlyphFactoryA {
			
	@Override
	public void createGlyphs(SeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv) {
		if (sym != null) {
			TierGlyph tierGlyph = smv.getTrack(style, Direction.NONE);
			SimpleSymWithResidues childSym = (SimpleSymWithResidues) sym.getChild(0);
			SeqSpan pspan = smv.getViewSeqSpan(childSym);
			if (pspan == null || pspan.getLength() == 0) {
				return;
			}  // if no span corresponding to seq, then return;
			tierGlyph.setDirection(Direction.NONE);
			tierGlyph.setInfo(sym);	
			GlyphI residueGlyph = addAlignedResidues(childSym, smv.getAnnotatedSeq());
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

	/**
	 * Determine and set the appropriate residues for this element.
	 */
	private static GlyphI addAlignedResidues(SeqSymmetry sym, BioSeq annotseq) {
		SeqSpan span = sym.getSpan(annotseq);
		if (span == null) {
			return null;
		}

		String residueStr = ((SymWithResidues) sym).getResidues();

		if (residueStr == null || residueStr.length() == 0) {
			return null;
		}
		
		AlignedResidueGlyph csg = new AlignedResidueGlyph();
		csg.setResidues(residueStr);
		csg.setHitable(false);
		// Do not set residue mask
		/*String bioSeqResidue = annotseq.getResidues(span.getMin(), span.getMin() + residueStr.length());
		if (bioSeqResidue != null) {
			csg.setResidueMask(bioSeqResidue);
		}*/
		csg.setDefaultShowMask(false);
		return csg;
			
		// SEQ array has unexpected behavior;  commenting out for now.
		/*if (((SymWithProps) sym).getProperty("SEQ") != null) {
		byte[] seqArr = (byte[]) ((SymWithProps) sym).getProperty("SEQ");
		for (int i = 0; i < seqArr.length; i++) {
		System.out.print((char) seqArr[i]);
		}
		System.out.println();
		isg.setResidueMask(seqArr);
		}*/
		
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
