package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.SeqMapRefreshed;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimplePairSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.swing.JRPPopupMenu;
import com.affymetrix.igb.swing.RPAdjustableJSlider;
import com.affymetrix.igb.tiers.TrackStyle;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.Adjustable;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;

final class AltSpliceSeqMapView extends SeqMapView implements SeqMapRefreshed {

    private static final long serialVersionUID = 1L;
    private boolean slicing_in_effect = false;
    /**
     * number of bases that slicer tries to buffer on each side of every span it
     * is using to guide slicing
     */
    private int slice_buffer = 100;
    /**
     * current symmetry used to determine slicing
     */
    private SeqSymmetry slice_symmetry;
    private CThreadWorker<Void, Void> slice_thread = null;
    private Map<ITrackStyleExtended, ITrackStyleExtended> style2Style;

    AltSpliceSeqMapView(boolean b) {
        super(b, "AltSpliceSeqMapView", IGB.getInstance().getFrame());
        if (tierLabelManager != null) {
            tierLabelManager.setDoGraphSelections(false);
        }
        report_hairline_position_in_status_bar = false;
        report_status_in_status_bar = false;
        setEdgeMatching(false);
        IGB.getInstance().getMapView().addToRefreshList(this);
    }

    @Override
    public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view) {
        if (coord_shift) {
            // ignore the preserve_view parameter, always pretend it is false in the splice view
            super.setAnnotatedSeq(seq, preserve_selection, false);
        } else {
            this.clear();
            this.aseq = seq;
            this.viewseq = seq;
            slice_symmetry = null;
        }
    }

    @Override
    public TierGlyph getTrack(final ITrackStyleExtended style, final StyledGlyph.Direction tierDirection) {
        ITrackStyleExtended styleCopy = getStyle(style);
        // super.getTrack() may have created a brand new tier, in which case
        // the style is already set to "style_copy", or it may have re-used
        // a tier, in which case it may still have an old copy of the style
        // associated with it.  Reset the style to be certain.
        TierGlyph tierGlyph = super.getTrack(styleCopy, tierDirection);
        tierGlyph.setStyle(styleCopy);

        return tierGlyph;
    }

    private ITrackStyleExtended getStyle(final ITrackStyleExtended style) {
        // Lazy initialization
        if (style2Style == null) {
            style2Style = new HashMap<>();
        }

        ITrackStyleExtended style_copy = style2Style.get(style);
        if (style_copy == null) {
            style_copy = new TrackStyle() {

                @Override
                public String getMethodName() {
                    return style.getMethodName();
                }

                @Override
                public boolean drawCollapseControl() {
                    return false;
                }

            };
            style_copy.copyPropertiesFrom(style);
            style_copy.setMaxDepth(0);
            style2Style.put(style, style_copy);
        } else {
            // Properties that needs to be retained.
            boolean show = style_copy.getShow();

            // Apply all properties.
            style_copy.copyPropertiesFrom(style);

            // Apply retained properties.
            style_copy.setShow(show);
        }
        style_copy.setGraphTier(style.isGraphTier());

        return style_copy;
    }

    //@Override
    //protected void preparePopup(JPopupMenu popup, NeoMouseEvent nevt) {
    //popup.add(CenterAtHairlineAction.getAction());
    //}
    protected final void setSliceBuffer(int bases, final Runnable runnable) {
        slice_buffer = bases;
        if (slicing_in_effect) {
            stopSlicingThread();

            slice_thread = new CThreadWorker<Void, Void>("set slice buffer") {

                @Override
                protected Void runInBackground() {
                    enableSeqMap(false);
                    sliceAndDiceNow(slice_symmetry);
                    return null;
                }

                @Override
                protected void finished() {
                    setAnnotatedSeq(aseq);
                    enableSeqMap(true);
                    runnable.run();
                }
            };
            CThreadHolder.getInstance().execute(this, slice_thread);
        }
    }

    final int getSliceBuffer() {
        return slice_buffer;
    }

    public final void sliceAndDice(final List<SeqSymmetry> syms, final Runnable runnable) {
        stopSlicingThread();

        slice_thread = new CThreadWorker<Void, Void>("slice and dice") {

            @Override
            public Void runInBackground() {
                enableSeqMap(false);
                SimpleSymWithProps unionSym = new SimpleSymWithProps();
                SeqUtils.union(syms, unionSym, aseq);
                sliceAndDiceNow(unionSym);
                return null;
            }

            @Override
            protected void finished() {
                setAnnotatedSeq(aseq);
                enableSeqMap(true);
                runnable.run();
            }
        };

        CThreadHolder.getInstance().execute(this, slice_thread);
    }

    // disables the sliced view while the slicing thread works
    protected void enableSeqMap(boolean b) {
        seqmap.setVisible(b);
        if (map_range_box != null) {
            if (!b) {
//				map_range_box.range_box.setText("Working...");
                map_range_box.range_box.setText("");
            } else {
                SeqSpan span = this.getVisibleSpan();
                if (span != null) {
                    map_range_box.setRangeText(span.getMin(), span.getMax());
                }

            }
        }
        Component[] comps = xzoombox.getComponents();
        for (Component comp : comps) {
            comp.setEnabled(b);
        }
        comps = yzoombox.getComponents();
        for (Component comp : comps) {
            comp.setEnabled(b);
        }
    }

    private void stopSlicingThread() {
        if (slice_thread != null
                && slice_thread.getState() == SwingWorker.StateValue.STARTED) {
            slice_thread.cancel(true); // TODO: Deprecated, but seems OK here.  Maybe fix later.
            slice_thread = null;
            enableSeqMap(true);
        }
    }

    @Override
    public void dataRemoved() {
        if (slicing_in_effect && slice_symmetry != null) {
            sliceAndDiceNow(slice_symmetry);
            setAnnotatedSeq(aseq);
        }
    }

    public void mapRefresh() {
        if (slicing_in_effect && slice_symmetry != null) {
            sliceAndDiceNow(slice_symmetry);
            setAnnotatedSeq(aseq);
        }
    }

    /**
     * Performs a genometry-based slice-and-dice.
     * Assumes that symmetry children are ordered by
     * child.getSpan(aseq).getMin().
     */
    private void sliceAndDiceNow(SeqSymmetry sym) {
        int childCount = (sym == null) ? 0 : sym.getChildCount();
        if (childCount == 0) {
            return;
        }

        this.viewseq = new BioSeq("view_seq", aseq.getLength());
        slice_symmetry = sym;
        coord_shift = true;

        if (seq2viewSym == null) {
            seq2viewSym = new SimpleMutableSeqSymmetry();
        } else {
            seq2viewSym.clear();
        }

        // rebuild seq2viewSym as a symmetry mapping slices of aseq to abut next to each other
        //    mapped to viewseq
        int prev_max = 0;
        int slice_offset = 0;
        MutableSeqSpan prev_seq_slice = null;
        MutableSeqSpan prev_view_slice = null;
        for (int i = 0; i < childCount; i++) {
            SeqSymmetry child = sym.getChild(i);
            SeqSpan exact_span = child.getSpan(aseq);
            if (exact_span == null) {
                continue;
            }  // skip any children that don't have a span in aseq
            int next_min;
            if (i == (childCount - 1)) {
                next_min = aseq.getLength();
            } else {
                next_min = sym.getChild(i + 1).getSpan(aseq).getMin();
            }

            int slice_min = Math.max(prev_max, (exact_span.getMin() - slice_buffer));
            int slice_max = Math.min(next_min, (exact_span.getMax() + slice_buffer));
            MutableSeqSpan seq_slice_span = new SimpleMutableSeqSpan(slice_min, slice_max, aseq);

            int slice_length = seq_slice_span.getLength();
            MutableSeqSpan view_slice_span
                    = new SimpleMutableSeqSpan(slice_offset, slice_offset + slice_length, viewseq);

            if (prev_seq_slice != null && SeqUtils.looseOverlap(prev_seq_slice, seq_slice_span)) {
                // if new seq slice span abuts the old one, then just
                // lengthen existing spans (seq and view) rather than adding new ones
                SeqUtils.encompass(prev_seq_slice, seq_slice_span, prev_seq_slice);
                SeqUtils.encompass(prev_view_slice, view_slice_span, prev_view_slice);
            } else {
                addIntronTransforms(prev_seq_slice, seq_slice_span, view_slice_span);

                SeqSymmetry slice_sym = new SimplePairSeqSymmetry(seq_slice_span, view_slice_span);
                seq2viewSym.addChild(slice_sym);

                prev_seq_slice = seq_slice_span;
                prev_view_slice = view_slice_span;
            }
            slice_offset += slice_length;
            prev_max = slice_max;
        }

        SeqSpan seq_span = SeqUtils.getChildBounds(seq2viewSym, aseq);
        SeqSpan view_span = SeqUtils.getChildBounds(seq2viewSym, viewseq);
        seq2viewSym.addSpan(seq_span);
        seq2viewSym.addSpan(view_span);

        viewseq.setComposition(seq2viewSym);
        viewseq.setBounds(view_span.getMin(), view_span.getMax());
        transform_path = new SeqSymmetry[1];
        transform_path[0] = seq2viewSym;
        slicing_in_effect = true;

    }

    /**
     * Add spans to the transformation sym that will cause all
     * "intron" spans in the regions of aseq between exons chosen for slicing
     * to be transformed into zero-length spans.
     * This allows glyph factories to find "deleted" exons
     * and draw them (if desired) without requiring messy calculations in
     * the glyph factories.
     *
     * @param prev_seq_slice
     * @param seq_slice_span
     * @param view_slice_span
     */
    private void addIntronTransforms(MutableSeqSpan prev_seq_slice, MutableSeqSpan seq_slice_span, MutableSeqSpan view_slice_span) {
        if (prev_seq_slice != null) {
            SeqSpan intron_region_span = new SimpleSeqSpan(prev_seq_slice.getMax(), seq_slice_span.getMin(), aseq);
            SeqSpan zero_length_span = new SimpleSeqSpan(view_slice_span.getMin(), view_slice_span.getMin(), viewseq);
            // SimplePairSeqSymmetry is better than EfficientPairSeqSymmetry here,
            // since there will be frequent calls to getSpan(BioSeq)
            seq2viewSym.addChild(new SimplePairSeqSymmetry(intron_region_span, zero_length_span));
        }
    }

    @Override
    public boolean isGenomeSequenceSupported() {
        return false;
    }

    @Override
    public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y) {
        stopSlicingThread();
        super.setAnnotatedSeq(seq, preserve_selection, preserve_view_x, preserve_view_y);
    }

    @Override
    public boolean shouldAddCytobandGlyph() {
        return false;
    }

    @Override
    protected void clear() {
        stopSlicingThread();
        super.clear();
    }

    @Override
    protected Adjustable getXZoomer(String id) {
        return new RPAdjustableJSlider(id + "_xzoomer", Adjustable.HORIZONTAL);
    }

    @Override
    protected void addRefreshButton(String id) {
        //Do nothing.
    }

    @Override
    protected void addLoadResidueButton(String id) {
        //Do nothing.
    }

    @Override
    protected void addSearchButton(String id) {
        //Do nothing.
    }

    @Override
    protected void addDependentAndEmptyTrack() {
        //Do Nothing for alt splice view.
    }

    @Override
    protected void addZoomInXButton(String id) {
        // Do nothing
    }

    @Override
    protected void addZoomOutXButton(String id) {
        // Do nothing
    }

    @Override
    protected void addZoomInYButton(String id) {
        // Do nothing
    }

    @Override
    protected void addZoomOutYButton(String id) {
        // Do nothing
    }

    @Override
    protected void preparePopup(JRPPopupMenu popup, NeoMouseEvent nevt) {
        // No right click menu in slice view
    }

}
