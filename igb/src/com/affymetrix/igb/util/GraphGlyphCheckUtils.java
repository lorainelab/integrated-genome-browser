/**
 *   Copyright (c) 2001-2006 Affymetrix, Inc.
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
package com.affymetrix.igb.util;

import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public final class GraphGlyphCheckUtils {

	/**
	 *  Checks to make sure the the boundaries of a floating glyph are
	 *  inside the map view.
	 *  If the glyph is not a floating glyph, this will have no effect on it.
	 *  Assumes that graph glyph is a child of a PixelFloaterGlyph, so that
	 *   the glyph's coord box is also its pixel box.
	 */
	public static void checkPixelBounds(GraphGlyph gl, AffyTieredMap map) {
		if (gl.getGraphState().getFloatGraph()) {
			Rectangle mapbox = map.getView().getPixelBox();
			Rectangle2D.Double gbox = gl.getCoordBox();
			if (gbox.y < mapbox.y) {
				gl.setCoords(gbox.x, mapbox.y, gbox.width, gbox.height);
			} else if (gbox.y > (mapbox.y + mapbox.height - 10)) {
				gl.setCoords(gbox.x, mapbox.y + mapbox.height - 10, gbox.width, gbox.height);
			}
		}
	}
}
