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


/**
 * An implementation of graphs for NeoMaps,
 * capable of rendering graphs in a variety of styles.
 * Started with {@link com.affymetrix.genoviz.glyph.BasicGraphGlyph}
 * and improved from there.
 * <p><em><strong>This is only meant for graphs on horizontal maps.</strong></em>
 * </p>
 */
public class GraphTierGlyph extends AbstractTierGlyph{
	public GraphTierGlyph(ITrackStyleExtended style) {
		super();
		setStyle(style);
	}
}
