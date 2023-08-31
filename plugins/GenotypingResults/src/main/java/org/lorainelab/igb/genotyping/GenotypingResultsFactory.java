package org.lorainelab.igb.genotyping;

import org.osgi.service.component.annotations.Component;
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
import com.affymetrix.genometry.symmetry.impl.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.glyph.SoftClippingSeqGlyph;
import com.affymetrix.igb.glyph.TriangleInsertionSeqGlyph;
import com.affymetrix.igb.shared.PreprocessorRegistry;
import com.affymetrix.igb.view.factories.MapTierGlyphFactoryA;
import com.affymetrix.igb.view.factories.MapTierGlyphFactoryI;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import org.lorainelab.igb.services.visualization.SeqSymmetryPreprocessorI;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

@Component(name = GenotypingResultsFactory.COMPONENT_NAME, service = {MapTierGlyphFactoryI.class}, immediate = true)
public class GenotypingResultsFactory extends MapTierGlyphFactoryA {
    public static final String COMPONENT_NAME = "GenotypingResultsFactory";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GenotypingResultsFactory.class);
    private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###.###");

    protected SeqMapViewExtendedI seqMap;
    private BioSeq annotSeq;
    private BioSeq viewSeq;
    //span of the symmetry that is on the seq being viewed
    private SeqSpan symSpan;
    private boolean isValidSymSpan;
    boolean drawChildren;
    private Track track;
    private TierGlyph tierGlyph;
    private ITrackStyleExtended trackStyle;

    private static final Class<?> UNLABELLED_PARENT_GLYPH_CLASS = (new TwentyThreeAndMeVariationGlyph()).getClass();
    private static final Class<?> CHILD_GLYPH_CLASS = (new TwentyThreeAndMeVariationGlyph()).getClass();
    private static final Class<?> LABLELLED_PARENT_GLYPH_CLASS = (new TwentyThreeAndMeVariationGlyph()).getClass();

    public GenotypingResultsFactory() {
    }

    @Override
    public void init(Map<String, Object> options) {
    }

    @Override
    public boolean supportsTwoTrack() {
        return false;
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

        //TrackConstants.DirectionType directionType = TrackConstants.DirectionType.valueFor(trackStyle.getDirectionType());
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
                    handleSoftClippingGlyphs(sym, parentGlyph.get());
                }
            }

        } else {
            // depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
            parentGlyph = determineGlyph(CHILD_GLYPH_CLASS, sym);
/*            if (parentGlyph.isPresent()) {
                addAlignedResiduesGlyph(sym, symSpan, DEFAULT_CHILD_HEIGHT, parentGlyph.get());
            }*/
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
            handleSoftClippingGlyphs(sym1, firstChildPglyph.get());
            addChildren(sym2, secondChildPglyph.get());
            handleInsertionGlyphs(sym2, secondChildPglyph.get());
            handleSoftClippingGlyphs(sym2, secondChildPglyph.get());
            parentGlyph.addChild(firstChildPglyph.get());
            parentGlyph.addChild(secondChildPglyph.get());
        }

    }

    private Optional<GlyphI> determineGlyph(Class<?> glyphClass, SeqSymmetry sym) {
        TwentyThreeAndMeVariationGlyph pglyph = null;
        try {
            SeqSpan span = seqMap.getViewSeqSpan(sym);
            Color color = getSymColor(sym);
            boolean labelInSouth = !span.isForward() && track.getReverseTier().isPresent();

            // Note: Setting parent height (pheight) larger than the child height (cheight)
            // allows the user to select both the parent and the child as separate entities
            // in order to look at the properties associated with them.  Otherwise, the method
            // EfficientGlyph.pickTraversal() will only allow one to be chosen.
            double pheight = DEFAULT_CHILD_HEIGHT + 0.0001;
            if ((sym instanceof MultiTierSymWrapper) && com.affymetrix.igb.view.factories.AbstractTierGlyph.useLabel(trackStyle)) {
                pglyph = TwentyThreeAndMeVariationGlyph.class.newInstance();
            } else if (com.affymetrix.igb.view.factories.AbstractTierGlyph.useLabel(trackStyle)) {
                TwentyThreeAndMeVariationGlyph lglyph = (TwentyThreeAndMeVariationGlyph) LABLELLED_PARENT_GLYPH_CLASS.newInstance();
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
                pglyph = (TwentyThreeAndMeVariationGlyph) glyphClass.newInstance();
            }
            if (sym instanceof MultiTierSymWrapper) {
                pglyph.setDirection(NeoConstants.NONE);
                color = Color.BLACK;
            } else {
            }
            pglyph.setCoords(span.getMin(), 0, span.getLength(), pheight);
            pglyph.setColor(color);
            tierGlyph.setDataModelFromOriginalSym(pglyph, sym);
        } catch (InstantiationException ex) {
            Logger.getLogger(GenotypingResultsFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GenotypingResultsFactory.class.getName()).log(Level.SEVERE, null, ex);
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
                //addAlignedResiduesGlyph(child, cspan, cheight, parentGlyph);

                if (!cds) {
                    handleCDSSpan(cdsSpan, cspan, cds_sym, child, sym, color, parentGlyph);
                }
            }
        }

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
        }
    }

    private static Optional<Object> getTheProperty(SeqSymmetry sym, String prop) {
        if (Strings.isNullOrEmpty(prop)) {
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

    private Optional<GlyphI> getChild(SeqSpan cspan, boolean isFirst, boolean isLast) {
        try {
            return Optional.ofNullable((GlyphI) CHILD_GLYPH_CLASS.newInstance());
        } catch (InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(GenotypingResultsFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.<GlyphI>ofNullable(null);
    }

    private Color getSymColor(SeqSymmetry insym) {
        Color color = null;
        SeqSpan span = seqMap.getViewSeqSpan(insym);
        ColorProviderI cp = trackStyle.getColorProvider();
        //TrackConstants.DirectionType directionType = TrackConstants.DirectionType.valueFor(trackStyle.getDirectionType());
        boolean isForward = span.isForward();
        if (cp != null) {
            SeqSymmetry sym = insym;
            if (insym instanceof DerivedSeqSymmetry) {
                sym = getMostOriginalSymmetry(insym);
            }
            color = cp.getColor(sym);
        }

        if (color == null) {
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
                    GlyphI cds_glyph = (GlyphI) CHILD_GLYPH_CLASS.newInstance();
                    cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), DEFAULT_CHILD_HEIGHT);
                    cds_glyph.setColor(childColor); // CDS same color as exon
                    tierGlyph.setDataModelFromOriginalSym(cds_glyph, cdsSym2);
                    pglyph.addChild(cds_glyph);
                    if (cds_span.getLength() > 2) { // sanity check, narrowpeak uses cds for peak and should not have any code glyph child
                    }
                }
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(GenotypingResultsFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GenotypingResultsFactory.class.getName()).log(Level.SEVERE, null, ex);
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

            TriangleInsertionSeqGlyph triangleInsertionGlyph = new TriangleInsertionSeqGlyph();
            triangleInsertionGlyph.setSelectable(true);
            String residues = inssym.getResidues(ispan.getMin() - 1, ispan.getMin() + 1);
            triangleInsertionGlyph.setResidues(residues);
            triangleInsertionGlyph.setCoords(Math.max(pspan.getMin(), dspan.getMin() - 1), 0, residues.length(), DEFAULT_CHILD_HEIGHT);
            pglyph.addChild(triangleInsertionGlyph);
            tierGlyph.setDataModelFromOriginalSym(triangleInsertionGlyph, childsym);
        }
    }

    private void handleSoftClippingGlyphs(SeqSymmetry sym, GlyphI pglyph) {
        if (!(sym instanceof BAMSym)) {
            return;
        }

        BAMSym softsym = (BAMSym) sym;
        if (softsym.getSoftChildCount() == 0) {
            return;
        }

        BioSeq coordseq = seqMap.getViewSeq();
        SeqSymmetry psym = softsym;
        if (annotSeq != viewSeq) {
            psym = seqMap.transformForViewSeq(softsym, annotSeq);
        }
        SeqSpan pspan = seqMap.getViewSeqSpan(psym);

        for (int i = 0; i < softsym.getSoftChildCount(); i++) {

            SeqSymmetry childsym = softsym.getSoftChild(i);
            SeqSymmetry dsym = childsym;

            if (annotSeq != coordseq) {
                dsym = seqMap.transformForViewSeq(childsym, annotSeq);
            }
            SeqSpan dspan = seqMap.getViewSeqSpan(dsym);
            SeqSpan ispan = childsym.getSpan(annotSeq);

            if (ispan == null || dspan == null) {
                continue;
            }

            if(!trackStyle.getShowSoftClipped()){
                String residues = softsym.getResidues(ispan.getMin(), ispan.getMax());
                SoftClippingSeqGlyph softClippingGlyph = new SoftClippingSeqGlyph();

                if(trackStyle.getShowSoftClippedResidues()) {
                    //addAlignedResiduesGlyph(childsym, ispan, DEFAULT_CHILD_HEIGHT, pglyph);
                    softClippingGlyph.setCoords(Math.max(pspan.getMin(), dspan.getMin()), 0, residues.length(), DEFAULT_CHILD_HEIGHT);
                    softClippingGlyph.setSelectable(true);
                    softClippingGlyph.setShowBackground(false);
                } else {
                    softClippingGlyph.setCoords(Math.max(pspan.getMin(), dspan.getMin()), 0, residues.length(), DEFAULT_CHILD_HEIGHT);
                    softClippingGlyph.setSelectable(true);
                    softClippingGlyph.setColor(trackStyle.getsoftClipColor());
                    softClippingGlyph.setResidues(residues);
                }

                pglyph.addChild(softClippingGlyph);
                tierGlyph.setDataModelFromOriginalSym(softClippingGlyph, childsym);
            }
        }
    }

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        checkNotNull(sym);
        checkNotNull(style);
        checkNotNull(seq);
        checkNotNull(gviewer);
        //apply preprocessors
        for (SeqSymmetryPreprocessorI preprocessor : PreprocessorRegistry.getPreprocessorsForType(FileTypeCategory.PersonalGenomics)) {
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
      //        return ImmutableSet.<FileTypeCategory>builder()
      //          .add(FileTypeCategory.Annotation).build();
        return ImmutableSet.<FileTypeCategory>builder()
                .add(FileTypeCategory.PersonalGenomics).build();
    }
}
