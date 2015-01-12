/*  Copyright (c) 2012 Genentech, Inc.
 *
 *  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.GraphGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Make all the selected tracks easy to compare by giving them the same size and
 * scale. When no tracks are selected, do them all. Scale selected graphs to the
 * smallest scale (largest MaxY). Listen to ListSelectionModel to setEnabled
 * depending on selection. This last could get a bit complicated with numbers
 * and types of tracks to consider.
 * <p>
 * TODOSet the same stack depth for selected annotation tracks?
 *
 * @author Eric Blossom
 */
public class CanonicalizeTracksAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static CanonicalizeTracksAction ACTION;

    /**
     * Get an (the) instance of a canonicalizer.
     *
     * @return the same static instance every time.
     */
    public static CanonicalizeTracksAction getAction() {
        if (ACTION == null) {
            ACTION = new CanonicalizeTracksAction();
            AffyTieredMap m = ACTION.getSeqMapView().getSeqMap();
            if (m instanceof AffyLabelledTierMap) {
                AffyLabelledTierMap ltm = (AffyLabelledTierMap) m;
                ltm.addListSelectionListener(new Enabler(ACTION));
            }
        }
        return ACTION;
    }

    private CanonicalizeTracksAction() {
        super(BUNDLE.getString("canonicalizeTracks"),
                "16x16/actions/equalizer.png",
                "22x22/actions/equalizer.png");
        putValue(Action.SHORT_DESCRIPTION,
                BUNDLE.getString("canonicalizeTracksTooltip"));
    }

    private List<TierGlyph> graphTracks = new ArrayList<>();

    /**
     * Should be enabled only if we can do something. Check that more than one
     * graph track is selected or that no tracks are selected.
     */
    public boolean shouldBeEnabled() {
        List<? extends GlyphI> l = getTierManager().getSelectedTiers();
        if (0 == l.size()) {
            l = this.getSeqMapView().getTierManager().getAllTierGlyphs(false);
        }
        if (1 == l.size()) {
            return false;
        }
        this.graphTracks.clear();
        for (GlyphI g : l) {
            TierGlyph tg = (TierGlyph) g;
            Object info = tg.getInfo();
            if (info instanceof GraphSym) { // then we have a graph.
                this.graphTracks.add(tg);
            }

        }
        return (1 < this.graphTracks.size());
    }

    /**
     * Set the heights and scales of selected (graph) tracks to match each
     * other. Set the heights to that of the tallest tier. Set the max y of each
     * graph tier to the largest max y of all the graph tiers. This should make
     * them all the same scale. TODO Set same stack depth for all selected
     * annotation tiers.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (1 == this.graphTracks.size()) {
            JOptionPane.showMessageDialog(null,
                    "Select more (or fewer) than one track.\n"
                    + "Actually you shouldn't be able to get here.\n"
                    + "File a bug report.",
                    this.getClass().getSimpleName(),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        double maxHeight = 0;
        float maxMax = Float.MIN_VALUE;
        for (TierGlyph tg : this.graphTracks) {
            double h = tg.getCoordBox().height;
            if (tg.isManuallyResizable() && maxHeight < h) {
                maxHeight = h;
            }
            GraphSym graph = (GraphSym) tg.getInfo();
            float[] y = graph.getGraphYCoords();
            float m = Float.MIN_VALUE;
            for (int i = 0; i < y.length; i++) {
                if (m < y[i]) {
                    m = y[i];
                }
            }
            maxMax = Math.max(maxMax, m);
        }
        for (TierGlyph tg : this.graphTracks) {
            if (tg.isManuallyResizable()) {
                tg.setPreferredHeight(
                        maxHeight,
                        this.getSeqMapView().getSeqMap().getView()
                );
                if (Float.MIN_VALUE < maxMax) {
                    if (tg.getAnnotStyle().isGraphTier()) {
                        for (GlyphI g : tg.getChildren()) {
                            if (!(g instanceof GraphGlyph)) {
                                break;
                            }

                            GraphGlyph gg = (GraphGlyph) g;
                            gg.setVisibleMaxY(maxMax);
                        }
                    }
                }
            }
        }
        this.refreshMap(true, false);
    }

    private static class Enabler implements ListSelectionListener {

        private CanonicalizeTracksAction client;

        Enabler(CanonicalizeTracksAction a) {
            this.client = a;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            this.client.setEnabled(this.client.shouldBeEnabled());
        }

    }

}
