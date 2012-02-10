package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface StyleGlyphI extends GlyphI {
	public ITrackStyleExtended getAnnotStyle();
}
