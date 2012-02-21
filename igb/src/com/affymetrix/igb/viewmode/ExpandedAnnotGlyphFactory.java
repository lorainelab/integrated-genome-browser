/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.StyleGlyphI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.util.NeoConstants;

public final class ExpandedAnnotGlyphFactory extends AbstractAnnotGlyphFactory implements MapViewGlyphFactoryI {

	// glyph class
	private class ExpandedAnnotationGlyph extends AbstractAnnotationGlyph implements StyleGlyphI {
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
	// end glyph class

	@Override
	public String getName() {
		return "expanded";
	}
	@Override
	protected ViewModeGlyph createViewModeGlyph(ITrackStyleExtended style, Direction direction) {
		ViewModeGlyph viewModeGlyph = new ExpandedAnnotationGlyph(style);
		viewModeGlyph.setDirection(direction);
		if (direction != Direction.REVERSE) {
			((FasterExpandPacker)((ExpandedAnnotationGlyph)viewModeGlyph).getPacker()).setMoveType(NeoConstants.UP);
		}
		return viewModeGlyph;
	}

	// for GenericGraphGlyphFactory, can be removed when that is removed
	public static ViewModeGlyph getViewModeGlyph(ITrackStyleExtended style) {
		return new ExpandedAnnotGlyphFactory().new ExpandedAnnotationGlyph(style);
	}
}
