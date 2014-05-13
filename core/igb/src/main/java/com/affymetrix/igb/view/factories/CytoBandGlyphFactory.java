package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.glyph.CytobandGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.StyledGlyph;
import com.affymetrix.igb.tiers.CoordinateStyle;

/**
 *
 * @author hiralv
 */
public class CytoBandGlyphFactory extends MapTierGlyphFactoryA {
	
	@Override
	public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq) {
		DefaultTierGlyph resultAxisTier = (DefaultTierGlyph)smv.getTrack(CoordinateStyle.coordinate_annot_style, StyledGlyph.Direction.AXIS);
		if(smv.shouldAddCytobandGlyph()){
			GlyphI cytoband_glyph = CytobandGlyph.makeCytobandGlyph(sym, smv, seq, resultAxisTier);
			if (cytoband_glyph != null) {
				resultAxisTier.addChild(cytoband_glyph);
			}
		}
	}
	
	@Override
	public String getName() {
		return "cytoband";
	}
}
