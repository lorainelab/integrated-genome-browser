package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.igb.shared.CollapsePacker;
import com.affymetrix.igb.shared.StyleGlyphI;

/**
 *  ViewModeGlyph for collapsed annotations
 */
public class CollapsedAnnotationGlyph extends AbstractAnnotationGlyph implements StyleGlyphI {
	public CollapsedAnnotationGlyph(ITrackStyleExtended style) {
		super(style);
	}

	@Override
	public int getActualSlots(){
		return 1;
	}

	@Override
	protected void setDepth() {
	}

	@Override
	protected PackerI createPacker() {
		return new CollapsePacker();
	}
}
