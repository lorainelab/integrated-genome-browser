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
package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.Rectangle2D;
import java.util.*;


/**
 * An implementation of graphs for NeoMaps,
 * capable of rendering graphs in a variety of styles.
 * Started with {@link com.affymetrix.genoviz.glyph.BasicGraphGlyph}
 * and improved from there.
 * <p><em><strong>This is only meant for graphs on horizontal maps.</strong></em>
 * </p>
 */
public class GraphTierGlyph extends AbstractTierGlyph{
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		temp.put("y_axis", Boolean.class);
		PREFERENCES = Collections.unmodifiableMap(temp);
	}

	public GraphTierGlyph(ITrackStyleExtended style) {
		super();
		setStyle(style);
	}
							
	@Override
	protected boolean shouldDrawToolBar(){
		return this.getChildCount() > 1;
	}
	
	@Override
	public Map<String, Class<?>> getPreferences() {
		return new HashMap<String, Class<?>>(PREFERENCES);
	}

	@Override
	public void setPreferences(Map<String, Object> preferences) {
	}
	
}
