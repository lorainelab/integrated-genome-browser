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
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackStyle;

public abstract class SetSummaryThresholdActionA extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static JSlider zoomer;

	private static JSlider getZoomer() {
		if (zoomer == null) {
			NeoMap map = ((IGB)IGB.getSingleton()).getMapView().getSeqMap();
			zoomer = (JSlider)map.getZoomer(NeoMap.X);
		}
		return zoomer;
	}

	public static boolean isSummary(ITrackStyleExtended style) {
		int zoomer_value = (getZoomer().getValue() * 100 / getZoomer().getMaximum());
		int threshold = style.getSummaryThreshold();
		if (threshold == 0) {
			threshold = PreferenceUtils.getIntParam(PreferenceUtils.PREFS_THRESHOLD, PreferenceUtils.default_threshold);
		}
		return zoomer_value < threshold;
	}

	public SetSummaryThresholdActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	protected int getThresholdValue() {
		return (getZoomer().getValue() * 100 / getZoomer().getMaximum());
	}

	protected abstract int adjustThreshold(int threshold);

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		for (GlyphI glyph : getSeqMapView().getSelectedTiers()) {
			TierGlyph tierGlyph = (TierGlyph)glyph;
			ITrackStyleExtended style = tierGlyph.getAnnotStyle();
			if (style != null && style instanceof TrackStyle) {
				((TrackStyle) style).setSummaryThreshold(getThresholdValue());
			}
		}
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		getSeqMapView().updatePanel();
	}
}
