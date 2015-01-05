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
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Connects a slider to a tiered map
 * for the purpose of zooming without stretching.
 * To do this it adjusts the number of "slots" for annotations.
 * Michael Lawrence of Genentech had the idea and, hence, the name.
 * Zoom "level"s go from minimum slots allowed (1 or 2?) zoomed all the way out,
 * to max slots in view (slotsNeeded) zoomed all the way in.
 * @author Eric Blossom
 */
public class LawrencianZoomer implements ChangeListener {

	private AffyLabelledTierMap context = null;
	private BoundedRangeModel range = null;
	private SharedListSelectionHandler rangeAdjuster = null;

	/**
	 * Links a map to a zoomer.
	 * This zoomer does not add itself as a listener to the range model.
	 * Use <code>slider.addChangeListener(<var>theMap</var>) to listen.
	 * @param theMap containing the tiers. Selected tiers are adjusted.
	 * @param theControl the max will be set upon tier selection changes.
	 */
	public LawrencianZoomer(AffyLabelledTierMap theMap,
			BoundedRangeModel theControl) {
		if (null == theMap) {
			throw new IllegalArgumentException("The map cannot be null.");
		}
		this.context = theMap;
		if (null != theControl) {
			this.range = theControl;
			this.rangeAdjuster = new SharedListSelectionHandler(theControl);
			this.context.addListSelectionListener(this.rangeAdjuster);
		}
	}

	/**
	 * So it can be called from SeqMapView.
	 */
	public LawrencianZoomer(AffyTieredMap theMap, BoundedRangeModel theControl) {
		this((AffyLabelledTierMap) theMap, theControl);
	}

	/**
	 * Constructs a zoomer that can then listen to an external range.
	 * @param theMap use <code>slider.addChangeListener(<var>theMap</var>).
	 */
	public LawrencianZoomer(AffyLabelledTierMap theMap) {
		this(theMap, new DefaultBoundedRangeModel());
	}

	/**
	 * So it can be called from SeqMapView.
	 */
	public LawrencianZoomer(AffyTieredMap theMap) {
		this((AffyLabelledTierMap) theMap);
	}

	/**
	 * Note when selection has changed and set the min and max accordingly.
	 * This is how we get the news from the tiered map that a tier has been selected.
	 * So set the min and max of the scroll bar to match slots in view.
	 * Actually, maybe this should not be in here, but a separate class.
	 */
	private class SharedListSelectionHandler implements ListSelectionListener {

		private BoundedRangeModel range = null;

		public SharedListSelectionHandler(BoundedRangeModel theRange) {
			this.range = theRange;
			// Do we need to invert?
			this.range.setMinimum(Math.max(this.range.getMinimum(), 1));
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			// Source is an inner class of our context.
			// Maybe we should complete that chain instead of just assuming it.
			// Here we assume the souce is our context
			// and we figure out selection ourselves
			// instead of using a ListSelectionModel.
			List<TierLabelGlyph> l
					= LawrencianZoomer.this.context.getOrderedTierLabels();
			ViewI v = LawrencianZoomer.this.context.getView();
			// Set the range maximum to max of slots needed.
			int ourMax = 1, ourVal = 1;
			for (TierLabelGlyph g: l) {
				boolean itCounts = g.isSelected();
				if (itCounts) {
					TierGlyph tg = g.getReferenceTier();
					ourMax = Math.max(ourMax, tg.getSlotsNeeded(v));
					ourVal = Math.max(ourVal, tg.getActualSlots());
				}
			}
			this.range.setRangeProperties(ourMax - ourVal, 0, 1, ourMax, true);
		}

	}

	private List<TierLabelGlyph> justSelected(List<TierLabelGlyph> allOfThem) {
		List<TierLabelGlyph> answer = new ArrayList<TierLabelGlyph>();
		for (TierLabelGlyph t: allOfThem) {
			if (t.isSelected()) {
				answer.add(t);
			}
		}
		return answer;
	}

	/**
	 * Set the maximum slots for the selected tier to be a portion of those needed.
	 * @param theLevel to set maximum stack depth.
	 */
	private void setZoomLevel(int theLevel) {
		if (theLevel < 0) {
			throw new IllegalArgumentException(
					"theLevel cannot be negative. theLevel: "
					+ theLevel);
		}
		theLevel += 1;
		List<TierLabelGlyph> orderedGlyphs = context.getOrderedTierLabels();
		List<TierLabelGlyph> selection = justSelected(orderedGlyphs);
		if (selection.isEmpty()) {
			System.err.println(this.getClass().getSimpleName() + ".setZoomLevel: No tracks selected.");
		}
		for (TierLabelGlyph tlg : selection) {
		    TierGlyph g = tlg.getReferenceTier();
			int slotsAtZoom = theLevel;
			TierGlyph tg = g;
			ITrackStyleExtended style = tg.getAnnotStyle();
			switch (g.getDirection()) {
				case FORWARD:
					style.setForwardMaxDepth(slotsAtZoom);
					break;
				case REVERSE:
					style.setReverseMaxDepth(slotsAtZoom);
					break;
				default:
				case BOTH:
				case NONE:
				case AXIS:
					style.setMaxDepth(slotsAtZoom);
			}
			context.packTiers(true, true, false);
			context.updateWidget(true);
		}
	}

	private boolean zoomDynamically = true;
	@Override
	public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider) e.getSource();
		if (zoomDynamically || !source.getValueIsAdjusting()) {
			if (!zoomDynamically) {
				source.setEnabled(false);
			}
			// Invert the vertically oriented zoomer.
			int zoomLevel = source.getMaximum() - source.getValue();
			if (source.getModel() != this.range) {
				// Scale it.
				zoomLevel = this.range.getMaximum() * zoomLevel / source.getMaximum();
			}
			this.setZoomLevel(zoomLevel);
			if (!zoomDynamically) {
				source.setEnabled(true);
			}
		}
	}

}
