package com.affymetrix.igb.view.factories;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.genoviz.glyph.EfficientLineContGlyph;
import com.affymetrix.genoviz.glyph.EfficientOutlineContGlyph;
import com.affymetrix.genoviz.glyph.EfficientOutlinedRectGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.LineContainerGlyph;
import com.affymetrix.genoviz.glyph.PointedGlyph;
import com.affymetrix.igb.shared.DeletionGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import com.affymetrix.igb.shared.MapTierGlyphFactoryI;
import com.lorainelab.igb.genoviz.extensions.api.SeqMapViewExtendedI;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import com.google.common.collect.ImmutableSet;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
@Component(name = ProbeSetGlyphFactory.COMPONENT_NAME, provide = {MapTierGlyphFactoryI.class})
public class ProbeSetGlyphFactory extends MapTierGlyphFactoryA {

    public static final String COMPONENT_NAME = "ProbeSetGlyphFactory";
    /*
     Algorithm for drawing probe-set-display data.

     Find the annotations on the chromosome.
     Recurse through each annotation down to depth=2
     Each of these is a consensus symmetry "CSym"
     Transform each CSym into "View" coordinates: "CSym_x_view"
     Each CSym points to a Consensus Seq: "CSeq"
     Recurse through the annotations of CSeq down to depth=2
     Each of these is a probe set: "PS"
     Transform each PS by the CSym giving "PS_x_Csym" with depth=3
     Transform again for the view "(PS_x_Csym)_x_View" with depth unknown

     In (PS_x_Csym)_x_View, the overal depth is unknown, but you do know
     that the top level is probeset, then probe, then pieces of probes (if split
     across introns)

     If you try to skip a step and transform PS by Csym_x_View
     giving PS_x_(CSym_x_View), you cannot predict at what depth to find
     the probeset, probe and pieces of probes
     */
    private static final boolean DEBUG = false;
    /**
     * Any method name (track-line name) ending with this is taken as a
     * poly_a_site.
     */
    public static final String POLY_A_SITE_METHOD = "netaffx poly_a_sites";
    /**
     * Any method name (track-line name) ending with this is taken as a
     * poly_a_stack.
     */
    public static final String POLY_A_STACK_METHOD = "netaffx poly_a_stacks";
    /**
     * Any method name (track-line name) ending with this is taken as a
     * consensus/exemplar sequence.
     */
    public static final String NETAFFX_CONSENSUS = " netaffx consensus";
    public static final String NETAFFX_PROBESETS = "netaffx probesets";
    private static final Color ps_color = Color.PINK;
    private static final Color poly_a_site_color = Color.BLUE;
    private static final Color poly_a_stack_color = Color.CYAN;
    /**
     * Whether to put an outline around the probe glyphs in the same probeset.
     */
    private static final boolean outline_probes_in_probeset = false;
    private static final int glyph_depth = 2;
    private String label_field = null;
    private static final int GLYPH_HEIGHT = 20;

    /**
     * Recurses children of sym until SeqUtils.getDepth(sym) equals
     * depth_of_consensuses, then calls addToTier.
     *
     * @param sym a SeqSymmetry representing a consensus sequence (or a tree
     * where consensus symmetries are at depth = desired_leaf_depth). Each
     * consensus symmetry represents the mapping of a consensus onto the genome,
     * and can contain "introns" and "exons".
     * @param depth_of_consensuses Depth at which consensus sequences symmetries
     * will be found. Normally should be set to 2.
     */
    private void addLeafsToTier(SeqMapViewExtendedI gviewer, SeqSymmetry sym,
            TierGlyph ftier, TierGlyph rtier,
            int depth_of_consensuses) throws InstantiationException, IllegalAccessException {
        int depth = SeqUtils.getDepth(sym);
        if (depth > depth_of_consensuses) {
            for (int i = 0; i < sym.getChildCount(); i++) {
                SeqSymmetry child = sym.getChild(i);
                addLeafsToTier(gviewer, child, ftier, rtier, depth_of_consensuses);
            }
        } else if (depth < 1) {
            System.out.println("ERROR in ProbeSetDisplayGlyphFactory: at wrong depth.");
        } else {  // 1 <= depth <= depth_of_consensus
            addToTier(gviewer, sym, ftier, rtier);
        }
    }

