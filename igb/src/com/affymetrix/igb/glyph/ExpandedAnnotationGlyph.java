package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.StyleGlyphI;

/**
 *  ViewModeGlyph for expanded annotations
 */
public class ExpandedAnnotationGlyph extends AbstractAnnotationGlyph implements StyleGlyphI {
	public ExpandedAnnotationGlyph(ITrackStyleExtended style) {
		super(style);
	}
	/** Changes the maximum depth of the expanded packer.
	 *  This does not call pack() afterwards.
	 */
	private void setMaxExpandDepth(int max) {
		((FasterExpandPacker)getPacker()).setMaxSlots(max);
	}

	@Override
	public int getActualSlots(){
		return ((FasterExpandPacker)getPacker()).getActualSlots();
	}

	@Override
	protected void setDepth() {
		setMaxExpandDepth(style.getMaxDepth());
	}

	@Override
	protected PackerI createPacker() {
		return new FasterExpandPacker();
	}
}
