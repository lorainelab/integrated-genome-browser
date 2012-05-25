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

import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Make all the selected tracks easy to compare
 * by giving them the same size and scale.
 * When no tracks are selected, do them all.
 * Scale selected graphs to the smallest scale (largest MaxY).
 * <p> TODO
 * </p>
 * <ol>
 * <li> What's the correct way to resize? setPreferredHeight?
 * <li> Set the same stack depth for selected annotation tracks?
 * <li> Listen to ListSelectionModel to setEnabled depending on selection.
 * This last could get a bit complicated with numbers and types of tracks to consider.
 * </ol>
 * @author blossome
 */
public class CanonicalizeTracksAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static CanonicalizeTracksAction ACTION;

	/**
	 * Get an (the) instance of a canonicalizer.
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
		super("Canonicalize Tracks...",
				"16x16/actions/equalizer.png",
				"22x22/actions/equalizer.png");
		putValue(Action.SHORT_DESCRIPTION,
			"Make the scales of the selected tracks match so they can be readily compared.");
	}

	/**
	 * Enabled only if we can do something.
	 * Over simplified to try out strategy.
	 * Just check that a track (any track) is selected.
	 */
	public boolean shouldBeEnabled() {
		List<? extends GlyphI> l = this.getSeqMapView().getSelectedTiers();
		return (1 != l.size());
	}

	/**
	 * Set the heights of selected graph tiers to the max of selected graph tiers.
	 * Make sure selected graph tiers are all to the same scale.
	 * Set same stack depth for all selected annotation tiers.
	 * Pops up a dialog.
	 */
    @Override
    public void actionPerformed(ActionEvent e) {
		List<? extends GlyphI> l = this.getSeqMapView().getSelectedTiers();
		if (l.isEmpty()) { // then no tracks are selected.
			// Canonicalize all the tracks.
			l = this.getSeqMapView().getTierManager().getAllTierGlyphs();
		}
		if (1 == l.size()) {
			JOptionPane.showMessageDialog(null,
					"Select more (or fewer) than one track.\n" +
					"Actually you shouldn't be able to get here.\n" +
					"File a bug report.",
					this.getClass().getSimpleName(),
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		double maxHeight = 0;
		float maxMax = Float.MIN_VALUE;
		for (GlyphI g: l) {
			double h = g.getCoordBox().height;
			TierGlyph tg = (TierGlyph) g;
			if (tg.isManuallyResizable() && maxHeight < h) {
				maxHeight = h;
			}
			Object info = tg.getInfo();
			if (info instanceof GraphSym) {
				GraphSym graph = (GraphSym) info;
				float[] y = graph.getGraphYCoords();
				float m = Float.MIN_VALUE;
				for (int i = 0; i < y.length; i++) {
					if (m < y[i]) {
						m = y[i];
					}
				}
				maxMax = Math.max(maxMax, m);
			}
		}
		JOptionPane.showMessageDialog(null,
				"Thank you for your interest. Setting heights to " + maxMax,
				this.getClass().getSimpleName(),
				JOptionPane.INFORMATION_MESSAGE);
		for (GlyphI g: l) {
			Rectangle2D.Double b = g.getCoordBox();
			TierGlyph tg = (TierGlyph) g;
			if (tg.isManuallyResizable()) {
				tg.setPreferredHeight(
						maxHeight,
						this.getSeqMapView().getSeqMap().getView()
				);
				if (Float.MIN_VALUE < maxMax) {
					ViewModeGlyph vg = tg.getViewModeGlyph();
					if (vg instanceof AbstractGraphGlyph) {
						AbstractGraphGlyph gg = (AbstractGraphGlyph) vg;
						gg.setVisibleMaxY(maxMax);
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
