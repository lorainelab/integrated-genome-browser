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

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.color.ColorProviderI;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.PairedBamSymWrapper;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.BioSeqUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.genoviz.glyph.EfficientLineContGlyph;
import com.affymetrix.genoviz.glyph.EfficientOutlinedRectGlyph;
import com.affymetrix.genoviz.glyph.EfficientSolidGlyph;
import com.affymetrix.genoviz.glyph.InsertionSeqGlyph;
import com.affymetrix.genoviz.glyph.PointedGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.shared.AlignedResidueGlyph;
import com.affymetrix.igb.shared.CodonGlyphProcessor;
import com.affymetrix.igb.shared.DeletionGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import static com.affymetrix.igb.shared.MapTierGlyphFactoryI.DEFAULT_CHILD_HEIGHT;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TrackConstants.DirectionType;
import com.google.common.base.Optional;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @version $Id: AnnotationGlyphFactory.java 10247 2012-02-10 16:36:20Z lfrohman
 * $
 */
public class AnnotationGlyphFactory extends MapTierGlyphFactoryA {
    
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
    private SeqMapViewExtendedI seqMap;
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
    
    protected void addLeafsToTier(SeqSymmetry originalSym, int desired_leaf_depth) {
        int depth = SeqUtils.getDepthFor(originalSym);
        drawChildren = (depth >= 2);
        if (originalSym instanceof PairedBamSymWrapper) {
            addToTier(originalSym);
            return;
        }
        if (depth > desired_leaf_depth || originalSym instanceof TypeContainerAnnot) {
            int childCount = originalSym.getChildCount();
            for (int i = 0; i < childCount; i++) {
                addLeafsToTier(originalSym.getChild(i), desired_leaf_depth);
            }
        } else {  // depth == desired_leaf_depth
            addToTier(originalSym);
        }
    }
    
    private void addToTier(List<? extends SeqSymmetry> insyms) {
        for (SeqSymmetry insym : insyms) {
            int depth = SeqUtils.getDepthFor(insym);
            drawChildren = (depth >= 2);
            addToTier(insym);
        }
    }
    
    protected void addToTier(SeqSymmetry originalSym) {
        SeqSymmetry sym = originalSym;
        updateAnnotSeq();
        updateViewSeq();
        if (annotSeq != viewSeq) {
            sym = seqMap.transformForViewSeq(originalSym, annotSeq);
        }
        if (sym == null) {
            return;
        }
        addTopChild(originalSym, sym);
    }
    
    protected void addTopChild(SeqSymmetry originalSym, SeqSymmetry sym) {
        updateSymSpan(sym);
        if (!isValidSymSpan) {
            return;
        }
        TierGlyph tierGlyph = getTierGlyph(track);
        Optional<GlyphI> glyph = determinePGlyph(originalSym, sym);
        if (glyph.isPresent()) {
            ITrackStyleExtended trackStyle = tierGlyph.getAnnotStyle();
            if (trackStyle.getFilter() != null) {
                glyph.get().setVisibility(trackStyle.getFilter().filterSymmetry(annotSeq, sym));
            }
            tierGlyph.addChild(glyph.get());
        }
    }
    
    protected Optional<GlyphI> determinePGlyph(SeqSymmetry originalSym, SeqSymmetry sym) {
        Optional<GlyphI> parentGlyph;
        TierGlyph tierGlyph = getTierGlyph(track);
        ITrackStyleExtended trackStyle = tierGlyph.getAnnotStyle();
        DirectionType directionType = DirectionType.valueFor(trackStyle.getDirectionType());
        Color color = getSymColor(originalSym, trackStyle, directionType);
        if (drawChildren && originalSym.getChildCount() > 0) {
            parentGlyph = determineGlyph(UNLABELLED_PARENT_GLYPH_CLASS, originalSym, sym, color);
            // call out to handle rendering to indicate if any of the children of the
            //    original annotation are completely outside the view
            if (parentGlyph.isPresent()) {
                if (originalSym instanceof PairedBamSymWrapper) {
                    handlePairedChildren((PairedBamSymWrapper) originalSym, sym, parentGlyph.get(), color);
                } else {
                    addChildren(originalSym, sym, parentGlyph.get(), color);
                    handleInsertionGlyphs(tierGlyph, originalSym, parentGlyph.get());
                }
            }
            
        } else {
            // depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
            parentGlyph = determineGlyph(CHILD_GLYPH_CLASS, originalSym, sym, color);
            if (parentGlyph.isPresent()) {
                if (directionType == DirectionType.ARROW || directionType == DirectionType.BOTH) {
                    addChildGlyph(sym, symSpan, directionType, parentGlyph.get().getCoordBox().getHeight(), color, false, parentGlyph.get());
                }
                addAlignedResiduesGlyph(originalSym, symSpan, DEFAULT_CHILD_HEIGHT, parentGlyph.get());
            }
        }
        return parentGlyph;
    }
    
