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

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.graph.GraphOperator;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genoviz.glyph.PixelFloaterGlyph;
import com.affymetrix.genoviz.widget.NeoMap;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class GraphGlyphUtils {

	public static final String PREF_USE_FLOATING_GRAPHS = "use floating graphs";
	public static final String PREF_ATTACHED_COORD_HEIGHT = "default attached graph coord height";
	public static final NumberFormat numberParser = NumberFormat.getNumberInstance();

	private static final String selectExactGraphsMessage = "Select exactly {0} graphs";
	private static final String selectMinGraphsMessage = "Select at least {0} graphs";
	private static final String selectRangeGraphsMessage = "Select between {0} and {1} graphs";

	/**
	 *  Checks to make sure the the boundaries of a floating glyph are
	 *  inside the map view.
	 *  If the glyph is not a floating glyph, this will have no effect on it.
	 *  Assumes that graph glyph is a child of a PixelFloaterGlyph, so that
	 *   the glyph's coord box is also its pixel box.
	 */
	public static void checkPixelBounds(GraphGlyph gl, NeoMap map) {
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

	/**
	 * Identical to above, except for new version of AbstractGraphGlyph. Remove the above
	 * when completely converted to new version of AbstractGraphGlyph.
	 * @param gl the graph glyph to check
	 * @param map the map (AffyTieredMap)
	 */
	public static void checkPixelBounds(AbstractGraphGlyph gl, NeoMap map) {
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

	/**
	 * get the error message text for an attempted graph operation
	 * @param graphCount the number of graph glyphs
	 * @param minCount the minimum graphs for the operator
	 * @param maxCount the maximum graphs for the operator
	 * @return the error message text
	 */
	private static String getOperandMessage(int graphCount, int minCount, int maxCount) {
		if (minCount == maxCount) {
			return MessageFormat.format(selectExactGraphsMessage, minCount);
		}
		if (maxCount == Integer.MAX_VALUE) {
			return MessageFormat.format(selectMinGraphsMessage, minCount);
		}
		return MessageFormat.format(selectRangeGraphsMessage, minCount, maxCount);
	}

	/**
	 * potentially performs a given graph operation on a given set of graphs
	 * @param operator the GraphOperator
	 * @param graph_glyphs the Graph Glyph operands
	 * @param gviewer the SeqMapView
	 * @return GraphSym
	 */
	public static GraphSym doOperateGraphs(GraphOperator operator, List<AbstractGraphGlyph> graph_glyphs) {
		GraphSym newsym = null;
		if (graph_glyphs.size() >= operator.getOperandCountMin() && graph_glyphs.size() <= operator.getOperandCountMax()) {
			newsym = performOperation(graph_glyphs, operator);

			if (newsym != null) {
				BioSeq aseq = newsym.getGraphSeq();
				aseq.addAnnotation(newsym);
			}
		}
		else {
			ErrorHandler.errorPanel("ERROR", getOperandMessage(graph_glyphs.size(), operator.getOperandCountMin(), operator.getOperandCountMax()));
		}
		return newsym;
	}

	/**
	 * performs a given graph operation on a given set of graphs and returns the resulting graph
	 * note - there can be a mix of widthless (no wCoords) and width graphs, if all input graphs
	 * are widthless, the result is also widthless, otherwise all widthless graphs will be treated
	 * as if they have width of 1.
	 * @param graphs the selected graphs to use as the operands of the operation
	 * @param operator the GraphOperator to use
	 * @return the graph result of the operation
	 */
	private static GraphSym performOperation(List<AbstractGraphGlyph> graphs, GraphOperator operator) {
		// get the x, y, and w (width) coordinates of the graphs int Lists
		ArrayList<ArrayList<Integer>> xCoords = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> wCoords = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Float>> yCoords = new ArrayList<ArrayList<Float>>();
		boolean hasWidthGraphs = false;
		int[] index = new int[graphs.size()];
		ArrayList<String> labels = new ArrayList<String>();
		for (int i = 0; i < graphs.size(); i++) {
			AbstractGraphGlyph graph = graphs.get(i);
			index[i] = 0;
			int[] xArray = graph.getXCoords();
			ArrayList<Integer> xCoordList = new ArrayList<Integer>();
			for (int j = 0; j < xArray.length; j++) {
				xCoordList.add(xArray[j]);
			}
			xCoords.add(xCoordList);
			ArrayList<Integer> wCoordList = null;
			int[] wArray = graph.getWCoords();
			if (wArray != null) {
				hasWidthGraphs = true;
				wCoordList = new ArrayList<Integer>();
				for (int j = 0; j < wArray.length; j++) {
					wCoordList.add(wArray[j]);
				}
			}
			wCoords.add(wCoordList);
			float[] yArray = graph.copyYCoords();
			ArrayList<Float> yCoordList = new ArrayList<Float>();
			for (int j = 0; j < yArray.length; j++) {
				yCoordList.add(yArray[j]);
			}
			yCoords.add(yCoordList);
			labels.add(graph.getLabel());
		}
		List<Integer> xList = new ArrayList<Integer>();
		List<Integer> wList = new ArrayList<Integer>();
		List<Float> yList = new ArrayList<Float>();
		// find the minimum x of all graphs to start with
		int spanBeginX = Integer.MAX_VALUE;
		for (int i = 0; i < graphs.size(); i++) {
			spanBeginX = Math.min(spanBeginX, xCoords.get(i).get(0));
		}
		// loop through finding the next x values by searching through all the x coords,
		// and applying the operation on all the graphs
		boolean lastWidth0 = false;
		int spanEndX = 0;
		while (spanBeginX < Integer.MAX_VALUE) {
			// find the next x value, the minimum of all x, x + w that is greater than the current x
			spanEndX = Integer.MAX_VALUE;
			for (int i = 0; i < graphs.size(); i++) {
				int graphIndex = index[i];
				if (graphIndex < xCoords.get(i).size()) {
					int startX = xCoords.get(i).get(graphIndex);
					int endX = startX + getWidth(wCoords.get(i), graphIndex, hasWidthGraphs);
					if (startX == endX && startX < spanEndX) { // widthless (width == 0) coordinate
						spanEndX = startX;
					}
					else if (startX > spanBeginX && startX < spanEndX) {
						spanEndX = startX;
					}
					else if (endX > spanBeginX && endX < spanEndX) {
						spanEndX = endX;
					}
				}
			}
			if (lastWidth0) {
				spanBeginX = spanEndX;
			}
			// now that we have currentX and nextX (the start and end of the span)
			// we get each y coord as an operand
			List<Float> operands = new ArrayList<Float>();
			for (int i = 0; i < graphs.size(); i++) {
				float value = 0;
				int graphIndex = index[i];
				if (graphIndex < xCoords.get(i).size()) {
					int startX = xCoords.get(i).get(graphIndex);
					int endX = startX + getWidth(wCoords.get(i), graphIndex, hasWidthGraphs);
					if (spanBeginX >= startX && spanEndX <= endX) {
						value = yCoords.get(i).get(graphIndex);
					}
				}
				operands.add(value);
			}
			// now we have the operands, actually perform the operation
			float currentY = operator.operate(operands);
			// add the span and result - x, y, w - to the result graph
			xList.add(spanBeginX);
			wList.add(spanEndX - spanBeginX);
			yList.add(currentY);
			// now go through all graphs, and increment the index if necessary
			for (int i = 0; i < graphs.size(); i++) {
				int graphIndex = index[i];
				if (graphIndex < xCoords.get(i).size()) {
					int startX = xCoords.get(i).get(graphIndex);
					int endX = startX + getWidth(wCoords.get(i), graphIndex, hasWidthGraphs);
					if (endX <= spanEndX) {
						index[i]++;
					}
				}
			}
			// we are done for this span, move the end of span to the beginning
			lastWidth0 = spanEndX == spanBeginX;
			spanBeginX = spanEndX;
		}
		// get the display name for the result graph
		String symbol = operator.getSymbol();
		String separator = (symbol == null) ? ", " : " " + symbol + " ";
		String newname = 
			operator.getName().toLowerCase() + ": " + (graphs.size() == 2 ? "(" + graphs.get(0).getLabel() + ")" + separator + "(" + graphs.get(1).getLabel() + ")" :
			"(..." + graphs.size() + ")");
		BioSeq aseq = ((GraphSym) graphs.get(0).getInfo()).getGraphSeq();
		newname = GraphSymUtils.getUniqueGraphID(newname, aseq);
		// create the new graph from the results
		int[] x = intListToArray(xList);
		int[] w = intListToArray(wList);
		float[] y =floatListToArray(yList);
		if (x.length == 0) { // if no data, just create a dummy zero span
			x = new int[]{xCoords.get(0).get(0)};
			y = new float[]{0};
			w = new int[]{1};
		}
//		if (!graphA.hasWidth()) {
//			newsym = new GraphSym(graphA.getXCoords(), newY, newname, aseq);
//		} else {
//			newsym = new GraphIntervalSym(graphA.getXCoords(), graphA.getWCoords(), newY, newname, aseq);
//		}
		GraphSym newsym = new GraphSym(x, w, y, newname, aseq);

		newsym.setGraphName(newname);
		newsym.getGraphState().setGraphStyle(graphs.get(0).getGraphState().getGraphStyle());
		newsym.getGraphState().setHeatMap(graphs.get(0).getGraphState().getHeatMap());
		return newsym;
	}

	private static int getWidth(ArrayList<Integer> widths, int index, boolean hasWidthGraphs) {
		int width = 0;
		if (widths == null) {
			if (hasWidthGraphs) {
				width = 1;
			}
		}
		else {
			width = widths.get(index);
		}
		return width;
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
