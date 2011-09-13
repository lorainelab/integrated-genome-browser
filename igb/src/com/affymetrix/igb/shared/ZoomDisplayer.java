package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;

/**
 * interface for displaying zoom graphs at different levels
 * this will determine the zoom level from the ViewI, and return
 * a GraphGlyph, or null if the zoom is low enough to show annotations
 */
public interface ZoomDisplayer {
	public GlyphI getZoomGlyph(ViewI view);
}
