/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import javax.swing.JSlider;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackStyle;

public abstract class SetSummaryThresholdActionA extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private JSlider getZoomer() {
		return (JSlider)getSeqMapView().getSeqMap().getZoomer(NeoMap.X);
	}

	public SetSummaryThresholdActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	protected int getZoomerValue() {
		return (getZoomer().getValue() * 100 / getZoomer().getMaximum());
	}

	protected abstract int adjustThreshold(int threshold);

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		int zoomerValue = getZoomerValue();
		int summaryThreshold = adjustThreshold(zoomerValue);
		for (GlyphI glyph : getSeqMapView().getSelectedTiers()) {
			TierGlyph tierGlyph = (TierGlyph)glyph;
			ITrackStyleExtended style = tierGlyph.getAnnotStyle();
			if (style != null && style instanceof TrackStyle) {
				((TrackStyle) style).setSummaryThreshold(summaryThreshold);
				if (tierGlyph.getViewModeGlyph() instanceof NeoRangeListener) {
					NeoRangeEvent newevt = new NeoRangeEvent(getSeqMapView(), tierGlyph.getCoordBox().x, tierGlyph.getCoordBox().x + tierGlyph.getCoordBox().width);
					((NeoRangeListener) tierGlyph.getViewModeGlyph()).rangeChanged(newevt);
				}
			}
		}
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		getSeqMapView().getSeqMap().updateWidget();
	}
}
