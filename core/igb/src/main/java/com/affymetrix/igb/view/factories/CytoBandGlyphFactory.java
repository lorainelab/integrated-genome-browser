package com.affymetrix.igb.view.factories;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.glyph.CytobandGlyph;
import com.affymetrix.igb.glyph.DefaultTierGlyph;
import com.affymetrix.igb.tiers.CoordinateStyle;
import org.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import java.util.Set;

/**
 *
 * @author hiralv
 */
//@Component(name = CytoBandGlyphFactory.COMPONENT_NAME, provide = {MapTierGlyphFactoryI.class})
public class CytoBandGlyphFactory extends MapTierGlyphFactoryA {

    public static final String COMPONENT_NAME = "CytoBandGlyphFactory";

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq) {
        DefaultTierGlyph resultAxisTier = (DefaultTierGlyph) smv.getTrack(CoordinateStyle.coordinate_annot_style, StyledGlyph.Direction.AXIS);
        if (smv.shouldAddCytobandGlyph()) {
            GlyphI cytoband_glyph = CytobandGlyph.makeCytobandGlyph(sym, smv, seq, resultAxisTier);
            if (cytoband_glyph != null) {
                resultAxisTier.addChild(cytoband_glyph);
            }
        }
    }

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }

    @Override
    public Set<FileTypeCategory> getSupportedCategories() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
