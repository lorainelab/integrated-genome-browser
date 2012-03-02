package com.affymetrix.igb.viewmode;

import java.util.Map;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.ViewModeGlyph;

public interface SemanticZoomRule {
	public ViewModeGlyph getGlyph(ViewI view);
	public ViewModeGlyph getDefaultGlyph();
	public Map<String, ViewModeGlyph> getAllViewModeGlyphs();
}
