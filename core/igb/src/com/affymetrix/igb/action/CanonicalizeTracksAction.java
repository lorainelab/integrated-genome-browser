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
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import javax.swing.JOptionPane;

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

	public CanonicalizeTracksAction() {
		super("Canonicalize Tracks...",
				"16x16/actions/equalizer.png",
				"22x22/actions/equalizer.png");
		putValue(Action.SHORT_DESCRIPTION,
				"Make the scales of the selected tracks match.");
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
				System.out.println("   We gots a graph!");
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
			ViewModeGlyph vg = tg.getViewModeGlyph();
			System.out.println(this.getClass().getName() + ": " + info.toString());
			System.out.println(this.getClass().getName() + ": " + vg.toString());
		}
		JOptionPane.showMessageDialog(null,
				"Thank you for your interest. Setting heights to " + maxMax,
				this.getClass().getSimpleName(),
				JOptionPane.INFORMATION_MESSAGE);
		for (GlyphI g: l) {
			Rectangle2D.Double b = g.getCoordBox();
			System.out.println("Glyph: " + g.getClass().getName());
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

}
