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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.TierGlyph;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.Action;

/**
 * Make all the selected tracks easy to compare
 * by giving them the same size and scale.
 * <p> TODO
 * </p>
 * <ol>
 * <li> What's the correct way to resize? setPreferredHeight?
 * <li> Scale selected graphs. What to do beyond setting heights?
 * <li> Set the same stack depth for selected annotation tracks.
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
		double maxHeight = 0;
		for (GlyphI g: l) {
			double h = g.getCoordBox().height;
			if (maxHeight < h) {
				maxHeight = h;
			}
		}
		for (GlyphI g: l) {
			Rectangle2D.Double b = g.getCoordBox();
			System.out.println("Glyph: " + g.getClass().getName());
			TierGlyph tg = (TierGlyph) g;
			if (tg.isManuallyResizable()) {
				tg.setPreferredHeight(
						maxHeight,
						this.getSeqMapView().getSeqMap().getView()
				);
			}
			//g.setCoords(b.x, b.y, b.width, maxHeight);
		}
		this.getSeqMapView().updateUI(); // Is this right? Need we do more?
	}

}
