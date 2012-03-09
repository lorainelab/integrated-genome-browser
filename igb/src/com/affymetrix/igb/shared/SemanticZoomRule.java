package com.affymetrix.igb.shared;

import java.util.Map;

import com.affymetrix.genoviz.bioviews.ViewI;

public interface SemanticZoomRule {
	public ViewModeGlyph getGlyph(ViewI view);
	public ViewModeGlyph getDefaultGlyph();
	public Map<String, ViewModeGlyph> getAllViewModeGlyphs();
}
