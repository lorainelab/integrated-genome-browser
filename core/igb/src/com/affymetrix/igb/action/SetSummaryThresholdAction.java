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

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackStyle;

public class SetSummaryThresholdAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final SetSummaryThresholdAction ACTION = new SetSummaryThresholdAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SetSummaryThresholdAction getAction() {
		return ACTION;
	}

	protected SetSummaryThresholdAction() {
		super(IGBConstants.BUNDLE.getString("setSummaryThresholdAction"),
			"16x16/actions/summary.png", "22x22/actions/summary.png");
	}

	public SetSummaryThresholdAction(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	private JSlider getZoomer() {
		return (JSlider)getSeqMapView().getSeqMap().getZoomer(NeoMap.X);
	}

	protected int getZoomerValue() {
		return (getZoomer().getValue() * 100 / getZoomer().getMaximum());
	}

	public boolean isDetail(ITrackStyleExtended style) {
		int trackThreshold = PreferenceUtils.getIntParam(PreferenceUtils.PREFS_THRESHOLD, PreferenceUtils.default_threshold);
		if (style != null && style.getSummaryThreshold() > 0) {
			trackThreshold = style.getSummaryThreshold();
		}
		return getZoomerValue() >= trackThreshold;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		int zoomerValue = getZoomerValue();
		int summaryThreshold;
		for (GlyphI glyph : getSeqMapView().getSelectedTiers()) {
			TierGlyph tierGlyph = (TierGlyph)glyph;
			ITrackStyleExtended style = tierGlyph.getAnnotStyle();
			if (style != null && style instanceof TrackStyle) {
				boolean isDetail = isDetail(style);
				if (isDetail) {
					summaryThreshold = Math.min(100, zoomerValue + 1);
				}
				else {
					summaryThreshold = Math.max(0, zoomerValue - 1);
				}
				((TrackStyle) style).setSummaryThreshold(summaryThreshold);
				if (tierGlyph.getViewModeGlyph() instanceof NeoRangeListener) {
					NeoRangeEvent newevt = new NeoRangeEvent(getSeqMapView(), tierGlyph.getCoordBox().x, tierGlyph.getCoordBox().x + tierGlyph.getCoordBox().width);
					((NeoRangeListener) tierGlyph.getViewModeGlyph()).rangeChanged(newevt);
				}
			}
		}
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		getSeqMapView().getSeqMap().updateWidget();
		getZoomer().repaint();
	}
}
