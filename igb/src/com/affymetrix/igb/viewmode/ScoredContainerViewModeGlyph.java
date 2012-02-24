
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *
 * @author hiralv
 */
public class ScoredContainerViewModeGlyph extends AbstractViewModeGlyph{
	
	ScoredContainerViewModeGlyph(ITrackStyleExtended style){
		setStyle(style);
	}
	
	@Override
	public void setPreferredHeight(double height, ViewI view) {
		// Still to be implemented
	}

	@Override
	public int getActualSlots() {
		return 1;
		// Still to be implemented
	}
	
}
