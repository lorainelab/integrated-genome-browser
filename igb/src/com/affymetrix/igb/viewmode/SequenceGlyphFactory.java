package com.affymetrix.igb.viewmode;

import java.util.Map;

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
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class SequenceGlyphFactory implements MapViewGlyphFactoryI {
	private SeqMapViewExtendedI gviewer;

	@Override
	public void init(Map<String, Object> options) {
	}

	@Override
	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI smv) {
		// not implemented
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym,
			ITrackStyleExtended style, Direction direction) {
		ViewModeGlyph viewModeGlyph = new SequenceGlyph(style);
		SimpleSymWithResidues childSym = (SimpleSymWithResidues)sym.getChild(0);
		SeqSpan pspan = gviewer.getViewSeqSpan(childSym);
		if (pspan == null || pspan.getLength() == 0) {
			return viewModeGlyph;
		}  // if no span corresponding to seq, then return;
		viewModeGlyph.setDirection(direction);
		viewModeGlyph.setInfo(sym);
		FillRectGlyph childGlyph = new FillRectGlyph();
		double pheight = style.getHeight() + 0.0001;
		childGlyph.setCoords(pspan.getMin(), 0, pspan.getLength(), pheight);
		childGlyph.setColor(style.getForeground());
		gviewer.setDataModelFromOriginalSym(childGlyph, childSym);
		BioSeq annotseq = gviewer.getAnnotatedSeq();
		GlyphI alignResidueGlyph = handleAlignedResidues(childSym, annotseq);
		alignResidueGlyph.setCoordBox(childGlyph.getCoordBox());
		childGlyph.addChild(alignResidueGlyph);
		viewModeGlyph.addChild(childGlyph);
		return viewModeGlyph;
	}

	/**
	 * Determine and set the appropriate residues for this element.
	 * @param sym
	 * @param annotseq
	 * @return GlyphI
	 */
	private GlyphI handleAlignedResidues(SeqSymmetry sym, BioSeq annotseq) {
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
		String bioSeqResidue = annotseq.getResidues(span.getMin(), span.getMin() + residueStr.length());
		if (bioSeqResidue != null) {
			csg.setResidueMask(bioSeqResidue);
		}
		csg.setHitable(false);
		
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
	public boolean isFileSupported(FileTypeCategory category) {
		return category == FileTypeCategory.Sequence;
	}

	public void setSeqMapView(SeqMapViewExtendedI gviewer) {
		this.gviewer = gviewer;
	}

	@Override
	public final SeqMapViewExtendedI getSeqMapView(){
		return gviewer;
	}
}
