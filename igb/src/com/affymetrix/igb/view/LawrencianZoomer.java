package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.util.List;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Connects a slider to a tiered map
 * for the purpose of zooming without stretching.
 * To do this it adjusts the number of "slots" for annotations.
 * Zoom "level"s go from 0 (zoomed all the way out)
 * to 100 zoomed all the way in.
 * This also matches Ann's terminology for zoom "level".
 *   0 is "low zoom"  like low  magnification.
 * 100 is "high zoom" like high magnification.
 * @author Eric Blossom
 */
public class LawrencianZoomer implements ChangeListener {

	private AffyTieredMap context;

	public LawrencianZoomer(AffyTieredMap theMap) {
		this.context = theMap;
	}

	/**
	 * Set the maximum slots for each tier to be a portion of those needed.
	 * @param theLevel Should be the percentage (0 to 100 inclusive) of the need.
	 */
	private void setZoomLevel(int theLevel) {
		if (theLevel < 0 || 100 < theLevel) {
			throw new IllegalArgumentException(
					"theLevel cannot be negative nor over 100. theLevel: "
					+ theLevel);
		}
		theLevel += 1;
		List<TierGlyph> l = context.getTiers();
		for (TierGlyph g : l) {
			int slotsAtZoom = 0;
			if (theLevel < 101) {
				int sn = g.getSlotsNeeded(this.context.getView());
				float percentOfSlotsNeeded = theLevel / 100.0f;
				slotsAtZoom = Math.round(sn * percentOfSlotsNeeded); // ceil?
				slotsAtZoom = Math.max(1, slotsAtZoom);
			}
			ViewModeGlyph vmg = g.getViewModeGlyph();
			ITrackStyleExtended style = vmg.getAnnotStyle();
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
	
	@Override
	public void stateChanged(ChangeEvent e) {
		assert e.getSource().getClass() == JSlider.class;
		JSlider source = (JSlider) e.getSource();
		if (!source.getValueIsAdjusting()) {
			source.setEnabled(false);
			int zoomLevel = 100 - source.getValue();
			this.setZoomLevel(zoomLevel);
			source.setEnabled(true);
		}
	}
	
}
