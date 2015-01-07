/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SeqMapRefreshed;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.NumericFilter;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IGBTabPanelI;
import com.affymetrix.igb.swing.JRPCheckBox;
import com.affymetrix.igb.swing.JRPTextField;
import com.affymetrix.igb.tiers.TierLabelManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.text.AbstractDocument;

@Component(name = AltSpliceView.COMPONENT_NAME, provide = IGBTabPanelI.class)
public class AltSpliceView extends IGBTabPanel
        implements ActionListener, ComponentListener, ItemListener,
        SymSelectionListener, SeqSelectionListener, PreferenceChangeListener,
        TierLabelManager.PopupListener, SeqMapRefreshed {

    public static final String COMPONENT_NAME = "AltSpliceView";
    private static final long serialVersionUID = 1L;
    private static final int TAB_POSITION = 5;

    private JRPTextField buffer_sizeTF;
    private JLabel buffer_sizeL;
    private JRPCheckBox slice_by_selectionCB;
    private List<SeqSymmetry> last_selected_syms = new ArrayList<>();
    private BioSeq last_seq_changed = null;
    private boolean pending_sequence_change = false;
    private boolean pending_selection_change = false;
    private boolean slice_by_selection_on = true;

    private IGBService igbService;
    private AltSpliceSeqMapView altSpliceSeqMapView;
    private OrfAnalyzer orf_analyzer;

    @Activate
    public void activate() {
        super.activate(BUNDLE.getString("slicedViewTab"), BUNDLE.getString("slicedViewTab"), BUNDLE.getString("slicedViewTooltip"), false, TAB_POSITION);
        this.setLayout(new BorderLayout());
        altSpliceSeqMapView.subselectSequence = false;

        buffer_sizeTF = new JRPTextField("AltSpliceView_buffer_size", 4);
        ((AbstractDocument) buffer_sizeTF.getDocument()).setDocumentFilter(new NumericFilter.IntegerNumericFilter());
        buffer_sizeTF.setText("" + altSpliceSeqMapView.getSliceBuffer());
        slice_by_selectionCB = new JRPCheckBox("AltSpliceView_slice_by_selectionCB", "Slice By Selection", true);

        JPanel buf_adjustP = new JPanel(new FlowLayout());
        buffer_sizeL = new JLabel("Slice Buffer: ");
        buf_adjustP.add(buffer_sizeL);
        buf_adjustP.add(buffer_sizeTF);

        JPanel pan1 = new JPanel(new GridLayout(1, 2));

        pan1.add(slice_by_selectionCB);
        pan1.add(buf_adjustP);
        JPanel options_panel = new JPanel(new BorderLayout());

        options_panel.add("West", pan1);
        options_panel.add("East", orf_analyzer);
        JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitpane.setResizeWeight(1);  // allocate as much space as possible to top panel
        splitpane.setDividerSize(8);
        splitpane.setTopComponent(altSpliceSeqMapView);
        splitpane.setBottomComponent(options_panel);
        this.add("Center", splitpane);

        this.addComponentListener(this);
        buffer_sizeTF.addActionListener(this);
        slice_by_selectionCB.addItemListener(this);

        altSpliceSeqMapView.setAnnotatedSeq(GenometryModel.getInstance().getSelectedSeq());
        GenometryModel.getInstance().addSeqSelectionListener(this);
        GenometryModel.getInstance().addSymSelectionListener(this);
        PreferenceUtils.getTopNode().addPreferenceChangeListener(this);

        TierLabelManager tlman = altSpliceSeqMapView.getTierManager();
        if (tlman != null) {
            tlman.addPopupListener(this);
        }
        IGB.getSingleton().getMapView().addToRefreshList(this);
        resetAll();
    }

    /**
     * This method is notified when selected symmetries change. It usually
     * triggers a re-computation of the sliced symmetries to draw. If no
     * selected syms, then don't change. Any Graphs in the selected symmetries
     * will be ignored (because graphs currently span entire sequence and
     * slicing on them can use too much memory).
     */
    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        if (IGBService.DEBUG_EVENTS) {
            System.out.println("AltSpliceView received selection changed event");
        }
        Object src = evt.getSource();
        // ignore if symmetry selection originated from this AltSpliceView -- don't want to
        //   reslice based on internal selection!
        if ((src != this) && (src != altSpliceSeqMapView)) {
            // catching altSpliceSeqMapView as source of event because currently sym selection events actually originating
            //    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
            last_selected_syms = evt.getSelectedGraphSyms();
            last_selected_syms = removeGraphs(last_selected_syms);
            if (last_selected_syms.size() > 0) {
                if (!this.isShowing()) {
                    pending_selection_change = true;
                } else if (slice_by_selection_on) {
                    this.sliceAndDice(last_selected_syms);
                    pending_selection_change = false;
                } else {
                    altSpliceSeqMapView.select(last_selected_syms, false);
                    pending_selection_change = false;
                }
            }
        }
        resetAll();
        refreshView();
    }

    /**
     * Takes a list of SeqSymmetries and removes any GraphSyms from it.
     */
    private static List<SeqSymmetry> removeGraphs(List<SeqSymmetry> syms) {
        List<SeqSymmetry> v = new ArrayList<>(syms.size());
        for (SeqSymmetry sym : syms) {
            if (!(sym instanceof GraphSym)) {
                v.add(sym);
            }
        }
        return v;
    }

    @Override
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        if (IGBService.DEBUG_EVENTS) {
            System.out.println("AltSpliceView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
        }
        BioSeq newseq = GenometryModel.getInstance().getSelectedSeq();
        if (last_seq_changed != newseq) {
            last_seq_changed = newseq;
            if (this.isShowing() && slice_by_selection_on) {
                altSpliceSeqMapView.setAnnotatedSeq(last_seq_changed);
                pending_sequence_change = false;
            } else {
                pending_sequence_change = true;
            }
        }
        resetAll();
    }

    private void setSliceBySelection(boolean b) {
        slice_by_selection_on = b;
    }

    public void setSliceBuffer(int buf_size) {
        buffer_sizeTF.setText(String.valueOf(buf_size));
        altSpliceSeqMapView.setSliceBuffer(buf_size,
                new Runnable() {

                    public void run() {
                        orf_analyzer.redoOrfs();
                    }
                }
        );
    }

    private void sliceAndDice(List<SeqSymmetry> syms) {
        if (syms.size() > 0) {
            altSpliceSeqMapView.sliceAndDice(syms,
                    new Runnable() {

                        public void run() {
                            orf_analyzer.redoOrfs();
                        }
                    });
        }
    }

    // ComponentListener implementation
    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
        if (pending_sequence_change && slice_by_selection_on) {
            altSpliceSeqMapView.setAnnotatedSeq(last_seq_changed);
            pending_sequence_change = false;
        }
        if (pending_selection_change) {
            if (slice_by_selection_on) {
                this.sliceAndDice(last_selected_syms);
            } else {
                altSpliceSeqMapView.select(last_selected_syms, false);
            }
            pending_selection_change = false;
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if (src == buffer_sizeTF) {
            String str = buffer_sizeTF.getText();
            if (str != null) {
                try {
                    int new_buf_size = Integer.parseInt(str);
                    this.setSliceBuffer(new_buf_size);
                } catch (NumberFormatException e) {
                    //do nothing
                }
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent evt) {
        Object src = evt.getSource();
        if (src == slice_by_selectionCB) {
            setSliceBySelection(evt.getStateChange() == ItemEvent.SELECTED);
            if (slice_by_selection_on) {
                this.sliceAndDice(last_selected_syms);
            }
        }
    }

    @Override
    public void popupNotify(JPopupMenu popup, final TierLabelManager handler) {
        if (handler != altSpliceSeqMapView.getTierManager()) {
            return;
        }

        Action hide_action = new GenericAction("Hide Tier", null, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                altSpliceSeqMapView.doEdgeMatching(Collections.<GlyphI>emptyList(), false);
                handler.hideTiers(handler.getSelectedTierLabels(), false, true);
            }
        };

        Action restore_all_action = new GenericAction("Show All", null, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                // undo all edge-matching, because packing will behave badly otherwise.
                altSpliceSeqMapView.doEdgeMatching(Collections.<GlyphI>emptyList(), false);
                handler.showTiers(handler.getAllTierLabels(), true, true);
            }
        };

        hide_action.setEnabled(!handler.getSelectedTierLabels().isEmpty());
        restore_all_action.setEnabled(true);

        if (popup.getComponentCount() > 0) {
            popup.add(new JSeparator());
        }
        popup.add(hide_action);
        popup.add(restore_all_action);
    }

    public SeqMapView getSplicedView() {
        return altSpliceSeqMapView;
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        if (!evt.getNode().equals(PreferenceUtils.getTopNode())
                || !this.isShowing()) {
            return;
        }

        if (evt.getKey().equals(OrfAnalyzer.PREF_STOP_CODON_COLOR)
                || evt.getKey().equals(OrfAnalyzer.PREF_DYNAMIC_ORF_COLOR)
                || evt.getKey().equals(OrfAnalyzer.PREF_BACKGROUND_COLOR)) {
            // Each time changed the color, it would triger this method twice and caused a concurrent modification exception 
            ThreadUtils.runOnEventQueue(new Runnable() {

                @Override
                public void run() {
                    orf_analyzer.redoOrfs();
                }
            });
        }
    }

    public JRPTextField getBufferSizeTF() {
        return this.buffer_sizeTF;
    }

    public void refreshView() {
        orf_analyzer.redoOrfs();
    }

    protected final void resetAll() {
        boolean enable = igbService != null && igbService.getVisibleTierGlyphs() != null && igbService.getVisibleTierGlyphs().size() > 1;
        buffer_sizeL.setEnabled(enable);
        buffer_sizeTF.setEnabled(enable);
        orf_analyzer.setEnabled(enable);
        slice_by_selectionCB.setEnabled(enable);
        altSpliceSeqMapView.enableSeqMap(enable);
        orf_analyzer.enableView(enable);
    }

    @Override
    public void mapRefresh() {
        resetAll();
        refreshView();
    }

    @Reference
    public void setIgbService(IGBService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void setAltSpliceSeqMapView(AltSpliceSeqMapView altSpliceSeqMapView) {
        this.altSpliceSeqMapView = altSpliceSeqMapView;
    }

    @Reference
    public void setOrf_analyzer(OrfAnalyzer orf_analyzer) {
        this.orf_analyzer = orf_analyzer;
    }

}
