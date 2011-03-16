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

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.PixelFloaterGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class GraphGlyphUtils {

	public static final String PREF_USE_FLOATING_GRAPHS = "use floating graphs";
	public static final String PREF_ATTACHED_COORD_HEIGHT = "default attached graph coord height";
	public static final NumberFormat numberParser = NumberFormat.getNumberInstance();
	
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
	public static final String MATH_SUM = "sum";
	public static final String MATH_DIFFERENCE = "diff";
	public static final String MATH_PRODUCT = "product";
	public static final String MATH_RATIO = "ratio";

	private static float operate(float a, float b, String operation) {
		if (MATH_SUM.equals(operation)) {
			return a + b;
		} else if (MATH_DIFFERENCE.equals(operation)) {
			return a - b;
		} else if (MATH_PRODUCT.equals(operation)) {
			return a * b;
		} else if (MATH_RATIO.equals(operation)) {
			float y;
			if (b == 0) {
				return 0; // hack to avoid infinities
			} else {
				y = a / b;
			}
			if (Float.isInfinite(y) || Float.isNaN(y)) {
				y = 0.0f;
			}
			return y;
		}
		return 0;
	}

	private static GraphSym graphRegionArithmetic(GraphGlyph graphA, GraphGlyph graphB, String operation) {
		int[] xA = graphA.getXCoords();
		int[] xB = graphB.getXCoords();
		int[] wA = graphA.getWCoords();
		int[] wB = graphB.getWCoords();
		float[] yA = graphA.copyYCoords();
		float[] yB = graphB.copyYCoords();
		String symbol = ",";
		if (MATH_SUM.equals(operation)) {
			symbol = "+";
		} else if (MATH_DIFFERENCE.equals(operation)) {
			symbol = "-";
		} else if (MATH_PRODUCT.equals(operation)) {
			symbol = "*";
		} else if (MATH_RATIO.equals(operation)) {
			symbol = "/";
		}
		List<Integer> xList = new ArrayList<Integer>();
		List<Integer> wList = new ArrayList<Integer>();
		List<Float> yList = new ArrayList<Float>();
		int currentX = Integer.MIN_VALUE;
		int nextX = 0;
		int aIndex = 0;
		int bIndex = 0;
		float aValue = 0;
		float bValue = 0;
		while (aIndex < xA.length || bIndex < xB.length) {
			nextX = Integer.MAX_VALUE;
			if (aIndex < xA.length && xA[aIndex] > currentX && xA[aIndex] < nextX) {
				nextX = xA[aIndex];
			}
			else if (aIndex < xA.length && xA[aIndex] + wA[aIndex] > currentX && xA[aIndex] + wA[aIndex] < nextX) {
				nextX = xA[aIndex] + wA[aIndex];
			}
			if (bIndex < xB.length && xB[bIndex] > currentX && xB[bIndex] < nextX) {
				nextX = xB[bIndex];
			}
			else if (bIndex < xB.length && xB[bIndex] + wB[bIndex] > currentX && xB[bIndex] + wB[bIndex] < nextX) {
				nextX = xB[bIndex] + wB[bIndex];
			}
			if (aIndex < xA.length && currentX >= xA[aIndex] && nextX <= xA[aIndex] + wA[aIndex]) {
				aValue = yA[aIndex];
			}
			else {
				aValue = 0;
			}
			if (bIndex < xB.length && currentX >= xB[bIndex] && nextX <= xB[bIndex] + wB[bIndex]) {
				bValue = yB[bIndex];
			}
			else {
				bValue = 0;
			}
			float currentY = operate(aValue, bValue, operation);
			if (currentY > 0) {
				xList.add(currentX);
				wList.add(nextX - currentX);
				yList.add(currentY);
			}
			if (aIndex < xA.length && xA[aIndex] + wA[aIndex] <= nextX) {
				aIndex++;
			}
			if (bIndex < xB.length && xB[bIndex] + wB[bIndex] <= nextX) {
				bIndex++;
			}
			currentX = nextX;
		}
		String newname = operation + ": (" + graphA.getLabel() + ") " +
		symbol + " (" + graphB.getLabel() + ")";
		BioSeq aseq =
			((GraphSym) graphA.getInfo()).getGraphSeq();
		newname = GraphSymUtils.getUniqueGraphID(newname, aseq);
		int[] x = new int[xList.size()];
		for (int index = 0; index < xList.size(); index++) {
			x[index] = xList.get(index);
		}
		int[] w = new int[wList.size()];
		for (int index = 0; index < wList.size(); index++) {
			w[index] = wList.get(index);
		}
		float[] y = new float[yList.size()];
		for (int index = 0; index < yList.size(); index++) {
			y[index] = yList.get(index);
		}
		if (x.length == 0) { // if no data, just create a dummy zero span
			x = new int[]{Math.min(xA[0], xB[0])};
			y = new float[]{0};
			w = new int[]{1};
		}
		GraphSym newsym = new GraphSym(x, w, y, newname, aseq);

		newsym.setGraphName(newname);
		newsym.getGraphState().setGraphStyle(graphA.getGraphState().getGraphStyle());
		newsym.getGraphState().setHeatMap(graphA.getGraphState().getHeatMap());
		return newsym;
	}

	/**
	 *  Combines two graphs by the given arithmetical operation.
	 *  Returns null if the two graphs are not comparable via {@link #graphsAreComparable(GraphGlyph,GraphGlyph)}.
	 *  During division, indefinite values are replaced by zero.
	 */
	public static GraphSym graphArithmetic(GraphGlyph graphA, GraphGlyph graphB, String operation) {
		String error = GraphGlyphUtils.graphsAreComparable(graphA, graphB);

		if ("Graphs must have the same X points".equals(error) && graphA.hasWidth() && graphB.hasWidth()) {
			return GraphGlyphUtils.graphRegionArithmetic(graphA, graphB, operation);
		}
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

		BioSeq aseq =
				((GraphSym) graphA.getInfo()).getGraphSeq();
		newname = GraphSymUtils.getUniqueGraphID(newname, aseq);
		GraphSym newsym;
		if (!graphA.hasWidth()) {
			newsym = new GraphSym(graphA.getXCoords(), newY, newname, aseq);
		} else {
			newsym = new GraphIntervalSym(graphA.getXCoords(), graphA.getWCoords(), newY, newname, aseq);
		}

		newsym.setGraphName(newname);
		newsym.getGraphState().setGraphStyle(graphA.getGraphState().getGraphStyle());
		newsym.getGraphState().setHeatMap(graphA.getGraphState().getHeatMap());
		return newsym;
	}

	/** Parse a String floating-point number that may optionally end with a "%" symbol. */
	public static float parsePercent(String text) throws ParseException {
		if (text.endsWith("%")) {
			text = text.substring(0, text.length() - 1);
		}
	
		return numberParser.parse(text).floatValue();
	}
}
