package com.affymetrix.igb.action;

import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class ChangeGraphTypeAction extends ChangeViewModeAction {
	private static final long serialVersionUID = 1L;

	public ChangeGraphTypeAction(MapViewGlyphFactoryI mode) {
		super(mode);
	}

	@Override
	protected void changeViewMode(ViewModeGlyph glyph) {
		super.changeViewMode(glyph);
	}
}
