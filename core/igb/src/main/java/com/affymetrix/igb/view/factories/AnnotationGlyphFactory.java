/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view.factories;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.MultiTierSymWrapper;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.genoviz.glyph.EfficientLineContGlyph;
import com.affymetrix.genoviz.glyph.EfficientMateJoinGlyph;
import com.affymetrix.genoviz.glyph.EfficientOutlinedRectGlyph;
import com.affymetrix.genoviz.glyph.EfficientSolidGlyph;
import com.affymetrix.genoviz.glyph.InsertionSeqGlyph;
import com.affymetrix.genoviz.glyph.PointedGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.glyph.AlignedResidueGlyph;
import com.affymetrix.igb.shared.CodonGlyphProcessor;
import com.affymetrix.igb.glyph.DeletionGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import com.affymetrix.igb.shared.MapTierGlyphFactoryI;
import static com.affymetrix.igb.shared.MapTierGlyphFactoryI.DEFAULT_CHILD_HEIGHT;
import com.affymetrix.igb.shared.PreprocessorRegistry;
import com.affymetrix.igb.tiers.TrackConstants.DirectionType;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableSet;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;
import com.lorainelab.igb.genoviz.extensions.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.lorainelab.igb.services.visualization.SeqSymmetryPreprocessorI;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @version $Id: AnnotationGlyphFactory.java 10247 2012-02-10 16:36:20Z lfrohman
 * $
 */
@Component(name = AnnotationGlyphFactory.COMPONENT_NAME, provide = {MapTierGlyphFactoryI.class}, immediate = true)
public class AnnotationGlyphFactory extends MapTierGlyphFactoryA {

