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
package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public final class CollapsedAnnotGlyphFactory extends AbstractAnnotGlyphFactory implements MapViewGlyphFactoryI {
	@Override
	public String getName() {
		return "collapsed";
	}
	@Override
	protected ViewModeGlyph createViewModeGlyph(ITrackStyleExtended style, Direction direction) {
		ViewModeGlyph viewModeGlyph = new CollapsedAnnotationGlyph(style);
		viewModeGlyph.setDirection(direction);
		return viewModeGlyph;
	}
}
