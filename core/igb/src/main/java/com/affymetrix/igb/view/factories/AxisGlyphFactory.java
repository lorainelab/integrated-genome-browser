package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.glyph.CharSeqGlyph;
import com.affymetrix.igb.glyph.CytobandGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.StyledGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.CoordinateStyle;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author hiralv
 */
public class AxisGlyphFactory extends MapTierGlyphFactoryA {

    private static final int TIER_SIZE = 54;
    private static final int AXIS_SIZE = 27;
    private static final Font axisFont = NeoConstants.default_bold_font;

    public static TierGlyph addAxisTier(SeqMapView smv, int tier_index) {
        TransformTierGlyph resultAxisTier = new TransformTierGlyph(CoordinateStyle.coordinate_annot_style);
        resultAxisTier.setInfo(new RootSeqSymmetry() {
            @Override
            public FileTypeCategory getCategory() {
                return FileTypeCategory.Axis;
            }

            @Override
            public void search(Set<SeqSymmetry> results, String id) {
            }

            @Override
            public void searchHints(Set<String> results, Pattern regex, int limit) {
            }

            @Override
            public void search(Set<SeqSymmetry> result, Pattern regex, int limit) {
            }

            @Override
            public void searchProperties(Set<SeqSymmetry> results, Pattern regex, int limit) {
            }
        });
        resultAxisTier.setPacker(null);
        resultAxisTier.setFixedPixHeight(TIER_SIZE);
        resultAxisTier.setDirection(TierGlyph.Direction.AXIS);

        AxisGlyph axis_glyph = smv.getSeqMap().addAxis(AXIS_SIZE);
        axis_glyph.setHitable(true);
        axis_glyph.setFont(axisFont);

        axis_glyph.setBackgroundColor(resultAxisTier.getBackgroundColor());
        axis_glyph.setForegroundColor(resultAxisTier.getForegroundColor());

        SeqMapView.setAxisFormatFromPrefs(axis_glyph);
        if (smv.shouldAddCytobandGlyph()) {
            GlyphI cytoband_glyph = CytobandGlyph.makeCytobandGlyph(smv, resultAxisTier);
            if (cytoband_glyph != null) {
                resultAxisTier.addChild(cytoband_glyph);
            }
        }
        resultAxisTier.addChild(axis_glyph);

        // it is important to set the colors before adding the tier
        // to the map, else the label tier colors won't match
        if (smv.getSeqMap().getTiers().size() >= tier_index) {
            smv.getSeqMap().addTier(resultAxisTier, tier_index);
        } else {
            smv.getSeqMap().addTier(resultAxisTier, false);
        }

        CharSeqGlyph seq_glyph = CharSeqGlyph.initSeqGlyph(smv.getViewSeq(), axis_glyph);
        resultAxisTier.addChild(seq_glyph);

        resultAxisTier.setCoords(0, 0, smv.getSeqMap().getScene().getCoordBox().getWidth(), TIER_SIZE);

        return resultAxisTier;
    }

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq) {
        DefaultTierGlyph resultAxisTier = (DefaultTierGlyph) smv.getTrack(style, StyledGlyph.Direction.AXIS);
        resultAxisTier.setInfo(new RootSeqSymmetry() {
            @Override
            public FileTypeCategory getCategory() {
                return FileTypeCategory.Axis;
            }

            @Override
            public void search(Set<SeqSymmetry> results, String id) {
            }

            @Override
            public void searchHints(Set<String> results, Pattern regex, int limit) {
            }

            @Override
            public void search(Set<SeqSymmetry> result, Pattern regex, int limit) {
            }

            @Override
            public void searchProperties(Set<SeqSymmetry> results, Pattern regex, int limit) {
            }
        });
        resultAxisTier.setPacker(new PackerI() {

            @Override
            public Rectangle pack(GlyphI parent_glyph, GlyphI child_glyph, ViewI view) {
                parent_glyph.setCoords(parent_glyph.getCoordBox().x,
                        parent_glyph.getCoordBox().y, view.getScene().getCoordBox().getWidth(), TIER_SIZE);
                return null;
            }

            @Override
            public Rectangle pack(GlyphI parent_glyph, ViewI view) {
                parent_glyph.setCoords(parent_glyph.getCoordBox().x,
                        parent_glyph.getCoordBox().y, view.getScene().getCoordBox().getWidth(), TIER_SIZE);
                return null;
            }

        });
        resultAxisTier.setHeightFixed(true);
        resultAxisTier.setFixedPixHeight(TIER_SIZE);
        resultAxisTier.setDirection(TierGlyph.Direction.AXIS);
        resultAxisTier.setTierType(TierGlyph.TierType.NONE);

        if (smv.shouldAddCytobandGlyph()) {
            GlyphI cytoband_glyph = CytobandGlyph.makeCytobandGlyph(smv, resultAxisTier);
            if (cytoband_glyph != null) {
                resultAxisTier.addChild(cytoband_glyph);
            }
        }

        AxisGlyph axis_glyph = smv.getSeqMap().addAxis(AXIS_SIZE);
        axis_glyph.setHitable(true);
        axis_glyph.setFont(axisFont);

        axis_glyph.setBackgroundColor(resultAxisTier.getBackgroundColor());
        axis_glyph.setForegroundColor(resultAxisTier.getForegroundColor());

        SeqMapView.setAxisFormatFromPrefs(axis_glyph);
        resultAxisTier.addChild(axis_glyph);

        CharSeqGlyph seq_glyph = CharSeqGlyph.initSeqGlyph(smv.getViewSeq(), axis_glyph);
        resultAxisTier.addChild(seq_glyph);
    }

    @Override
    public String getName() {
        return "axis";
    }

}
