package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.ZoomDisplayer;

public class DepthZoomDisplayer implements ZoomDisplayer {
	private GlyphI glyph;

	public DepthZoomDisplayer(GlyphI glyph) {
		super();
		this.glyph = glyph;
	}

	@Override
	public GlyphI getZoomGlyph(ViewI view) {
		if (glyph != null && (view.getTransform().getScaleX() < 0.002)) {
			return glyph;
		}
		return null;
	}

}
