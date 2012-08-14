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

/**
 * Factory to create a specific type of ViewModeGlyph. MapViewGlyphFactoryI
 * instances can be registered in the igb bundle Activator
 * initMapViewGlyphFactorys() method.
 */
public interface MapViewGlyphFactoryI  {
	public static final int DEFAULT_CHILD_HEIGHT = 25;
	
	/**
	 * initialize the factory
	 * @param options - any options appropriate to the factory
	 */
	public void init(Map<String, Object> options);
	/**
	 * Create empty view mode glyph
	 * @param style
	 * @param direction
	 * @param gviewer
	 * @return 
	 */
	public AbstractViewModeGlyph createViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction direction, SeqMapViewExtendedI gviewer);
	/**
	 * create a ViewModeGlyph for the SeqSymmetry
	 * @param sym - The SeqSymmetry (object model) for the TierGlyph
	 * @param style - track style
	 * @param tier_direction - the direction of the Tier
	 * @param smv - reference to the SeqMapView parent of the Tier
	 * @return the ViewModeGlyph
	 */
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction, SeqMapViewExtendedI smv);
	/**
	 * unique identifier
	 * @return name of the factory
	 */
	public String getName();
	/**
	 * name that will be displayed to the user
	 * @return display name
	 */
	public String getDisplayName();
	/**
	 * if this view mode glyph supports two (forward and reverse) tracks
	 * @return supports two track
	 */
	public boolean supportsTwoTrack();
	/**
	 * specifies if this view mode glyph supports the specified category
	 * @param category - the FileTypeCategory to test
	 * @return if the category is supported
	 */
	public boolean isCategorySupported(FileTypeCategory category);
	/**
	 * specifies if the uri can be autoloaded vs. waiting for the Load Data
	 *   button to be pressed
	 * @param uri - the uri to test
	 * @return if the uri can be autoloaded
	 */
	public boolean canAutoLoad(String uri);
}