    /**
     * Creation of genoviz Glyphs for rendering probe set alignments includes
     * transformations used by slice view and other alternative coordinate
     * systems
     */
    private GlyphI addToTier(SeqMapViewExtendedI gviewer, SeqSymmetry consensus_sym,
            TierGlyph forward_tier, TierGlyph reverse_tier)
            throws InstantiationException, IllegalAccessException {

        if (SeqUtils.getDepth(consensus_sym) != glyph_depth) {
            System.out.println("ProbeSetDisplayGlyphFactory: at wrong depth!");
            return null;
        }

        BioSeq annotseq = gviewer.getAnnotatedSeq();
        BioSeq coordseq = gviewer.getViewSeq();

        SeqSymmetry transformed_consensus_sym = gviewer.transformForViewSeq(consensus_sym, gviewer.getAnnotatedSeq());

        SeqSpan pspan = transformed_consensus_sym.getSpan(gviewer.getViewSeq());
        if (pspan == null) {
            // if no span corresponding to ViewSeq, then return.
            // This can easily happen in the Sliced View and is not usually an error
            return null;
        }

        boolean forward = pspan.isForward();

        TierGlyph the_tier = forward ? forward_tier : reverse_tier;
        boolean labelInSouth = !pspan.isForward() && (reverse_tier != forward_tier);

        int parent_height = GLYPH_HEIGHT; // height of the consensus glyph
        // if there is a label, this height value will be adjusted below
        int child_height = GLYPH_HEIGHT; // height of the consensus "exons"
        int parent_y = 100; // irrelevant because packing will move the glyphs around
        int child_y = 100; // relevant relative to parent_y

        ITrackStyleExtended the_style = the_tier.getAnnotStyle();
        TrackConstants.DirectionType direction_type = TrackConstants.DirectionType.valueFor(the_style.getDirectionType());
        Color consensus_color = getSymColor(consensus_sym, the_style, forward, direction_type);

        boolean use_label = (label_field != null && !label_field.equals(TrackConstants.NO_LABEL)
                && (label_field.trim().length() > 0) && (consensus_sym instanceof SymWithProps));
        GlyphI pglyph;
        if (use_label) {
            EfficientLabelledLineGlyph lglyph = new EfficientLabelledLineGlyph();
            lglyph.setMoveChildren(false);
            if (labelInSouth) {
                lglyph.setLabelLocation(GlyphI.SOUTH);
            } else {
                lglyph.setLabelLocation(GlyphI.NORTH);
                child_y += parent_height;
            }
            String label = (String) ((SymWithProps) consensus_sym).getProperty(label_field);
            lglyph.setLabel(label);
            parent_height = 2 * parent_height;
            pglyph = lglyph;
        } else {
            pglyph = new EfficientLineContGlyph();
            ((EfficientLineContGlyph) pglyph).setMoveChildren(false);
        }

        pglyph.setCoords(pspan.getMin(), parent_y, pspan.getLength(), parent_height);
        //System.out.println("PARENT: "+pglyph.getCoordBox().y+", "+pglyph.getCoordBox().height);
        pglyph.setColor(consensus_color);
        the_tier.setDataModelFromOriginalSym(pglyph, transformed_consensus_sym);

        int childCount = transformed_consensus_sym.getChildCount();
        java.util.List<SeqSymmetry> outside_children = new ArrayList<>();

        for (int i = 0; i < childCount; i++) {
            SeqSymmetry child = transformed_consensus_sym.getChild(i);
            SeqSpan cspan = child.getSpan(coordseq);
            if (cspan == null) {
                outside_children.add(child);
            } else {
                GlyphI cglyph = getChild(cspan, cspan.getMin() == pspan.getMin(), cspan.getMax() == pspan.getMax(), direction_type);
                cglyph.setCoords(cspan.getMin(), child_y + child_height / 4, cspan.getLength(), child_height / 2);
                cglyph.setColor(consensus_color);
                pglyph.addChild(cglyph);
                the_tier.setDataModelFromOriginalSym(cglyph, child);
                if (cglyph instanceof DirectedGlyph) {
                    ((DirectedGlyph) cglyph).setForward(cspan.isForward());
                }
            }
        }

//		ArrowHeadGlyph.addDirectionGlyphs(gviewer, transformed_consensus_sym, pglyph, coordseq, coordseq, 
//				child_y + child_height / 4, child_height/2, the_style.getDirectionType() == TrackConstants.DirectionType.ARROW.ordinal());
        // call out to handle rendering to indicate if any of the children of the
        //    orginal annotation are completely outside the view
        DeletionGlyph.handleEdgeRendering(outside_children, pglyph, annotseq, coordseq,
                child_y + child_height / 4, child_height / 2);

        // Add the pglyph to the tier before drawing probesets because probesets
        // calculate their positions relative to the coordinates of the pglyph's coordbox
        // and the coordbox can be moved around by adding the glyph to the tier
        the_tier.addChild(pglyph);

        BioSeq consensus_seq = getConsensusSeq(consensus_sym, annotseq);
        if (consensus_seq != null) {
            drawConsensusAnnotations(gviewer, consensus_seq, consensus_sym, pglyph, the_tier, child_y, child_height);
        }

        return pglyph;
    }