    private void handlePairedChildren(PairedBamSymWrapper originalSym, SeqSymmetry sym, GlyphI parentGlyph, Color color) {
        TierGlyph tierGlyph = getTierGlyph(track);
        Optional<GlyphI> firstChildPglyph;
        Optional<GlyphI> secondChildPglyph;
        SeqSymmetry originalSym1 = originalSym.getChild(0);
        SeqSymmetry sym1 = originalSym1;
        SeqSymmetry originalSym2 = originalSym.getChild(1);
        SeqSymmetry sym2 = originalSym2;
        
        if (annotSeq != viewSeq) {
            sym1 = seqMap.transformForViewSeq(originalSym1, annotSeq);
            sym2 = seqMap.transformForViewSeq(originalSym2, annotSeq);
        }
        
        firstChildPglyph = determineGlyph(UNLABELLED_PARENT_GLYPH_CLASS, originalSym1, sym1, color);
        secondChildPglyph = determineGlyph(UNLABELLED_PARENT_GLYPH_CLASS, originalSym2, sym2, color);
        
        if (firstChildPglyph.isPresent() && secondChildPglyph.isPresent()) {
            addChildren(originalSym1, sym1, firstChildPglyph.get(), color);
            handleInsertionGlyphs(tierGlyph, originalSym1, firstChildPglyph.get());
            addChildren(originalSym2, sym2, secondChildPglyph.get(), color);
            handleInsertionGlyphs(tierGlyph, originalSym2, secondChildPglyph.get());
            
            parentGlyph.addChild(firstChildPglyph.get());
            parentGlyph.addChild(secondChildPglyph.get());
        }
        
    }
    
