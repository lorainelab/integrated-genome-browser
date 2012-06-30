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
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class SequenceGlyphFactory extends MapViewGlyphFactoryA {
	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv) {
		ViewModeGlyph viewModeGlyph = new SequenceGlyph(style);
		
		if(sym == null){
			return viewModeGlyph;
		}
		
		SimpleSymWithResidues childSym = (SimpleSymWithResidues)sym.getChild(0);
		SeqSpan pspan = smv.getViewSeqSpan(childSym);
		if (pspan == null || pspan.getLength() == 0) {
			return viewModeGlyph;
		}  // if no span corresponding to seq, then return;
		viewModeGlyph.setDirection(direction);
		viewModeGlyph.setInfo(sym);
		FillRectGlyph childGlyph = new FillRectGlyph();
		childGlyph.setCoords(pspan.getMin(), 0, pspan.getLength(), style.getHeight() + 0.0001);
		childGlyph.setColor(style.getForeground());
		smv.setDataModelFromOriginalSym(childGlyph, childSym);
		BioSeq annotseq = smv.getAnnotatedSeq();
		addAlignedResidues(childSym, annotseq, childGlyph);
		viewModeGlyph.addChild(childGlyph);
		return viewModeGlyph;
	}

	/**
	 * Determine and set the appropriate residues for this element.
	 */
	private static void addAlignedResidues(SeqSymmetry sym, BioSeq annotseq, GlyphI childGlyph) {
		SeqSpan span = sym.getSpan(annotseq);
		if (span == null) {
			return;
		}

		String residueStr = ((SymWithResidues) sym).getResidues();

		if (residueStr == null || residueStr.length() == 0) {
			return;
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
		csg.setCoordBox(childGlyph.getCoordBox());
		childGlyph.addChild(csg);
			
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