    private GlyphI getChild(SeqSpan cspan, boolean isFirst, boolean isLast, TrackConstants.DirectionType direction_type)
            throws InstantiationException, IllegalAccessException {

        if (cspan.getLength() == 0) {
            return new DeletionGlyph();
        } else if (((isLast && cspan.isForward()) || (isFirst && !cspan.isForward()))
                && (direction_type == TrackConstants.DirectionType.ARROW || direction_type == TrackConstants.DirectionType.BOTH)) {
            return new PointedGlyph();
        }

        return new EfficientOutlinedRectGlyph();
    }

    private static Color getSymColor(SeqSymmetry insym, ITrackStyleExtended style, boolean isForward, TrackConstants.DirectionType direction_type) {
        if (direction_type == TrackConstants.DirectionType.COLOR || direction_type == TrackConstants.DirectionType.BOTH) {
            if (isForward) {
                return style.getForwardColor();
            }
            return style.getReverseColor();
        }
        return style.getForeground();
    }

    /**
     * If given a SeqSymmetry with exactly two Spans, will return the BioSeq of
     * the Span that is NOT the sequence you specify. TODO: This could return
     * more than one consensus sequence
     */
    private static BioSeq getConsensusSeq(SeqSymmetry sym, BioSeq primary_seq) {
        assert primary_seq != null;
        assert sym != null;

        int span_count = sym.getSpanCount();
        if (span_count != 2) {
            // Although this is normally an error, there are conditions where this glyph factory
            // might be used to display things that are not consensus sequences (such as DAS queries)
            // so just return null.
            return null;
        }

        BioSeq consensus_seq = null;
        for (int i = 0; i < 2; i++) {
            BioSeq seq = sym.getSpan(i).getBioSeq();
            if (seq != primary_seq) {
                consensus_seq = seq;
                break;
            }
        }
        if (consensus_seq != null) {
            return consensus_seq;
        } else {
            System.out.println("ProbeSetDisplayGlyphFactory: Consensus Seq is null!");
            return null;
        }
    }

    /**
     * Finds the annotations at depth 2 on the consensus_seq, which are assumed
     * to be probe sets, and draws glyphs for them.
     *
     * @param consensus_seq An BioSeq containing annotations which are probe
     * sets.
     * @param consensus_sym A symmetry of depth 2 that maps the consensus onto
     * the genome. (More generally, it maps the consensus onto
     * SeqMapView.getAnnotatedSeq(). It should NOT have already been transformed
     * to map onto SeqMapView.getViewSeq(), because then we couldn't guarantee
     * that the depth would still be 2.)
     * @param parent_glyph the Glyph representing the consensus sequence
     * @param y coordinate of the "Exon" regions of the consensus glyph
     * @param height height of the "Exon" regions of the consensus glyph
     */
    private void drawConsensusAnnotations(SeqMapViewExtendedI gviewer, BioSeq consensus_seq, SeqSymmetry consensus_sym,
            GlyphI parent_glyph, TierGlyph tier, double y, double height) {
        int annot_count = consensus_seq.getAnnotationCount();
        for (int i = 0; i < annot_count; i++) {
            SeqSymmetry sym = consensus_seq.getAnnotation(i);
            // probe sets and poly-A sites (and everything else) all get sent
            // to handleConsensusAnnotations, because the first few steps are the same
            handleConsensusAnnotations(gviewer, tier, sym, consensus_sym, parent_glyph,
                    y, height);
        }
    }

