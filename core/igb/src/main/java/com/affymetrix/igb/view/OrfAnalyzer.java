/**
 * Copyright (c) 2001-2005 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import cern.colt.list.IntArrayList;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.style.SimpleTrackStyle;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.FlyPointLinkerGlyph;
import com.affymetrix.igb.swing.JRPCheckBox;
import com.affymetrix.igb.swing.JRPSlider;
import com.affymetrix.igb.swing.JRPTextField;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.factories.TransformTierGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * OrfAnalyzer2 is used on the virtual sequence being viewed in AltSpliceView.
 * It does not incorporate content stat graphs (GC, dicodon, etc.).
 */
public final class OrfAnalyzer extends JComponent
        implements ChangeListener, ActionListener {

    private static final long serialVersionUID = 1L;

    public static final String PREF_STOP_CODON_COLOR = "stop codon";
    public static final String PREF_DYNAMIC_ORF_COLOR = "dynamic orf";
    public static final String PREF_BACKGROUND_COLOR = "background";
    public static final Color default_stop_codon_color = new Color(255, 0, 0);
    public static final Color default_dynamic_orf_color = new Color(0, 255, 0);
    public static final Color default_background_color = new Color(169, 169, 169);
    // GAH 8-23-2004
    // As IGB is currently configured, smv should be set to the internal SeqMapView of the AltSpliceView...
    private final SeqMapView smv;
    private JRPSlider orf_thresh_slider;
    private JRPCheckBox showCB;
    private JLabel orf_threshL;
    private JLabel orfL;
    private TransformTierGlyph fortier;
    private TransformTierGlyph revtier;
    private int current_orf_thresh = 300;
    private static final int orf_thresh_min = 10;
    private static final int orf_thresh_max = 500;
    private static final int max_analysis_span = 1000000;
    private boolean show_orfs;
    private final List<FlyPointLinkerGlyph> orf_holders = new ArrayList<>();
    private static final String[] stop_codons = {"TAA", "TAG", "TGA", "TTA", "CTA", "TCA"};
    private int previousBefferSize;

    public OrfAnalyzer(SeqMapView view) {
        super();
        smv = view;
        init();
    }

    public void init() {

        orfL = new JLabel("Min ORF Length:", SwingConstants.CENTER);
        orf_threshL = new JLabel(Integer.toString(current_orf_thresh), SwingConstants.CENTER);
        showCB = new JRPCheckBox("OrfAnalyzer_showCB", "Analyze ORFs");
        orf_thresh_slider = new JRPSlider("OrfAnalyzer_orf_thresh_slider", JSlider.HORIZONTAL, orf_thresh_min, orf_thresh_max, current_orf_thresh);
        JPanel pan1 = new JPanel();
        pan1.setLayout(new FlowLayout());
        pan1.add(orfL);
        pan1.add(orf_threshL);

        this.setLayout(new FlowLayout());
        this.add(showCB);
        this.add(pan1);
        this.add(orf_thresh_slider);

        showCB.addActionListener(this);
        orf_thresh_slider.addChangeListener(this);
    }

    public void stateChanged(ChangeEvent evt) {
        Object src = evt.getSource();
        if (src == orf_thresh_slider) {
            current_orf_thresh = orf_thresh_slider.getValue();
            for (FlyPointLinkerGlyph fw : orf_holders) {
                fw.setMinThreshold(current_orf_thresh);
            }
            AffyTieredMap map = smv.getSeqMap();
            map.updateWidget();
            orf_threshL.setText(Integer.toString(current_orf_thresh));
        }
    }

    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if (src == showCB) {
            JRPTextField tf = AltSpliceView.getSingleton().getBufferSizeTF();
            show_orfs = ((JCheckBox) src).isSelected();
            if (show_orfs) {
                if (tf.getText() == null || tf.getText().length() == 0) {
                    return;
                }
                previousBefferSize = Integer.parseInt(tf.getText());
                AltSpliceView.getSingleton().setSliceBuffer(0);
            } else {
                AltSpliceView.getSingleton().setSliceBuffer(previousBefferSize);
                removeTiersAndCleanup();
                adjustMap();
            }

            tf.setEnabled(!show_orfs);
        }
    }

    void redoOrfs() {
        if (smv == null) {
            return;
        }
        BioSeq vseq = smv.getViewSeq();
        BioSeq seq = smv.getAnnotatedSeq();
        if (vseq == null || vseq.getComposition() == null || seq == null) {
            return;
        }
        removeTiersAndCleanup();
        if (!show_orfs) {
            return;
        }

        SeqSpan vspan = smv.getVisibleSpan();
        int span_start = vspan.getMin();
        int span_end = vspan.getMax();
        if (span_start < 0 || span_end < span_start) {
            ErrorHandler.errorPanel("Cannot perform ORF analysis: first select a sliced region");
            show_orfs = false;
            showCB.setSelected(false);
            return;
        }

        SeqSpan aspan = vseq.getComposition().getSpan(seq);
        if (!(seq.isAvailable(aspan))) {
            //Load Residues
            if (!GeneralLoadView.getLoadView().loadResidues(aspan, true)) {
                show_orfs = false;
                showCB.setSelected(false);
                return;
            }
            //ErrorHandler.errorPanel("Cannot perform ORF analysis: must first load residues for sequence");
        }

        Color bgcol = PreferenceUtils.getColor(PREF_BACKGROUND_COLOR, default_background_color);
        SimpleTrackStyle forSts = new SimpleTrackStyle("Stop Codon", false) {
            @Override
            public boolean drawCollapseControl() {
                return false;
            }
        };
        forSts.setBackground(bgcol);
        forSts.setLabelBackground(bgcol);
        //forSts.setLabelForeground(bgcol);
        forSts.setTrackName("Stop Codons");
        fortier = new TransformTierGlyph(forSts);
        fortier.setTierType(TierGlyph.TierType.ANNOTATION);
        fortier.setFixedPixHeight(25);
        fortier.setFillColor(bgcol);
        fortier.setBackgroundColor(bgcol);
        fortier.setDirection(StyledGlyph.Direction.FORWARD);

        AffyTieredMap map = smv.getSeqMap();
        map.addTier(fortier, true);  // put forward tier above axis

        SimpleTrackStyle revSts = new SimpleTrackStyle("Stop Codon", false) {
            @Override
            public boolean drawCollapseControl() {
                return false;
            }
        };
        revSts.setBackground(bgcol);
        revSts.setLabelBackground(bgcol);
        //revSts.setLabelForeground(bgcol);
        revSts.setTrackName("Stop Codons");
        revtier = new TransformTierGlyph(revSts);
        revtier.setTierType(TierGlyph.TierType.ANNOTATION);
        revtier.setFixedPixHeight(25);
        revtier.setFillColor(bgcol);
        revtier.setBackgroundColor(bgcol);
        revtier.setDirection(StyledGlyph.Direction.REVERSE);
        map.addTier(revtier, false);  // put reverse tier below axis

        Color pointcol = PreferenceUtils.getColor(PREF_STOP_CODON_COLOR, default_stop_codon_color);
        Color linkcol = PreferenceUtils.getColor(PREF_DYNAMIC_ORF_COLOR, default_dynamic_orf_color);

        int span_mid = (int) (0.5f * span_start + 0.5f * span_end);

        span_start = span_mid - (max_analysis_span / 2);
        span_start = Math.max(0, span_start);	// shouldn't have negative start
        span_start -= span_start % 3;
        span_end = span_mid + (max_analysis_span / 2);
        span_end -= span_end % 3;

        int residue_offset = vseq.getMin();
        IntArrayList[] frame_lists = buildFrameLists(span_start, residue_offset, vseq, span_end);

        for (int frame = 0; frame < 6; frame++) {
            boolean forward = frame <= 2;
            IntArrayList xpos_vec = frame_lists[frame];
            int[] xpos = xpos_vec.elements();
            // must sort positions!  because positions were added to IntList for each type of
            //   stop codon before other types, positions in IntList will _not_ be in
            //   ascending order (though for a particular type, e.g. "TAA", they will be)
            Arrays.sort(xpos);

            GlyphI point_template = new FillRectGlyph();
            point_template.setColor(pointcol);
            point_template.setCoords(residue_offset, 0, vseq.getLength(), 10);

            TierGlyph tier = forward ? fortier : revtier;
            GlyphI orf_glyph = null;
            if (xpos.length > 0) {
                GlyphI link_template = new FillRectGlyph();
                link_template.setColor(linkcol);
                link_template.setCoords(0, 0, 0, 4);  // only height is retained

                FlyPointLinkerGlyph fw = new FlyPointLinkerGlyph(point_template, link_template, xpos, 3,
                        span_start, span_end);
                fw.setHitable(false);
                orf_glyph = fw;
                fw.setMinThreshold(current_orf_thresh);
                orf_holders.add(fw);
            } else {
                orf_glyph = new FillRectGlyph();
                orf_glyph.setHitable(false);
                orf_glyph.setColor(tier.getBackgroundColor());
            }
            // Make orf_glyph as long as vseq; otherwise, two or more could pack into one line.
            // The underlying symmetry may be shorter, so "zoom to selected" won't work.
            // But the glyph not hittable (for other reasons), so this isn't an issue.
            orf_glyph.setCoords(residue_offset, 0, vseq.getLength(), point_template.getCoordBox().height);
            tier.addChild(orf_glyph);
        }
        adjustMap();
    }

    private static IntArrayList[] buildFrameLists(int span_start, int residue_offset, BioSeq vseq, int span_end) {
        int span_length = span_end - span_start;
        IntArrayList[] frame_lists = new IntArrayList[6];
        for (int i = 0; i < 6; i++) {
            // estimating number of stop codons, then padding by 20%
            frame_lists[i] = new IntArrayList((int) ((span_length / 64) / 1.2));
        }
        for (int i = 0; i < stop_codons.length; i++) {
            boolean forward_codon = i <= 2;
            String codon = stop_codons[i];
            int seq_index = span_start;
            int res_index = span_start - residue_offset;
            res_index = caseInsensitiveIndexOfHack(vseq, codon, res_index);
            // need to factor in possible offset of residues string from start of
            //    sequence (for example, when sequence is a CompNegSeq)
            while (res_index >= 0 && (seq_index < span_end)) {
                int frame;
                // need to factor in possible offset of residues string from start of
                //    sequence (for example, when sequence is a CompNegSeq)
                seq_index = res_index + residue_offset;
                if (forward_codon) {
                    frame = res_index % 3;	 // forward frames = (0, 1, 2)
                } else {
                    frame = 3 + (res_index % 3); // reverse frames = (3, 4, 5)
                }

                frame_lists[frame].add(seq_index);
                res_index = caseInsensitiveIndexOfHack(vseq, codon, res_index + 1);
            }
        }
        return frame_lists;
    }

    private static int caseInsensitiveIndexOfHack(BioSeq vseq, String codon, int resIndex) {
        int temp_index = vseq.indexOf(codon, resIndex);
        if (temp_index == -1) {
            temp_index = vseq.indexOf(codon.toLowerCase(), resIndex);	// hack for case-insensitivity
        }
        return temp_index;
    }

    private void removeTiersAndCleanup() {
        AffyTieredMap map = smv.getSeqMap();
        if (fortier != null) {
            map.removeTier(fortier);
            fortier = null;
        }
        if (revtier != null) {
            map.removeTier(revtier);
            revtier = null;
        }
        orf_holders.clear();
    }

    private void adjustMap() {
        ThreadUtils.runOnEventQueue(() -> {

            AbstractAction action = new AbstractAction() {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    AffyTieredMap tiermap = smv.getSeqMap();
                    tiermap.repack();
                    tiermap.stretchToFit(false, true);
                    tiermap.updateWidget();
                }
            };
            smv.preserveSelectionAndPerformAction(action);
        });

    }

    public void enableView(boolean enable) {
        orf_thresh_slider.setEnabled(enable);
        showCB.setEnabled(enable);
        orf_threshL.setEnabled(enable);
        orfL.setEnabled(enable);
    }
}