    private Optional<GlyphI> determineGlyph(Class<?> glyphClass, SeqSymmetry originalSym, SeqSymmetry sym, Color color) {
        EfficientSolidGlyph pglyph = null;
        //override class variable to handle special paired child special case
        SeqSpan symSpan = seqMap.getViewSeqSpan(originalSym);
        try {
            TierGlyph tierGlyph = getTierGlyph(track);
            boolean labelInSouth = !symSpan.isForward() && track.getReverseTier().isPresent();
            ITrackStyleExtended trackStyle = tierGlyph.getAnnotStyle();
            DirectionType directionType = DirectionType.valueFor(trackStyle.getDirectionType());

            // Note: Setting parent height (pheight) larger than the child height (cheight)
            // allows the user to select both the parent and the child as separate entities
            // in order to look at the properties associated with them.  Otherwise, the method
            // EfficientGlyph.pickTraversal() will only allow one to be chosen.
            double pheight = DEFAULT_CHILD_HEIGHT + 0.0001;
            if (AbstractTierGlyph.useLabel(trackStyle)) {
                
                EfficientLabelledGlyph lglyph = (EfficientLabelledGlyph) LABLELLED_PARENT_GLYPH_CLASS.newInstance();
                Optional<Object> property = getTheProperty(originalSym, trackStyle.getLabelField());
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
            pglyph.setCoords(symSpan.getMin(), 0, symSpan.getLength(), pheight);
            pglyph.setColor(color);
            if (directionType == DirectionType.ARROW || directionType == DirectionType.BOTH) {
                pglyph.setDirection(symSpan.isForward() ? NeoConstants.RIGHT : NeoConstants.LEFT);
            }
            tierGlyph.setDataModelFromOriginalSym(pglyph, sym);
        } catch (InstantiationException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.<GlyphI>fromNullable(pglyph);
    }
    
    private void addChildren(SeqSymmetry originalSym, SeqSymmetry sym, GlyphI parentGlyph, Color color) {
        SeqSpan cdsSpan = null;
        SeqSymmetry cds_sym = null;
        TierGlyph tierGlyph = getTierGlyph(track);
        ITrackStyleExtended trackStyle = tierGlyph.getAnnotStyle();
        boolean sameSeq = annotSeq == viewSeq;
        if ((originalSym instanceof SupportsCdsSpan) && ((SupportsCdsSpan) originalSym).hasCdsSpan()) {
            cdsSpan = ((SupportsCdsSpan) originalSym).getCdsSpan();
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
        List<SeqSymmetry> outsideChildren = new ArrayList<SeqSymmetry>();
        DirectionType directionType = DirectionType.valueFor(trackStyle.getDirectionType());
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
                
                addChildGlyph(child, cspan, directionType, cheight, color, childCount > 1, parentGlyph);
                addAlignedResiduesGlyph(child, cspan, cheight, parentGlyph);
                
                if (!cds) {
                    handleCDSSpan(cdsSpan, cspan, cds_sym, child, originalSym, color, parentGlyph);
                }
            }
        }

//		ArrowHeadGlyph.addDirectionGlyphs(gviewer, sym, pglyph, viewSeq, viewSeq, 0.0, 
//			thin_height, the_style.getDirectionType() == DirectionType.ARROW.ordinal());
        // call out to handle rendering to indicate if any of the children of the
        //    orginal annotation are completely outside the view
        DeletionGlyph.handleEdgeRendering(outsideChildren, parentGlyph, annotSeq, viewSeq, 0.0, thinHeight);
    }
    
    private void addChildGlyph(SeqSymmetry sym, SeqSpan pspan, DirectionType directionType, double height, Color color, boolean hitable, GlyphI pglyph) {
        TierGlyph tierGlyph = getTierGlyph(track);
        GlyphI cglyph;
        Optional<GlyphI> child = getChild(symSpan, symSpan.getMin() == pspan.getMin(), symSpan.getMax() == pspan.getMax(), directionType);
        if (child.isPresent()) {
            cglyph = child.get();
            cglyph.setCoords(symSpan.getMin(), 0, symSpan.getLength(), height);
            cglyph.setColor(color);
            tierGlyph.setDataModelFromOriginalSym(cglyph, sym);
            pglyph.addChild(cglyph);
            
            cglyph.setHitable(hitable);
            if (cglyph instanceof DirectedGlyph) {
                ((DirectedGlyph) cglyph).setForward(symSpan.isForward());
            }
            codon_glyph_processor.processGlyph(cglyph, sym, annotSeq);
        }
    }
    
    private static Optional<Object> getTheProperty(SeqSymmetry sym, String prop) {
        if (StringUtils.isBlank(prop)) {
            return Optional.absent();
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
            
            return Optional.fromNullable(ret);
        }
        return Optional.absent();
    }
    
    private void addAlignedResiduesGlyph(SeqSymmetry sym, SeqSpan span, double height, GlyphI pglyph) {
        TierGlyph tierGlyph = getTierGlyph(track);
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
    
    private Optional<GlyphI> getChild(SeqSpan cspan, boolean isFirst, boolean isLast, DirectionType directionType) {
        try {
            if (cspan.getLength() == 0) {
                return Optional.<GlyphI>fromNullable(new DeletionGlyph());
            } else if (((isLast && cspan.isForward()) || (isFirst && !cspan.isForward()))
                    && (directionType == DirectionType.ARROW || directionType == DirectionType.BOTH)) {
                return Optional.<GlyphI>fromNullable(new PointedGlyph());
            }
            return Optional.<GlyphI>fromNullable((GlyphI) CHILD_GLYPH_CLASS.newInstance());
        } catch (InstantiationException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AnnotationGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.<GlyphI>fromNullable(null);
    }
    
    private Color getSymColor(SeqSymmetry insym, ITrackStyleExtended style, DirectionType directionType) {
        Color color = null;
        ColorProviderI cp = style.getColorProvider();
        boolean isForward = symSpan.isForward();
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
                    return style.getForwardColor();
                }
                return style.getReverseColor();
            }
            return style.getForeground();
        }
        
        return color;
    }
    
