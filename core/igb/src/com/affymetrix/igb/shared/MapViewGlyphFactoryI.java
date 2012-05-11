/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.shared;

import java.util.Map;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;

public interface MapViewGlyphFactoryI  {
	public void init(Map<String, Object> options);
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction, SeqMapViewExtendedI smv);
	public String getName();
	public String getDisplayName();
	public boolean supportsTwoTrack();
	public boolean isCategorySupported(FileTypeCategory category);
	public boolean isURISupported(String uri);
	public boolean canAutoLoad(String uri);
}
