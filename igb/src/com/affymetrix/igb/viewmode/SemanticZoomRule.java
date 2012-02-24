package com.affymetrix.igb.viewmode;

import com.affymetrix.genoviz.bioviews.ViewI;

public interface SemanticZoomRule {
	public String chooseViewMode(ViewI view);
	public String getName();
}