    private void handleCDSSpan(SeqSpan cdsSpan, SeqSpan childSpan, SeqSymmetry cdsSym, SeqSymmetry child, SeqSymmetry originalSym, Color childColor, GlyphI pglyph) {
        try {
            TierGlyph tierGlyph = getTierGlyph(track);
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
    
    private void handleInsertionGlyphs(TierGlyph tierGlyph, SeqSymmetry sym, GlyphI pglyph) {
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
        setSeqMap(gviewer);
        if (sym != null) {
            int glyph_depth = style.getGlyphDepth();
            TierGlyph.Direction useDirection = (!style.getSeparable()) ? TierGlyph.Direction.BOTH : TierGlyph.Direction.FORWARD;
            TierGlyph forward_tier = gviewer.getTrack(style, useDirection);
            forward_tier.setTierType(TierGlyph.TierType.ANNOTATION);
            forward_tier.setInfo(sym);
            if (style.getSeparate()) {
                TierGlyph reverse_tier = (useDirection == TierGlyph.Direction.BOTH) ? forward_tier : gviewer.getTrack(style, TierGlyph.Direction.REVERSE);
                reverse_tier.setTierType(TierGlyph.TierType.ANNOTATION);
                reverse_tier.setInfo(sym);
                setTrack(new Track(forward_tier, reverse_tier));
                addLeafsToTier(sym, glyph_depth);
                doMiddlegroundShading(reverse_tier, gviewer, seq);
            } else {
                // use only one tier
                setTrack(new Track(forward_tier));
                addLeafsToTier(sym, glyph_depth);
            }
            doMiddlegroundShading(forward_tier, gviewer, seq);
        }
    }
    
    @Override
    public void createGlyphs(RootSeqSymmetry rootSym, List<? extends SeqSymmetry> syms, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        setSeqMap(gviewer);
        TierGlyph.Direction useDirection = (!style.getSeparable()) ? TierGlyph.Direction.BOTH : TierGlyph.Direction.FORWARD;
        TierGlyph forward_tier = gviewer.getTrack(style, useDirection);
        forward_tier.setTierType(TierGlyph.TierType.ANNOTATION);
        forward_tier.setInfo(rootSym);
        
        TierGlyph reverse_tier = (useDirection == TierGlyph.Direction.BOTH) ? forward_tier : gviewer.getTrack(style, TierGlyph.Direction.REVERSE);
        reverse_tier.setTierType(TierGlyph.TierType.ANNOTATION);
        forward_tier.setInfo(rootSym);
        setTrack(new Track(forward_tier, reverse_tier));
        addToTier(syms);
        doMiddlegroundShading(forward_tier, gviewer, seq);
        doMiddlegroundShading(reverse_tier, gviewer, seq);
    }
    
    public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI gviewer) {
        setSeqMap(gviewer);
        String method = BioSeqUtils.determineMethod(sym);
        if (method == null) {
            return;
        }
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method);
        TierGlyph.Direction useDirection = (!style.getSeparable()) ? TierGlyph.Direction.BOTH : TierGlyph.Direction.FORWARD;
        TierGlyph forward_tier = seqMap.getTrack(style, useDirection);
        forward_tier.setTierType(TierGlyph.TierType.ANNOTATION);
        
        TierGlyph reverse_tier = (useDirection == TierGlyph.Direction.BOTH) ? forward_tier : seqMap.getTrack(style, TierGlyph.Direction.REVERSE);
        reverse_tier.setTierType(TierGlyph.TierType.ANNOTATION);
        setTrack(new Track(forward_tier, reverse_tier));
        int depth = SeqUtils.getDepthFor(sym);
        drawChildren = (depth >= 2);
        addToTier(sym);
    }
    
    @Override
    public String getName() {
        return "annotation/alignment";
    }
    
    private void updateAnnotSeq() {
        annotSeq = seqMap.getAnnotatedSeq();
    }
    
    private void updateViewSeq() {
        viewSeq = seqMap.getViewSeq();
    }
    
    private void updateSymSpan(SeqSymmetry sym) {
        symSpan = seqMap.getViewSeqSpan(sym);
        isValidSymSpan = !(symSpan == null || symSpan.getLength() == 0);
    }
    
    private TierGlyph getTierGlyph(Track track) {
        if (!symSpan.isForward()) {
            if (track.getReverseTier().isPresent()) {
                return track.getReverseTier().get();
            } else {
                return track.getForwardTier();
            }
        }
        return track.getForwardTier();
    }
    
    private void setTrack(Track track) {
        this.track = track;
    }
    
    private void setSeqMap(SeqMapViewExtendedI seqMap) {
        this.seqMap = seqMap;
    }
    
}
