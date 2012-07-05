package com.affymetrix.igb.action;

import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory.SemanticZoomGlyph;

public class ChangeGraphTypeAction extends ChangeViewModeAction {
	private static final long serialVersionUID = 1L;

	public ChangeGraphTypeAction(MapViewGlyphFactoryI mode) {
		super(mode);
	}

	@Override
	protected void changeViewMode(ViewModeGlyph glyph) {
		if (glyph instanceof SemanticZoomGlyph) {
			((SemanticZoomGlyph)glyph).setSummaryViewMode(mode.getName(), getSeqMapView());
			glyph.getAnnotStyle().setSummaryViewMode(mode.getName());
		}
		else {
			super.changeViewMode(glyph);
		}
	}
}