    private void handleConsensusAnnotations(SeqMapViewExtendedI gviewer, TierGlyph the_tier, SeqSymmetry sym_with_probesets, SeqSymmetry consensus_sym,
            GlyphI parent_glyph, double y, double height) {
        // Iterate until reaching depth=2 which represents a probeset (depth=2) containing probes (depth=1)
        int depth = SeqUtils.getDepth(sym_with_probesets);
        if (depth == 2) {
            drawConsensusAnnotation(gviewer, the_tier, sym_with_probesets, consensus_sym, parent_glyph, y, height);
        } else {
            int child_count = sym_with_probesets.getChildCount();
            for (int i = 0; i < child_count; i++) {
                SeqSymmetry child = sym_with_probesets.getChild(i);
                handleConsensusAnnotations(gviewer, the_tier, child, consensus_sym, parent_glyph, y, height);
            }
        }
    }

    /**
     * Draws a probeset or poly-A site as a child of a parent_glyph.
     *
     * @param probeset a symmetry representing a "probeset" containing "probes"
     * or representing a "poly-A region". Should be of depth 2. If depth>2,
     * deeper children are ignored.
     * @param consensus_sym a symmetry of depth 2 which can be used to transform
     * the probeset to the SeqMapView.getAnnotatedSeq() coordinates. If the
     * depth is not 2, it is not likely that things would go well, so the method
     * prints an error and returns.
     */
    private void drawConsensusAnnotation(SeqMapViewExtendedI gviewer, TierGlyph the_tier, SeqSymmetry probeset, SeqSymmetry consensus_sym,
            GlyphI parent_glyph, double y, double height) {
        if (DEBUG) {
            int consensus_depth = SeqUtils.getDepth(consensus_sym);
            if (consensus_depth != 2) {
                System.out.println("***************** ERROR: consensus_depth is not 2, but is " + consensus_depth);
                return;
            }
        }
        String meth = BioSeqUtils.determineMethod(probeset);
        DerivedSeqSymmetry probeset_sym = SeqUtils.copyToDerived(probeset);
        SeqUtils.transformSymmetry(probeset_sym, consensus_sym, true);
        // Note that the transformation generates a probeset_sym of depth 3

        if (meth != null && meth.endsWith(POLY_A_SITE_METHOD)) {
            drawPolyA(gviewer, the_tier, probeset_sym, parent_glyph, y, height, poly_a_site_color);
        } else if (meth != null && meth.contains(POLY_A_STACK_METHOD)) {
            drawPolyA(gviewer, the_tier, probeset_sym, parent_glyph, y, height, poly_a_stack_color);
        } else {
            drawProbeSetGlyph(gviewer, the_tier, probeset_sym, parent_glyph, y, height);
        }
    }

