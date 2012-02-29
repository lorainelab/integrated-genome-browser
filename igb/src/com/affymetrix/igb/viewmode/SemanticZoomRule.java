package com.affymetrix.igb.viewmode;

import java.util.List;

import com.affymetrix.genoviz.bioviews.ViewI;

public interface SemanticZoomRule {
	public String chooseViewMode(ViewI view);
	public String getName();
	public List<String> getMapViewGlyphFactories();
}