    public static final String COMPONENT_NAME = "AnnotationGlyphFactory";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AnnotationGlyphFactory.class);
    private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###.###");

    static {
        COMMA_FORMAT.setDecimalSeparatorAlwaysShown(false);
    }
    /**
     * Set to true if the we can assume the container SeqSymmetry being passed
     * to addLeafsToTier has all its leaf nodes at the same depth from the top.
     */
    private static final Class<?> UNLABELLED_PARENT_GLYPH_CLASS = (new EfficientLineContGlyph()).getClass();
    private static final Class<?> CHILD_GLYPH_CLASS = (new EfficientOutlinedRectGlyph()).getClass();
    private static final Class<?> LABLELLED_PARENT_GLYPH_CLASS = (new EfficientLabelledLineGlyph()).getClass();
    private final CodonGlyphProcessor codon_glyph_processor;
    protected SeqMapViewExtendedI seqMap;
    private BioSeq annotSeq;
    private BioSeq viewSeq;
    //span of the symmetry that is on the seq being viewed
    private SeqSpan symSpan;
    private boolean isValidSymSpan;
    /**
     * @param drawChildren Whether to draw this sym as a parent and also draw
     * its children, or to just draw the sym itself (using the child glyph
     * style). If this is set to true, then the symmetry must have a depth of at
     * least 2.
     */
    boolean drawChildren;
    private Track track;
    private TierGlyph tierGlyph;
    private ITrackStyleExtended trackStyle;

    public AnnotationGlyphFactory() {
        codon_glyph_processor = new CodonGlyphProcessor();
    }

    @Override
    public void init(Map<String, Object> options) {
    }

    @Override
    public boolean supportsTwoTrack() {
        return true;
    }

    protected void addLeafsToTier(SeqSymmetry originalSym, int desired_leaf_depth, ITrackStyleExtended style) {
        int depth = SeqUtils.getDepthFor(originalSym);
        if (originalSym instanceof MultiTierSymWrapper) {
            if (style.isShowAsPaired()) {
                addTopChild(originalSym);
            } else {
                addTopChild(originalSym.getChild(0));
                initSymSpan(originalSym.getChild(1));
                addTopChild(originalSym.getChild(1));
            }
            return;
        }
        if (depth > desired_leaf_depth || originalSym instanceof TypeContainerAnnot) {
            int childCount = originalSym.getChildCount();
            for (int i = 0; i < childCount; i++) {
                addLeafsToTier(originalSym.getChild(i), desired_leaf_depth, style);
            }
        } else {  // depth == desired_leaf_depth
            addTopChild(originalSym);
        }
    }

    private void addToTier(List<? extends SeqSymmetry> insyms) {
        insyms.forEach(this::addTopChild);
    }

    protected void addTopChild(SeqSymmetry originalSym) {
        SeqSymmetry sym = initSymSpan(originalSym);
        updateSymSpan(sym);
        if (!isValidSymSpan) {
            return;
        }
        setTierGlyph();
        int depth = SeqUtils.getDepthFor(sym);
        drawChildren = (depth >= 2);
        Optional<GlyphI> glyph = determinePGlyph(sym);
        if (glyph.isPresent()) {
            if (trackStyle.getFilter() != null) {
                glyph.get().setVisibility(trackStyle.getFilter().filterSymmetry(annotSeq, sym));
            }
            tierGlyph.addChild(glyph.get());
        }
    }

    protected Optional<GlyphI> determinePGlyph(SeqSymmetry sym) {
        Optional<GlyphI> parentGlyph;

        DirectionType directionType = DirectionType.valueFor(trackStyle.getDirectionType());
        Color color = getSymColor(sym);
        if (drawChildren && sym.getChildCount() > 0) {
            parentGlyph = determineGlyph(UNLABELLED_PARENT_GLYPH_CLASS, sym);
            // call out to handle rendering to indicate if any of the children of the
            //    original annotation are completely outside the view
            if (parentGlyph.isPresent()) {
                if (sym instanceof MultiTierSymWrapper) {
                    handlePairedChildren((MultiTierSymWrapper) sym, parentGlyph.get());
                } else {
                    addChildren(sym, parentGlyph.get());
                    handleInsertionGlyphs(sym, parentGlyph.get());
                }
            }

        } else {
            // depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
            parentGlyph = determineGlyph(CHILD_GLYPH_CLASS, sym);
            if (parentGlyph.isPresent()) {
                if (directionType == DirectionType.ARROW || directionType == DirectionType.BOTH) {
                    addChildGlyph(sym, symSpan, parentGlyph.get().getCoordBox().getHeight(), color, false, parentGlyph.get());
                }
                addAlignedResiduesGlyph(sym, symSpan, DEFAULT_CHILD_HEIGHT, parentGlyph.get());
            }
        }
        return parentGlyph;
    }

    private void handlePairedChildren(MultiTierSymWrapper sym, GlyphI parentGlyph) {
        Optional<GlyphI> firstChildPglyph;
        Optional<GlyphI> secondChildPglyph;
        SeqSymmetry sym1 = sym.getChild(0);
        SeqSymmetry sym2 = sym.getChild(1);

        sym1 = seqMap.transformForViewSeq(sym1, annotSeq);
        sym2 = seqMap.transformForViewSeq(sym2, annotSeq);

        firstChildPglyph = determineGlyph(UNLABELLED_PARENT_GLYPH_CLASS, sym1);
        secondChildPglyph = determineGlyph(UNLABELLED_PARENT_GLYPH_CLASS, sym2);

        if (firstChildPglyph.isPresent() && secondChildPglyph.isPresent()) {
            addChildren(sym1, firstChildPglyph.get());
            handleInsertionGlyphs(sym1, firstChildPglyph.get());
            addChildren(sym2, secondChildPglyph.get());
            handleInsertionGlyphs(sym2, secondChildPglyph.get());
            parentGlyph.addChild(firstChildPglyph.get());
            parentGlyph.addChild(secondChildPglyph.get());
        }

    }

    private Optional<GlyphI> determineGlyph(Class<?> glyphClass, SeqSymmetry sym) {
        EfficientSolidGlyph pglyph = null;
        try {
            SeqSpan span = seqMap.getViewSeqSpan(sym);
            Color color = getSymColor(sym);
            boolean labelInSouth = !span.isForward() && track.getReverseTier().isPresent();

            // Note: Setting parent height (pheight) larger than the child height (cheight)
            // allows the user to select both the parent and the child as separate entities
            // in order to look at the properties associated with them.  Otherwise, the method
            // EfficientGlyph.pickTraversal() will only allow one to be chosen.
            double pheight = DEFAULT_CHILD_HEIGHT + 0.0001;
            if ((sym instanceof MultiTierSymWrapper) && AbstractTierGlyph.useLabel(trackStyle)) {
                pglyph = EfficientMateJoinGlyph.class.newInstance();
            } else if (AbstractTierGlyph.useLabel(trackStyle)) {
                EfficientLabelledGlyph lglyph = (EfficientLabelledGlyph) LABLELLED_PARENT_GLYPH_CLASS.newInstance();
                Optional<Object> property = getTheProperty(sym, trackStyle.getLabelField());
                String label = "";
                if (property.isPresent()) {
                    if (property.get() instanceof Number) {
                        label = COMMA_FORMAT.format(property.get());
                    } else {
                        label = property.get().toString();
                    }
                }
                if (labelInSouth) {
                    lglyph.setLabelLocation(GlyphI.SOUTH);
                } else {
                    lglyph.setLabelLocation(GlyphI.NORTH);
                }
                lglyph.setLabel(label);
                pheight = 2 * pheight;
                pglyph = lglyph;

            } else {
                pglyph = (EfficientSolidGlyph) glyphClass.newInstance();
            }
            if (sym instanceof MultiTierSymWrapper) {
                pglyph.setDirection(NeoConstants.NONE);
                color = Color.BLACK;
            } else {
                DirectionType directionType = DirectionType.valueFor(tierGlyph.getAnnotStyle().getDirectionType());
                if (directionType == DirectionType.ARROW || directionType == DirectionType.BOTH) {
                    pglyph.setDirection(span.isForward() ? NeoConstants.RIGHT : NeoConstants.LEFT);
                }
            }
            pglyph.setCoords(span.getMin(), 0, span.getLength(), pheight);
            pglyph.setColor(color);
            tierGlyph.setDataModelFromOriginalSym(pglyph, sym);
        } catch (InstantiationException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.<GlyphI>ofNullable(pglyph);
    }

    private void addChildren(SeqSymmetry sym, GlyphI parentGlyph) {
        SeqSpan cdsSpan = null;
        SeqSymmetry cds_sym = null;
        boolean sameSeq = annotSeq == viewSeq;
        if ((sym instanceof SupportsCdsSpan) && ((SupportsCdsSpan) sym).hasCdsSpan()) {
            cdsSpan = ((SupportsCdsSpan) sym).getCdsSpan();
            MutableSeqSymmetry tempsym = new SimpleMutableSeqSymmetry();
            tempsym.addSpan(new SimpleMutableSeqSpan(cdsSpan));
            if (!sameSeq) {
                SeqUtils.transformSymmetry(tempsym, seqMap.getTransformPath());
                cdsSpan = seqMap.getViewSeqSpan(tempsym);
            }
            cds_sym = tempsym;
        }
        // call out to handle rendering to indicate if any of the children of the
        //    orginal annotation are completely outside the view

        int childCount = sym.getChildCount();
        List<SeqSymmetry> outsideChildren = new ArrayList<>();
        Color color = getSymColor(sym);
        double thinHeight = DEFAULT_CHILD_HEIGHT * 0.6;
//		Color start_color = the_style.getStartColor();
//		Color end_color = the_style.getEndColor();
        for (int i = 0; i < childCount; i++) {
            SeqSymmetry child = sym.getChild(i);
            SeqSpan cspan = seqMap.getViewSeqSpan(child);
            if (cspan == null) {
                // if no span for view, then child is either to left or right of view
                outsideChildren.add(child); // collecting children outside of view to handle later
            } else {
                boolean cds = (cdsSpan == null || SeqUtils.contains(cdsSpan, cspan));
                double cheight = thinHeight;
                if (cds) {
                    cheight = DEFAULT_CHILD_HEIGHT;
                }

                addChildGlyph(child, cspan, cheight, color, childCount > 1, parentGlyph);
                addAlignedResiduesGlyph(child, cspan, cheight, parentGlyph);

                if (!cds) {
                    handleCDSSpan(cdsSpan, cspan, cds_sym, child, sym, color, parentGlyph);
                }
            }
        }

//		ArrowHeadGlyph.addDirectionGlyphs(gviewer, sym, pglyph, viewSeq, viewSeq, 0.0,
//			thin_height, the_style.getDirectionType() == DirectionType.ARROW.ordinal());
        // call out to handle rendering to indicate if any of the children of the
        //    orginal annotation are completely outside the view
        DeletionGlyph.handleEdgeRendering(outsideChildren, parentGlyph, annotSeq, viewSeq, 0.0, thinHeight);
    }

    private void addChildGlyph(SeqSymmetry sym, SeqSpan cspan, double height, Color color, boolean hitable, GlyphI pglyph) {

        GlyphI cglyph;
        Optional<GlyphI> child = getChild(cspan, cspan.getMin() == symSpan.getMin(), cspan.getMax() == symSpan.getMax());
        if (child.isPresent()) {
            cglyph = child.get();
            cglyph.setCoords(cspan.getMin(), 0, cspan.getLength(), height);
            cglyph.setColor(color);
            tierGlyph.setDataModelFromOriginalSym(cglyph, sym);
            pglyph.addChild(cglyph);

            cglyph.setHitable(hitable);
            if (cglyph instanceof DirectedGlyph) {
                ((DirectedGlyph) cglyph).setForward(cspan.isForward());
            }
            codon_glyph_processor.processGlyph(cglyph, sym, annotSeq);
        }
    }

    private static Optional<Object> getTheProperty(SeqSymmetry sym, String prop) {
        if (StringUtils.isBlank(prop)) {
            return Optional.empty();
        }
        SeqSymmetry original = getMostOriginalSymmetry(sym);

        if (original instanceof SymWithProps) {
            Object ret = ((SymWithProps) original).getProperty(prop);

            if (ret == null || ret.toString().length() == 0) {
                ret = ((SymWithProps) original).getProperty(prop.toLowerCase());
            }

            if (ret == null || ret.toString().length() == 0) {
                ret = ((SymWithProps) original).getProperty(prop.toUpperCase());
            }

            return Optional.ofNullable(ret);
        }
        return Optional.empty();
    }

    private void addAlignedResiduesGlyph(SeqSymmetry sym, SeqSpan span, double height, GlyphI pglyph) {
        AlignedResidueGlyph alignResidueGlyph = getAlignedResiduesGlyph(sym, annotSeq, true);
        if (alignResidueGlyph != null) {
            alignResidueGlyph.setCoords(span.getMin(), 0, span.getLength(), height);
            alignResidueGlyph.setBackgroundColor(Color.WHITE);
            alignResidueGlyph.setForegroundColor(pglyph.getForegroundColor());
            alignResidueGlyph.setDefaultShowMask(tierGlyph.getAnnotStyle().getShowResidueMask());
            alignResidueGlyph.setUseBaseQuality(tierGlyph.getAnnotStyle().getShadeBasedOnQualityScore());
            tierGlyph.setDataModelFromOriginalSym(alignResidueGlyph, sym);
            pglyph.addChild(alignResidueGlyph);
        }
    }

    private Optional<GlyphI> getChild(SeqSpan cspan, boolean isFirst, boolean isLast) {
        try {
            DirectionType directionType = DirectionType.valueFor(trackStyle.getDirectionType());
            if (cspan.getLength() == 0) {
                return Optional.<GlyphI>ofNullable(new DeletionGlyph());
            } else if (((isLast && cspan.isForward()) || (isFirst && !cspan.isForward()))
                    && (directionType == DirectionType.ARROW || directionType == DirectionType.BOTH)) {
                return Optional.<GlyphI>ofNullable(new PointedGlyph());
            }
            return Optional.ofNullable((GlyphI) CHILD_GLYPH_CLASS.newInstance());
        } catch (InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.<GlyphI>ofNullable(null);
    }

    private Color getSymColor(SeqSymmetry insym) {
        Color color = null;
        SeqSpan span = seqMap.getViewSeqSpan(insym);
        ColorProviderI cp = trackStyle.getColorProvider();
        DirectionType directionType = DirectionType.valueFor(trackStyle.getDirectionType());
        boolean isForward = span.isForward();
        if (cp != null) {
            SeqSymmetry sym = insym;
            if (insym instanceof DerivedSeqSymmetry) {
                sym = getMostOriginalSymmetry(insym);
            }
            color = cp.getColor(sym);
        }

        if (color == null) {
            if (directionType == DirectionType.COLOR || directionType == DirectionType.BOTH) {
                if (isForward) {
                    return trackStyle.getForwardColor();
                }
                return trackStyle.getReverseColor();
            }
            return trackStyle.getForeground();
        }

        return color;
    }

    private void handleCDSSpan(SeqSpan cdsSpan, SeqSpan childSpan, SeqSymmetry cdsSym, SeqSymmetry child, SeqSymmetry originalSym, Color childColor, GlyphI pglyph) {
        try {
            boolean sameSeq = annotSeq == viewSeq;
            if (SeqUtils.overlap(cdsSpan, childSpan)) {
                CdsSeqSymmetry cdsSym2 = new CdsSeqSymmetry();
                SeqUtils.intersection(cdsSym, child, cdsSym2, annotSeq, childSpan.isForward());
                if (!sameSeq) {
                    //cds_sym_2 = (CdsSeqSymmetry)gviewer.transformForViewSeq(cds_sym_2, new CdsSeqSymmetry(), annotSeq);
                    SeqUtils.transformSymmetry(cdsSym2, seqMap.getTransformPath());
                }
                cdsSym2.setPropertySymmetry(originalSym);

                SeqSpan cds_span = seqMap.getViewSeqSpan(cdsSym2);
                if (cds_span != null) {
                    GlyphI cds_glyph;
                    if (childSpan.getLength() == 0) {
                        cds_glyph = new DeletionGlyph();
                    } else {
                        cds_glyph = (GlyphI) CHILD_GLYPH_CLASS.newInstance();
                    }
                    cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), DEFAULT_CHILD_HEIGHT);
                    cds_glyph.setColor(childColor); // CDS same color as exon
                    tierGlyph.setDataModelFromOriginalSym(cds_glyph, cdsSym2);
                    pglyph.addChild(cds_glyph);
                    codon_glyph_processor.processGlyph(cds_glyph, cdsSym2, annotSeq);
                }
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleInsertionGlyphs(SeqSymmetry sym, GlyphI pglyph) {
        if (!(sym instanceof BAMSym)) {
            return;
        }

        BAMSym inssym = (BAMSym) sym;
        if (inssym.getInsChildCount() == 0) {
            return;
        }

        BioSeq coordseq = seqMap.getViewSeq();
        SeqSymmetry psym = inssym;
        if (annotSeq != viewSeq) {
            psym = seqMap.transformForViewSeq(inssym, annotSeq);
        }
        SeqSpan pspan = seqMap.getViewSeqSpan(psym);

        Color color = Color.RED;

        for (int i = 0; i < inssym.getInsChildCount(); i++) {

            SeqSymmetry childsym = inssym.getInsChild(i);
            SeqSymmetry dsym = childsym;

            if (annotSeq != coordseq) {
                dsym = seqMap.transformForViewSeq(childsym, annotSeq);
            }
            SeqSpan dspan = seqMap.getViewSeqSpan(dsym);
            SeqSpan ispan = childsym.getSpan(annotSeq);

            if (ispan == null || dspan == null) {
                continue;
            }

            InsertionSeqGlyph isg = new InsertionSeqGlyph();
            isg.setSelectable(true);
            String residues = inssym.getResidues(ispan.getMin() - 1, ispan.getMin() + 1);
            isg.setResidues(residues);
            isg.setCoords(Math.max(pspan.getMin(), dspan.getMin() - 1), 0, residues.length(), DEFAULT_CHILD_HEIGHT);
            isg.setColor(color);

            pglyph.addChild(isg);
            tierGlyph.setDataModelFromOriginalSym(isg, childsym);
        }
    }

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        checkNotNull(sym);
        checkNotNull(style);
        checkNotNull(seq);
        checkNotNull(gviewer);
        //apply preprocessors
        for (SeqSymmetryPreprocessorI preprocessor : PreprocessorRegistry.getPreprocessorsForType(FileTypeCategory.Annotation)) {
            preprocessor.process(sym, style, gviewer, seq);
        }
        setSeqMap(gviewer);
        if (sym != null) {
            int glyphDepth = style.getGlyphDepth();
            StyledGlyph.Direction tierDirection = !style.getSeparable() ? StyledGlyph.Direction.BOTH : StyledGlyph.Direction.FORWARD;
            TierGlyph forwardTier = gviewer.getTrack(style, tierDirection);
            forwardTier.setTierType(TierGlyph.TierType.ANNOTATION);
            forwardTier.setInfo(sym);
            if (style.getSeparate()) {
                TierGlyph reverse_tier = (tierDirection == StyledGlyph.Direction.BOTH) ? forwardTier : gviewer.getTrack(style, StyledGlyph.Direction.REVERSE);
                reverse_tier.setTierType(TierGlyph.TierType.ANNOTATION);
                reverse_tier.setInfo(sym);
                setTrack(new Track(forwardTier, reverse_tier));
                addLeafsToTier(sym, glyphDepth, style);
                doMiddlegroundShading(reverse_tier, gviewer, seq);
            } else {
                // use only one tier
                setTrack(new Track(forwardTier));
                addLeafsToTier(sym, glyphDepth, style);
            }
            doMiddlegroundShading(forwardTier, gviewer, seq);
        }
    }

    @Override
    public void createGlyphs(RootSeqSymmetry rootSym, List<? extends SeqSymmetry> syms, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        checkNotNull(syms);
        checkNotNull(style);
        checkNotNull(seq);
        checkNotNull(gviewer);
        setSeqMap(gviewer);
        StyledGlyph.Direction useDirection = (!style.getSeparable()) ? StyledGlyph.Direction.BOTH : StyledGlyph.Direction.FORWARD;
        TierGlyph forward_tier = gviewer.getTrack(style, useDirection);
        forward_tier.setTierType(TierGlyph.TierType.ANNOTATION);
        forward_tier.setInfo(rootSym);

        TierGlyph reverse_tier = (useDirection == StyledGlyph.Direction.BOTH) ? forward_tier : gviewer.getTrack(style, StyledGlyph.Direction.REVERSE);
        reverse_tier.setTierType(TierGlyph.TierType.ANNOTATION);
        forward_tier.setInfo(rootSym);
        setTrack(new Track(forward_tier, reverse_tier));
        addToTier(syms);
        doMiddlegroundShading(forward_tier, gviewer, seq);
        doMiddlegroundShading(reverse_tier, gviewer, seq);
    }

    public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI gviewer) {
        checkNotNull(sym);
        checkNotNull(gviewer);
        setSeqMap(gviewer);

        Optional<ITrackStyleExtended> style = SeqUtils.getSymTrackStyle(sym);
        if (!style.isPresent()) {
            return;
        }
        StyledGlyph.Direction useDirection = (!style.get().getSeparable()) ? StyledGlyph.Direction.BOTH : StyledGlyph.Direction.FORWARD;
        TierGlyph forward_tier = seqMap.getTrack(style.get(), useDirection);
        forward_tier.setTierType(TierGlyph.TierType.ANNOTATION);

        TierGlyph reverse_tier = (useDirection == StyledGlyph.Direction.BOTH) ? forward_tier : seqMap.getTrack(style.get(), StyledGlyph.Direction.REVERSE);
        reverse_tier.setTierType(TierGlyph.TierType.ANNOTATION);
        setTrack(new Track(forward_tier, reverse_tier));
        int depth = SeqUtils.getDepthFor(sym);
        drawChildren = (depth >= 2);
        addTopChild(sym);
    }

    private void updateAnnotSeq() {
        annotSeq = seqMap.getAnnotatedSeq();
    }

    private void updateViewSeq() {
        viewSeq = seqMap.getViewSeq();
    }

    private SeqSymmetry initSymSpan(SeqSymmetry originalSym) {
        SeqSymmetry sym;
        updateAnnotSeq();
        updateViewSeq();
        sym = seqMap.transformForViewSeq(originalSym, annotSeq);
        return sym;
    }

    private void updateSymSpan(SeqSymmetry sym) {
        symSpan = seqMap.getViewSeqSpan(sym);
        isValidSymSpan = !(symSpan == null || symSpan.getLength() == 0);
    }

    private void setTierGlyph() {
        if (!symSpan.isForward()) {
            if (track.getReverseTier().isPresent()) {
                tierGlyph = track.getReverseTier().get();
            } else {
                tierGlyph = track.getForwardTier();
            }
        } else {
            tierGlyph = track.getForwardTier();
        }
        trackStyle = tierGlyph.getAnnotStyle();
    }

    protected void setTrack(Track track) {
        this.track = track;
    }

    private void setSeqMap(SeqMapViewExtendedI seqMap) {
        this.seqMap = seqMap;
    }

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }

    @Override
    public Set<FileTypeCategory> getSupportedCategories() {
        return ImmutableSet.<FileTypeCategory>builder()
                .add(FileTypeCategory.Alignment)
                .add(FileTypeCategory.Annotation).build();
    }

}