    private void drawPolyA(SeqMapViewExtendedI gviewer, TierGlyph the_tier,
            DerivedSeqSymmetry poly_A_sym, GlyphI consensus_glyph,
            double consensus_exon_y, double consensus_exon_height, Color color) {
        // The depth coming in should be 3
        SeqSymmetry transformed_sym = gviewer.transformForViewSeq(poly_A_sym, gviewer.getAnnotatedSeq());
        // After transformation, the depth is arbitrary, but we only deal with the top 3 levels

        SeqSpan span = transformed_sym.getSpan(gviewer.getViewSeq());
        if (span == null) {
            // this means the probeset doesn't map onto the coordinates of the view
            // In the Sliced view, this can happen easily and is not an error.
            return;
        }

        double height = consensus_exon_height / 3;
        double y;

        if (span.isForward()) {
            y = consensus_exon_y;
        } else {
            y = consensus_exon_y + consensus_exon_height - height;
        }

        FillRectGlyph polyA_glyph_rect = new FillRectGlyph();
        polyA_glyph_rect.setColor(color);
        polyA_glyph_rect.setCoords(span.getMin(), y, span.getLength(), height);
        consensus_glyph.addChild(polyA_glyph_rect);
        the_tier.setDataModelFromOriginalSym(polyA_glyph_rect, poly_A_sym);
    }

    /**
     * Draws glyphs for probeset and probes inside the parent glyph.
     *
     * @param probeset_sym A symmetry of depth=3. The top-level symmetry is
     * assumed to be the probeset, the children are assumed to be the probes,
     * the probes can be split into introns/exons. Any levels deper than this
     * third level will be ignored. This should NOT already have been mapped
     * onto SeqMapView.getAnnotatedSeq().
     */
    private void drawProbeSetGlyph(SeqMapViewExtendedI gviewer, TierGlyph the_tier,
            DerivedSeqSymmetry probeset_sym, GlyphI parent_glyph,
            double consensus_exon_y, double consensus_exon_height) {
        // The depth coming in should be 3
        SeqSymmetry transformed_probeset_sym = gviewer.transformForViewSeq(probeset_sym, gviewer.getAnnotatedSeq());
        // After transformation, the depth is arbitrary, but we only deal with the top 3 levels

        SeqSpan span = transformed_probeset_sym.getSpan(gviewer.getViewSeq());
        if (span == null) {
            // this means the probeset doesn't map onto the coordinates of the view
            // In the Sliced view, this can happen easily and is not an error.
            return;
        }

        double probe_height = consensus_exon_height / 3;
        double probe_y = consensus_exon_y;
        if (span.isForward()) {
            probe_y = consensus_exon_y;
        } else {
            probe_y = consensus_exon_y + consensus_exon_height - probe_height;
        }

        Color probeset_color = ps_color;

        if (outline_probes_in_probeset) {
            GlyphI probeset_glyph = new EfficientOutlineContGlyph();
            probeset_glyph.setCoords(span.getMin(), probe_y, span.getLength(), probe_height);
            probeset_glyph.setColor(probeset_color);

            parent_glyph.addChild(probeset_glyph);
            the_tier.setDataModelFromOriginalSym(probeset_glyph, probeset_sym);
            addProbesToProbeset(gviewer, the_tier, probeset_glyph, transformed_probeset_sym,
                    probe_y, probe_height, probeset_color);
        } else {
            addProbesToProbeset(gviewer, the_tier, parent_glyph, transformed_probeset_sym,
                    probe_y, probe_height, probeset_color);
        }
    }

    private void addProbesToProbeset(SeqMapViewExtendedI gviewer, TierGlyph the_tier,
            GlyphI probeset_glyph, SeqSymmetry transformed_probeset_sym,
            double probe_y, double probe_height, Color probeset_color) {
        int num_probes = transformed_probeset_sym.getChildCount();
        for (int i = 0; i < num_probes; i++) {
            SeqSymmetry probe_sym = transformed_probeset_sym.getChild(i);
            GlyphI probe_glyph = drawProbeGlyph(gviewer, the_tier,
                    probe_sym, probe_y, probe_height, probeset_color);
            if (probe_glyph == null) {
                continue;
            }
            probeset_glyph.addChild(probe_glyph);
            the_tier.setDataModelFromOriginalSym(probe_glyph, probe_sym);
        }
    }

