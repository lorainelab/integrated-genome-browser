package com.affymetrix.igb.shared;

import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithBaseQuality;
import com.affymetrix.genometry.symmetry.SymWithResidues;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.IGBConstants;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

public abstract class MapTierGlyphFactoryA implements MapTierGlyphFactoryI {

    @Override
    public void init(Map<String, Object> options) {
    }

    @Override
    public String getDisplayName() {
        String displayName = null;
        try {
            displayName = IGBConstants.BUNDLE.getString("viewmode_" + getName());
        } catch (MissingResourceException x) {
            displayName = getName();
        }
        return displayName;
    }

    @Override
    public boolean supportsTwoTrack() {
        return false;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    protected static void doMiddlegroundShading(TierGlyph tierGlyph, SeqMapViewExtendedI gviewer, BioSeq seq) {
        tierGlyph.clearMiddleGlyphs();
        GenericFeature feature = tierGlyph.getAnnotStyle().getFeature();
        if (feature != null) {
            SeqSymmetry inverse = SeqUtils.inverse(feature.getRequestSym(), seq);
            if (seq != gviewer.getViewSeq()) {
                inverse = gviewer.transformForViewSeq(inverse, seq);
            }
            int child_count = inverse.getChildCount();
            if (child_count > 0) {
                for (int i = 0; i < child_count; i++) {
                    SeqSymmetry child = inverse.getChild(i);
                    SeqSpan ospan = child.getSpan(gviewer.getViewSeq());
                    if (ospan != null && ospan.getLength() > 1) {
                        GlyphI mglyph = new FillRectGlyph();
                        mglyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
                        tierGlyph.addMiddleGlyph(mglyph);
                    }
                }
            } else {
                GlyphI mglyph = new FillRectGlyph();
                mglyph.setCoords(seq.getMin(), 0, seq.getLength() - 1, 0);
                tierGlyph.addMiddleGlyph(mglyph);
            }
        }
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

        SymWithResidues swr = (SymWithResidues) sym;
        String residueStr = swr.getResidues();

        if (residueStr == null || residueStr.length() == 0) {
            return null;
        }

        AlignedResidueGlyph csg = new AlignedResidueGlyph();
        csg.setResidues(residueStr);
        csg.setHitable(false);
        csg.setDefaultShowMask(false);
        if (setMask) {
            BitSet residueMask = swr.getResidueMask();
            if (residueMask == null && annotseq.isAvailable(span.getMin(), span.getMin() + residueStr.length())) {
                String bioSeqResidue = annotseq.getResidues(span.getMin(), span.getMin() + residueStr.length());
                residueMask = getResidueMask(residueStr, bioSeqResidue);
                swr.setResidueMask(residueMask);
            }
            csg.setResidueMask(residueMask);
        }
        if (sym instanceof SymWithBaseQuality) {
            csg.setBaseQuality(((SymWithBaseQuality) sym).getBaseQuality());
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

    @Override
    public void createGlyphs(RootSeqSymmetry rootSym, List<? extends SeqSymmetry> syms, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq) {

    }

    //Determines residue mismatches
    private static BitSet getResidueMask(String residues, String mask) {
        if (residues != null && mask != null) {
            BitSet residueMask = new BitSet();
            char[] residuesArr = mask.toLowerCase().toCharArray();
            char[] displayResArr = residues.toLowerCase().toCharArray();
            int minResLen = Math.min(residuesArr.length, displayResArr.length);

            // determine which residues disagree with the reference sequence
            for (int i = 0; i < minResLen; i++) {
                residueMask.set(i, displayResArr[i] != residuesArr[i]);
            }
            return residueMask;
        }
        return null;
    }

    protected static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
        if (sym instanceof DerivedSeqSymmetry) {
            return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
        }
        return sym;
    }

    protected class Track {

        private final TierGlyph forwardTier;
        private final TierGlyph reverseTier;

        public Track(TierGlyph forwardTier) {            
            this(forwardTier, null);
        }

        public Track(TierGlyph forwardTier, TierGlyph reverseTier) {
            checkNotNull(forwardTier);
            this.forwardTier = forwardTier;
            this.reverseTier = reverseTier;
        }

        public TierGlyph getForwardTier() {
            return forwardTier;
        }

        public Optional<TierGlyph> getReverseTier() {
            return Optional.fromNullable(reverseTier);
        }
    }
}
