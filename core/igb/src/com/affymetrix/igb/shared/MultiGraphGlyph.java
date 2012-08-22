package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;

public abstract class MultiGraphGlyph extends AbstractGraphGlyph {
		
	public MultiGraphGlyph(ITrackStyleExtended style) {
		super(style);
	}

	@Override
	public void drawMiddle(ViewI view) {
		if (getChildren() != null) {
			AbstractViewModeGlyph child;
			int numChildren = getChildren().size();
			for (int i = 0; i < numChildren; i++) {
				child = (AbstractViewModeGlyph) getChildren().get(i);
				child.drawMiddle(view);
			}
		}
	}

	@Override
	public void setPreferredHeight(double height, ViewI view) {
	}

	@Override
	public int getActualSlots() {
		return 0;
	}

	@Override
	public void setPreferences(java.util.Map<String, Object> preferences) {
	}

}