    private GlyphI drawProbeGlyph(SeqMapViewExtendedI gviewer, TierGlyph the_tier,
            SeqSymmetry probe_sym, double probe_y, double probe_height, Color c) {
        BioSeq viewSeq = gviewer.getViewSeq();
        SeqSpan probe_span = probe_sym.getSpan(viewSeq);
        if (probe_span == null) {
            return null;
        }

        int num_parts = probe_sym.getChildCount();
        GlyphI probe_glyph = null;
        if (num_parts > 1) {
            // Each probe can possibly be split into multiple exon/intron pieces,
            // so use something like a LineContainerGlyph
            probe_glyph = new LineContainerGlyph();
            probe_glyph.setCoords(probe_span.getMin(), probe_y, probe_span.getLength(), probe_height);
            probe_glyph.setColor(c);

            for (int i = 0; i < num_parts; i++) {
                SeqSymmetry probe_part_sym = probe_sym.getChild(i);
                SeqSpan probe_part_span = probe_part_sym.getSpan(viewSeq);
                if (probe_part_span == null) {
                    continue;
                }

                GlyphI probe_part_glyph = drawProbeSegmentGlyph(probe_part_span, probe_y, probe_height, c);
                probe_glyph.addChild(probe_part_glyph);
                the_tier.setDataModelFromOriginalSym(probe_part_glyph, probe_part_sym);
            }
        } else {
            probe_glyph = drawProbeSegmentGlyph(probe_sym.getSpan(viewSeq), probe_y, probe_height, c);
        }
        return probe_glyph;
    }

    /**
     * Draws an individual segment of a probe. Most probes will have only one
     * segment, but probes that cover a region of a transcript that gets split
     * into "Exons" can have multiple "parts".
     */
    private static GlyphI drawProbeSegmentGlyph(SeqSpan probe_part_span, double probe_y, double probe_height, Color c) {
        EfficientOutlinedRectGlyph probe_part_glyph = new EfficientOutlinedRectGlyph();
        probe_part_glyph.setCoords(probe_part_span.getMin(), probe_y, probe_part_span.getLength(), probe_height);
        probe_part_glyph.setColor(c);
        return probe_part_glyph;
    }

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        String meth = BioSeqUtils.determineMethod(sym);
        String human_name = meth;
        if (meth != null) {
            // Why to strip off the ending ??
            // Not stripping off the ending to resolve bug ID: 3213610
            // http://sourceforge.net/tracker/?func=detail&aid=3213610&group_id=129420&atid=714744
//			strip off the " netaffx consensus" ending
//			int n = meth.lastIndexOf(NETAFFX_CONSENSUS);
//			if (n > 0) {
//				meth = meth.substring(0, n);
//			}
            int n = human_name.lastIndexOf(NETAFFX_CONSENSUS);
            if (n > 0) {
                human_name = human_name.substring(0, n);
            }

            try {
                if (style.getTrackName() == null && style.getTrackName().contains(NETAFFX_CONSENSUS)) {
                    style.setTrackName(human_name);
                }
                label_field = style.getLabelField();
                StyledGlyph.Direction useDirection = (!style.getSeparate()) ? StyledGlyph.Direction.BOTH : StyledGlyph.Direction.FORWARD;
                TierGlyph ftier = gviewer.getTrack(style, useDirection);
                ftier.setTierType(TierGlyph.TierType.ANNOTATION);
                ftier.setInfo(sym);
                TierGlyph rtier = (useDirection == StyledGlyph.Direction.BOTH) ? ftier : gviewer.getTrack(style, StyledGlyph.Direction.REVERSE);
                rtier.setTierType(TierGlyph.TierType.ANNOTATION);
                rtier.setInfo(sym);
                if (style.getSeparate()) {
                    addLeafsToTier(gviewer, sym, ftier, rtier, glyph_depth);
                    doMiddlegroundShading(rtier, gviewer, seq);
                } else {
                    // use only one tier
                    addLeafsToTier(gviewer, sym, ftier, rtier, glyph_depth);
                }
                doMiddlegroundShading(ftier, gviewer, seq);
            } catch (Exception ex) {
                Logger.getLogger(ProbeSetGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public boolean supportsTwoTrack() {
        return true;
    }

    @Override
    public String getName() {
        return "probeset";
    }

       @Override
    public Set<FileTypeCategory> getSupportedCategories() {
        return ImmutableSet.<FileTypeCategory>builder()
                .add(FileTypeCategory.ProbeSet).build();
    }
}
