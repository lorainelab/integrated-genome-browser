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

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.GraphSymFloat;
import java.awt.Rectangle;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.PixelFloaterGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;

public final class GraphGlyphUtils {

	public static final String PREF_USE_FLOATING_GRAPHS = "use floating graphs";
	public static final String PREF_ATTACHED_COORD_HEIGHT = "default attached graph coord height";
	/** Pref for whether newly-constructed graph glyphs should only show a
	 *  limited range of values.
	 */
	public static final String PREF_APPLY_PERCENTAGE_FILTER = "apply graph percentage filter";
	public static final String PREF_USE_URL_AS_NAME = "Use complete URL as graph name";
	public static final boolean default_use_url_as_name = false;

	/**
	 *  Checks to make sure the the boundaries of a floating glyph are
	 *  inside the map view.
	 *  See {@link #checkPixelBounds(GraphGlyph, AffyTieredMap)}.
	 */
	public static boolean checkPixelBounds(GraphGlyph gl, SeqMapView gviewer) {
		AffyTieredMap map = gviewer.getSeqMap();
		return checkPixelBounds(gl, map);
	}

	/**
	 *  Checks to make sure the the boundaries of a floating glyph are
	 *  inside the map view.
	 *  Return true if graph coords were changed, false otherwise.
	 *  If the glyph is not a floating glyph, this will have no effect on it
	 *  and will return false.
	 *  Assumes that graph glyph is a child of a PixelFloaterGlyph, so that
	 *   the glyph's coord box is also its pixel box.
	 */
	public static boolean checkPixelBounds(GraphGlyph gl, AffyTieredMap map) {
		boolean changed_coords = false;
		if (gl.getGraphState().getFloatGraph()) {
			Rectangle mapbox = map.getView().getPixelBox();
			Rectangle2D.Double gbox = gl.getCoordBox();
			if (gbox.y < mapbox.y) {
				gl.setCoords(gbox.x, mapbox.y, gbox.width, gbox.height);
				changed_coords = true;
			} else if (gbox.y > (mapbox.y + mapbox.height - 10)) {
				gl.setCoords(gbox.x, mapbox.y + mapbox.height - 10, gbox.width, gbox.height);
				changed_coords = true;
			}
		}
		return changed_coords;
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

	public static Preferences getGraphPrefsNode() {
		return UnibrowPrefsUtil.getTopNode().node("graphs");
	}

	/**
	 *  Checks to make sure that two graphs can be compared with one
	 *  another for operations like diff, ratio, etc.
	 *  (Graphs must have exact same x positions, and if one has width coords,
	 *   the other must also.)
	 *  @return null if the graphs are comparable, or an explanation string if they are not.
	 */
	public static String graphsAreComparable(GraphGlyph graphA, GraphGlyph graphB) {
		// checking that both graphs are non-null
		if (graphA == null || graphB == null) {
			return "Must select exactly two graphs";
		}
		int numpoints = graphA.getPointCount();
		// checking that both graph have same number of points
		if (numpoints != graphB.getPointCount()) {
			return "Graphs must have the same X points";
		}
		if ((graphA.getWCoords() == null) != (graphB.getWCoords() == null)) {
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
	public static final String MATH_SUM = "sum";
	public static final String MATH_DIFFERENCE = "diff";
	public static final String MATH_PRODUCT = "product";
	public static final String MATH_RATIO = "ratio";

	/**
	 *  Combines two graphs by the given arithmetical operation.
	 *  Returns null if the two graphs are not comparable via {@link #graphsAreComparable(GraphGlyph,GraphGlyph)}.
	 *  During division, indefinite values are replaced by zero.
	 */
	public static GraphSymFloat graphArithmetic(GraphGlyph graphA, GraphGlyph graphB, String operation) {
		String error = GraphGlyphUtils.graphsAreComparable(graphA, graphB);

		if (error != null) {
			ErrorHandler.errorPanel("ERROR", error);
			return null;
		}

		int numpoints = graphA.getPointCount();
		float newY[] = new float[numpoints];

		String symbol = ",";
		if (MATH_SUM.equals(operation)) {
			for (int i = 0; i < numpoints; i++) {
				newY[i] = graphA.getYCoord(i) + graphB.getYCoord(i);
			}
			symbol = "+";
		} else if (MATH_DIFFERENCE.equals(operation)) {
			for (int i = 0; i < numpoints; i++) {
				newY[i] = graphA.getYCoord(i) - graphB.getYCoord(i);
			}
			symbol = "-";
		} else if (MATH_PRODUCT.equals(operation)) {
			for (int i = 0; i < numpoints; i++) {
				newY[i] = graphA.getYCoord(i) * graphB.getYCoord(i);
			}
			symbol = "*";
		} else if (MATH_RATIO.equals(operation)) {
			for (int i = 0; i < numpoints; i++) {
				if (graphB.getYCoord(i) == 0) {
					newY[i] = 0; // hack to avoid infinities
				} else {
					newY[i] = graphA.getYCoord(i) / graphB.getYCoord(i);
				}
				if (Float.isInfinite(newY[i]) || Float.isNaN(newY[i])) {
					newY[i] = 0.0f;
				}
			}
			symbol = "/";
		}

		String newname = operation + ": (" + graphA.getLabel() + ") " +
				symbol + " (" + graphB.getLabel() + ")";

		MutableAnnotatedBioSeq aseq =
				((GraphSym) graphA.getInfo()).getGraphSeq();
		newname = GraphSymUtils.getUniqueGraphID(newname, aseq);
		GraphSymFloat newsym;
		if (graphA.getWCoords() == null) {
			newsym = new GraphSymFloat(graphA.getXCoords(), newY, newname, aseq);
		} else {
			newsym = new GraphIntervalSym(graphA.getXCoords(), graphA.getWCoords(), newY, newname, aseq);
		}

		newsym.setGraphName(newname);
		newsym.getGraphState().setGraphStyle(graphA.getGraphState().getGraphStyle());
		newsym.getGraphState().setHeatMap(graphA.getGraphState().getHeatMap());
		return newsym;
	}
}
