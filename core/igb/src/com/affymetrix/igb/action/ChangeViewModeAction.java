package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.*;

import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class ChangeViewModeAction extends SeqMapViewActionA {

	private static final long serialVersionUID = 1L;
	protected final MapViewGlyphFactoryI mode;

	public ChangeViewModeAction(MapViewGlyphFactoryI mode) {
		super(mode.getDisplayName(), null, null);
		this.mode = mode;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		for (TierGlyph glyph : getTierManager().getSelectedTiers()) {
			if (glyph.getViewModeGlyph() instanceof MultiGraphGlyph && glyph.getViewModeGlyph().getChildCount() > 0) {
				for (GlyphI g : glyph.getViewModeGlyph().getChildren()) {
					if (g instanceof AbstractGraphGlyph) {
						changeViewMode(((AbstractGraphGlyph) (g)));
					}
				}
			}
			else {
				changeViewMode(glyph.getViewModeGlyph());
			}
		}
		
		// For Floating graphs
		if(getSeqMapView().getPixelFloater() != null && getSeqMapView().getPixelFloater().getChildren() != null){
			for (GlyphI glyph : getSeqMapView().getPixelFloater().getChildren()) {
				if (glyph.isSelected() && glyph instanceof AbstractGraphGlyph) {
					changeViewMode(((AbstractGraphGlyph) (glyph)));
				}
			}
		}
		
		refreshMap(false, true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(ae);
	}

	protected void changeViewMode(ViewModeGlyph glyph) {
		final ITrackStyleExtended style = glyph.getAnnotStyle();
		if (style.getSeparate() && !mode.supportsTwoTrack()) {
			style.setSeparate(false);
		}
		style.setViewMode(mode.getName());
		// special case - glyph with no data loaded yet
		if (glyph.getInfo() == null || glyph.getInfo() instanceof DummyRootSeqSymmetry) {
			glyph.getTierGlyph().setInfo(null);
		}
	}
}
