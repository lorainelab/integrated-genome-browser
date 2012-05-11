package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;

public abstract class MultiGraphGlyph extends AbstractGraphGlyph {
		
	public MultiGraphGlyph(SeqMapViewExtendedI smv, ITrackStyleExtended style) {
		super(new GraphState(style));
	}

	@Override
	public void drawMiddle(ViewI view) {
		if (getChildren() != null) {
			ViewModeGlyph child;
			int numChildren = getChildren().size();
			for (int i = 0; i < numChildren; i++) {
				child = (ViewModeGlyph) getChildren().get(i);
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

	@Override
	public void addSym(SeqSymmetry sym) {
	}

	@Override
	protected void doBigDraw(java.awt.Graphics g, GraphSym graphSym,
			java.awt.Point curr_x_plus_width, java.awt.Point max_x_plus_width, 
			float ytemp,int draw_end_index, int i) {
	}

}
