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

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.igb.viewmode.AbstractGraphGlyph;
import com.affymetrix.igb.viewmode.AbstractScoredContainerGlyphFactory;

public final class ScoredContainerGlyphFactory extends AbstractScoredContainerGlyphFactory {

	@Override
	public String getName() {
		return "scored";
	}
		
	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphIntervalSym graf, GraphState graphState) {
		return null;
	}
	
	@Override
	public boolean isFileSupported(FileTypeCategory checkCategory) {
		return true;
	}
}
