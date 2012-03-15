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
package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.PixelFloaterGlyph;
import com.affymetrix.genoviz.widget.NeoMap;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

public final class GraphGlyphUtils {

	public static final String PREF_USE_FLOATING_GRAPHS = "use floating graphs";
	public static final String PREF_ATTACHED_COORD_HEIGHT = "default attached graph coord height";
	public static final NumberFormat numberParser = NumberFormat.getNumberInstance();

	/**
	 *  Checks to make sure the the boundaries of a floating glyph are
	 *  inside the map view.
	 *  If the glyph is not a floating glyph, this will have no effect on it.
	 *  Assumes that glyph is a child of a PixelFloaterGlyph, so that
	 *   the glyph's coord box is also its pixel box.
	 */
	public static void checkPixelBounds(ViewModeGlyph gl, NeoMap map) {
		if (gl.getAnnotStyle().getFloatGraph()) {
			Rectangle mapbox = map.getView().getPixelBox();
			Rectangle2D.Double gbox = gl.getCoordBox();
			if (gbox.y < mapbox.y) {
				gl.setCoords(gbox.x, mapbox.y, gbox.width, gbox.height);
			} else if (gbox.y > (mapbox.y + mapbox.height - 10)) {
				gl.setCoords(gbox.x, mapbox.y + mapbox.height - 10, gbox.width, gbox.height);
			}
		}
	}

	public static boolean hasFloatingAncestor(GlyphI gl) {
		if (gl == null) {
			return false;
		}
		if (gl instanceof PixelFloaterGlyph) {
			return true;
		} else if (gl.getParent() == null) {
			return false;
		} else {
			return hasFloatingAncestor(gl.getParent());
		}
	}

	/**
	 *  Checks to make sure that two graphs can be compared with one
	 *  another for operations like diff, ratio, etc.
	 *  (Graphs must have exact same x positions, and if one has width coords,
	 *   the other must also.)
	 *  @return null if the graphs are comparable, or an explanation string if they are not.
	 */
	public static String graphsAreComparable(AbstractGraphGlyph graphA, AbstractGraphGlyph graphB) {
		// checking that both graphs are non-null
		if (graphA == null || graphB == null) {
			return "Must select exactly two graphs";
		}
		int numpoints = graphA.getPointCount();
		// checking that both graph have same number of points
		if (numpoints != graphB.getPointCount()) {
			return "Graphs must have the same X points";
		}
		if (graphA.hasWidth() != graphB.hasWidth()) {
			// one has width coords, the other doesn't.
			return "Must select two graphs of the same type";
		}

		// checking that both graphs have same x points
		for (int i = 0; i < numpoints; i++) {
			if (graphA.getXCoord(i) != graphB.getXCoord(i)) {
				return "Graphs must have the same X points";
			}
		}

		return null;
	}

	/** Parse a String floating-point number that may optionally end with a "%" symbol. */
	public static float parsePercent(String text) throws ParseException {
		if (text.endsWith("%")) {
			text = text.substring(0, text.length() - 1);
		}

		return numberParser.parse(text).floatValue();
	}

	public static int[] intListToArray(List<Integer> list) {
		int[] array = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	public static float[] floatListToArray(List<Float> list) {
		float[] array = new float[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}
}
