package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import com.affymetrix.igb.IGBConstants;
import java.util.Map;
import java.util.MissingResourceException;

public abstract class MapViewGlyphFactoryA implements MapViewGlyphFactoryI {
	
	@Override
	public void init(Map<String, Object> options) {	}
	
	@Override
	public String getDisplayName() {
		String displayName = null;
		try {
			displayName = IGBConstants.BUNDLE.getString("viewmode_" + getName());
		}
		catch(MissingResourceException x) {
			displayName = getName();
		}
		return displayName;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return false;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
	
	/**
	 * Determine and set the appropriate residues for this element.
	 */
	protected static AlignedResidueGlyph getAlignedResiduesGlyph(SeqSymmetry insym, BioSeq annotseq, boolean setMask) {
		SeqSymmetry sym = insym;
		if (insym instanceof DerivedSeqSymmetry) {
			sym = getMostOriginalSymmetry(insym);
		}

		if (!(sym instanceof SymWithResidues)) {
			return null;
		}
		
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
		csg.setDefaultShowMask(false);
		if(setMask){
			String bioSeqResidue = annotseq.getResidues(span.getMin(), span.getMin() + residueStr.length());
			if (bioSeqResidue != null) {
				csg.setResidueMask(bioSeqResidue);
			}
		}
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
	
	protected static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
		if (sym instanceof DerivedSeqSymmetry) {
			return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
		}
		return sym;
	}
}
