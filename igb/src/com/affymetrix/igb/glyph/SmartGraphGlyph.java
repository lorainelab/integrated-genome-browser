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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.*;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;

/*
 * These are replacing private copies which (Smart)GraphGlyph used to keep
 */
import static com.affymetrix.genometryImpl.style.GraphStateI.BAR_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.AVG_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.EXT_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.MAX_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.MIN_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.LINE_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.MINMAXAVG;

/**
 *  A smarter graph glyph.
 *  It:
 * <ol>
 * <li> improves efficiency of drawing zoomed-out large graphs (via max once-per-pixel drawing)
 * <li> improves efficiency of drawing zoomed-in large graphs (via binary search for draw end-points)
 * <li> allows for summarizing min, max, and average separately for zoomed-out graphs
 * <li> allows for on-the-fly thresholding and "glyphification" of regions that pass threshold
 * </ol>
 *
 *  Also, now trying to further improve efficiency for zoomed-out large graphs via data structure
 *      enhancements.
 *  And, working on drawing y-axis....
 *
 *  ONLY MEANT FOR GRAPHS ON HORIZONTAL MAPS
 */
public final class SmartGraphGlyph extends GraphGlyph {

	public boolean NEWDEBUG = false;
	public boolean THRESH_DEBUG = false;
	public static final double default_transition_scale = 100;
	/*
	 *  The x scale at which to transition from MinMaxAvg drawing to Bar drawing
	 *   (if graph_style is set to MINMAXAVG)
	 *   specified in coords_per_pixel
	 *   (note that view's transform scale is expressed differently, in pixels_per_coord...)
	 */
	double transition_scale = default_transition_scale;  // specified in coords_per_pixel
	boolean TRANSITION_TO_BARS = true;
	boolean SHOW_CACHE_INDICATOR = false;
	boolean CACHE_DIRECT_DRAW = false;
	boolean AVGLINE = true;
	boolean MINMAXBAR = true;
	int thresh_contig_height = 10;  // in pixels, for calculating where to draw thresholded regions
	int thresh_contig_yoffset = 2;  // in pixels, for calculating where to draw thresholded regions
	Rectangle2D.Double thresh_coord_box = new Rectangle2D.Double();  // for calculating where to draw thresholded regions
	Rectangle thresh_pix_box = new Rectangle();  // for calculating where to draw thresholded regions
	Color thresh_color;
	// still need to make thresh_glyph draw as a fixed-pixel (1 or 2) line instead of as a variable-pixel fillRect...
	ThreshGlyph thresh_glyph = new ThreshGlyph();
	int compression_level = 20;  // average # of points per entry in flat graph compression cache

	/*
	 *  may need to try a new approach to minimize switching graphics color
	 *  (testing is showing that graph draws ~5x _slower_ if have to alternate colors with
	 *    every graphics draw call...)
	 *  Therefore want to separate out for example the minmax bar drawing from the average line
	 *     drawing since they are different colors.  Could do this with two passes, but then
	 *     redoing a lot of summing, etc.
	 *    As an alternative, may try caching the average position for each pixel
	 *       (as doubles in graph coords, or maybe in pixel positions)
	 *
	 */
	int pixel_cache[];

	public SmartGraphGlyph(GraphSym graf, GraphStateI gstate) {
		super(graf, gstate);
		setDrawOrder(Glyph.DRAW_SELF_FIRST);

		thresh_glyph.setVisibility(getShowThreshold());
		thresh_glyph.setSelectable(false);
		if (thresh_color != null) {
			thresh_glyph.setColor(thresh_color);
		}
		this.addChild(thresh_glyph);

		if (xcoords == null || graf == null || xcoords.length <= 0 || graf.getPointCount() <= 0) {
			return;
		}

		if (Float.isInfinite(getMinScoreThreshold()) && Float.isInfinite(getMaxScoreThreshold())) {
			setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY()) / 2));
		}
		resetThreshLabel();
	}

	@Override
	public void draw(ViewI view) {
		if (NEWDEBUG) {
			System.out.println("called SmartGraphGlyph.draw(), coords = " + coordbox);
		}
		int graph_style = getGraphStyle();
		//    System.out.println("thresh glyph visibility: " + thresh_glyph.isVisible() + ", show threshold = " + show_threshold);

		// GAH 9-13-2002
		// hack to get thresholding to work -- thresh line child glyph keeps getting removed
		//   as a child of graph... (must be something in SeqMapView.setAnnotatedSeq()...
		if (this.getChildCount() == 0) {
			if (thresh_glyph == null) {
				thresh_glyph = new ThreshGlyph();
				thresh_glyph.setSelectable(false);
				thresh_glyph.setColor(thresh_color);
			}
			this.addChild(thresh_glyph);
		}

		if (graph_style == MINMAXAVG || graph_style == LINE_GRAPH) {
			double xpixels_per_coord = ((LinearTransform) view.getTransform()).getScaleX();
			double xcoords_per_pixel = 1 / xpixels_per_coord;
			if (TRANSITION_TO_BARS && (xcoords_per_pixel < transition_scale)) {
				// if at resolution where bars should be displayed, then draw as LINE or BAR graph
				if ((graph_style == MINMAXAVG) &&
						(Application.getSingleton() instanceof IGB)) {
					super.draw(view, BAR_GRAPH);
				} else {
					super.draw(view, LINE_GRAPH);
				}
			} else {
				drawSmart(view);
			}

		} else if (graph_style == MIN_HEAT_MAP || graph_style == MAX_HEAT_MAP || graph_style == AVG_HEAT_MAP || graph_style == EXT_HEAT_MAP) {

			drawSmart(view);

		} else {
			// Not one of the special styles, so default to regular GraphGlyph.draw method.
			super.draw(view);
		}

		if (getShowThreshold()) {
			drawThresholdedRegions(view);
		} else {
			thresh_glyph.setVisibility(false);
		}
	}

	/** Sets the scale at which the drawing routine will switch between the
	 *  style that is optimized for large genomic regions and the normal style.
	 */
	public void setTransitionScale(double d) {
		transition_scale = d;
	}

	private void drawSmart(ViewI view) {
		// could size cache to just the view's pixelbox, but then may end up creating a
		//   new int array every time the pixelbox changes (which with view damage or
		//   scrolling optimizations turned on could be often)
		int comp_ysize = ((View) view).getComponentSize().width;
		// could check for exact match with comp_ysize, but allowing larger comp size here
		//    may be good for multiple maps that share the same scene, so that new int array
		//    isn't created every time paint switches from mapA to mapB -- the array will
		//    be reused and be the length of the component with greatest width...
		if ((pixel_cache == null) || (pixel_cache.length < comp_ysize)) {
			//      System.out.println("in SmartGraphGlyph, creating new pixel cache");
			pixel_cache = new int[comp_ysize];
		}
		for (int i = 0; i < comp_ysize; i++) {
			pixel_cache[i] = Integer.MIN_VALUE;
		}
		if (TIME_DRAWING) {
			tim.start();
		}
		view.transformToPixels(coordbox, pixelbox);

		if (getShowGrid() && !GraphState.isHeatMapStyle(getGraphStyle())) {
			drawHorizontalGridLines(view);
		}
		if (getShowGraph()) {
			drawGraph(view);
		}
		if (getShowHandle()) {
			drawHandle(view);
		}
		if (getShowAxis()) {
			drawAxisLabel(view);
		}
		// drawing outline around bounding box
		if (getShowBounds()) {
			Graphics g = view.getGraphics();
			g.setColor(Color.green);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width - 1, pixelbox.height - 1);
		}
		if (getShowLabel()) {
			drawLabel(view);
		}
		if (TIME_DRAWING) {
			long drawtime = tim.read();
			System.out.println("smart graph draw time: " + drawtime);
		}
	}
	/** A variable used to hold some temporary calculations for LINE_GRAPH. */
	Point last_point_temp = new Point(0, 0);

	public void drawGraph(ViewI view) {
		if (xcoords.length <= 0) {
			return; // there is nothing to draw!
		}
		int graph_style = getGraphStyle();
		view.transformToPixels(coordbox, pixelbox);

		Graphics g = view.getGraphics();
		double coords_per_pixel = 1.0f / ((LinearTransform) view.getTransform()).getScaleX();

		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getOffsetY();

		Rectangle2D.Double view_coordbox = view.getCoordBox();
		double xmin = view_coordbox.x;
		double xmax = view_coordbox.x + view_coordbox.width;

		// plot_top_ypixel and plot_bottom_ypixel are replacements for pixelbox.y and pbox_yheight in many
		//   (but not all) calculations, they take into account an internal transform to shrink the graph rendering
		//   if necessary to allow space for the graph label and thresholded regions
		// plot_top_ypixel is "top" y pixel position allocated to plot rendering
		// plot_bottom_ypixel is "bottom" y pixel position allocated to plot rendering
		// since y pixel addressing in Graphics is numbered in increasing order from top,
		//         plot_top_ypixel < plot_bottom_ypixel
		//    this is a little confusing because it means as graph values decrease, pixel position increases
		//    a better way to think of this is:
		//        plot_top_ypixel = pixel position of graph.getVisibleMaxY()
		//        plot_bottom_ypixel = pixel position of graph.getVisibleMinY();
		coord.y = offset - ((getVisibleMaxY() - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, scratch_point);
		int plot_top_ypixel = scratch_point.y;  // replaces pixelbox.y

		coord.y = offset; // visible min, since = offset - ((getVisibleMinY() - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, scratch_point);
		int plot_bottom_ypixel = scratch_point.y; // replaces pbox_yheight

		Color[] heatmap_colors = null;
		double heatmap_scaling = 1;
		if (state.getHeatMap() != null) {
			heatmap_colors = state.getHeatMap().getColors();
			// scale based on pixel position, not cooord position, since most calculations below are in pixels
			heatmap_scaling = (double) (heatmap_colors.length - 1) / (-plot_top_ypixel + plot_bottom_ypixel);
		}

		float yzero = 0;
		if (getVisibleMinY() > yzero) {
			yzero = getVisibleMinY();
		} else if (getVisibleMaxY() < yzero) {
			yzero = getVisibleMaxY();
		}
		coord.y = offset - ((yzero - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, zero_point);

		if (graph_style == MINMAXAVG || graph_style == LINE_GRAPH) {
			if (show_min_max) {
				g.setColor(Color.yellow);
				g.drawLine(pixelbox.x, plot_bottom_ypixel, pixelbox.width, plot_bottom_ypixel);
				g.setColor(Color.blue);
				g.drawLine(pixelbox.x, plot_top_ypixel, pixelbox.width, plot_top_ypixel);
			}

			if (getGraphState().getShowZeroLine() && yzero == 0) {
				g.setColor(Color.gray);
				g.drawLine(pixelbox.x, zero_point.y, pixelbox.width, zero_point.y);
			}
			if (graph_style == MINMAXAVG) {
				g.setColor(darker);
			} else {
				g.setColor(getBackgroundColor());
			}
		}


		DrawPoints(xmin, xmax, offset, yscale, view, graph_style, g, plot_bottom_ypixel, plot_top_ypixel, heatmap_scaling);


		if (AVGLINE) {
			DrawAvgLine(graph_style, g, heatmap_scaling, plot_bottom_ypixel, plot_top_ypixel, coords_per_pixel);
		}
	}

	private void DrawPoints(double xmin, double xmax, double offset, double yscale, ViewI view, int graph_style, Graphics g, int plot_bottom_ypixel, int plot_top_ypixel, double heatmap_scaling) {
		// not using graph cache...
		// using binary search to find end points --
		//    assumes xcoords array is ordered by increasing value
		int draw_beg_index = Arrays.binarySearch(xcoords, (int) xmin);

		// The +1 on draw_end_index might be an error, but it gets corrected below
		int draw_end_index = Arrays.binarySearch(xcoords, (int) xmax) + 1;

		if (draw_beg_index < 0) {
			// want draw_beg_index to be index of max xcoord <= view_start
			//  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
			draw_beg_index = (-draw_beg_index - 1) - 1;
			if (draw_beg_index < 0) {
				draw_beg_index = 0;
			}
		}
		if (draw_end_index < 0) {
			// want draw_end_index to be index of min xcoord >= view_end
			//   (insertion point)  [as defined in Arrays.binarySearch() docs]
			draw_end_index = -draw_end_index - 1;
			if (draw_end_index < 0) {
				draw_end_index = 0;
			} else if (draw_end_index >= xcoords.length) {
				draw_end_index = xcoords.length - 1;
			}
			if (draw_end_index < (xcoords.length - 1)) {
				draw_end_index++;
			}
		}

		// draw_end_index is sometimes too large (by 1)
		if (draw_end_index >= xcoords.length) {
			draw_end_index = xcoords.length - 1;
		}

		coord.x = xcoords[draw_beg_index];
		//      coord.y = offset - (ycoords[draw_beg_index] * yscale);
		coord.y = offset - ((graf.getGraphYCoord(draw_beg_index) - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, prev_point);

		int ymin_pixel = prev_point.y;
		int ymax_pixel = prev_point.y;
		int yavg_pixel;
		int ysum = prev_point.y;
		int points_in_pixel = 1;
		int draw_count = 0;

		if (graph_style == MINMAXAVG) {
			g.setColor(darker);
		}
		if (graph_style == LINE_GRAPH) {
			g.setColor(getBackgroundColor());
		}
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			coord.x = xcoords[i];
			coord.y = offset - ((graf.getGraphYCoord(i) - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, curr_point);
			if (prev_point.x == curr_point.x) {
				// collect ymin, ymax, y_average for all coord points that transform to
				//    the same x pixel
				ymin_pixel = Math.min(ymin_pixel, curr_point.y);
				ymax_pixel = Math.max(ymax_pixel, curr_point.y);
				ysum += curr_point.y;
				points_in_pixel++;
			} else {
				// draw previous pixel position
				if ((graph_style == MINMAXAVG && MINMAXBAR) || graph_style == LINE_GRAPH || graph_style == MIN_HEAT_MAP || graph_style == MAX_HEAT_MAP || graph_style == EXT_HEAT_MAP) {
					// Does not apply to AVG_HEAT_MAP
					int ystart = Math.max(Math.min(ymin_pixel, plot_bottom_ypixel), plot_top_ypixel);
					int yend = Math.min(Math.max(ymax_pixel, plot_top_ypixel), plot_bottom_ypixel);
					int yheight = yend - ystart;

					if (graph_style == MINMAXAVG || graph_style == LINE_GRAPH) {
						g.fillRect(prev_point.x, ystart, 1, yheight);
					} else if (graph_style == MIN_HEAT_MAP) {
						g.setColor(state.getHeatMap().getColor((int) (heatmap_scaling * (plot_bottom_ypixel - yend))));
						g.fillRect(prev_point.x, plot_top_ypixel, 3, plot_bottom_ypixel - plot_top_ypixel);
					} else if (graph_style == MAX_HEAT_MAP) {
						g.setColor(state.getHeatMap().getColor((int) (heatmap_scaling * (plot_bottom_ypixel - ystart))));
						g.fillRect(prev_point.x, plot_top_ypixel, 3, plot_bottom_ypixel - plot_top_ypixel);
					} else if (graph_style == EXT_HEAT_MAP) {
						int max = (int) (heatmap_scaling * (plot_bottom_ypixel - ystart));
						int min = (int) (heatmap_scaling * (plot_bottom_ypixel - yend));

						if (Math.abs(max - 127) > Math.abs(min - 127)) {
							// Pick the most extreme value out of max and min
							g.setColor(state.getHeatMap().getColor(max));
						} else {
							g.setColor(state.getHeatMap().getColor(min));
						}
						g.fillRect(prev_point.x, plot_top_ypixel, 3, plot_bottom_ypixel - plot_top_ypixel);
					}
					draw_count++;
				}
				yavg_pixel = ysum / points_in_pixel;
				if (AVGLINE || graph_style == LINE_GRAPH) {
					// cache for drawing later
					if (prev_point.x > 0 && prev_point.x < pixel_cache.length) {
						pixel_cache[prev_point.x] = Math.min(Math.max(yavg_pixel, plot_top_ypixel), plot_bottom_ypixel);
					}
				}

				if (graph_style == LINE_GRAPH && i > 0 && i <= graf.getPointCount()) {
					coord.x = xcoords[i - 1];
					coord.y = offset - ((graf.getGraphYCoord(i - 1) - getVisibleMinY()) * yscale);
					view.transformToPixels(coord, last_point_temp);

					int y1 = Math.min(Math.max(last_point_temp.y, plot_top_ypixel), plot_bottom_ypixel);
					int y2 = Math.min(Math.max(curr_point.y, plot_top_ypixel), plot_bottom_ypixel);
					g.drawLine(prev_point.x, y1, curr_point.x, y2);
				}

				ymin_pixel = curr_point.y;
				ymax_pixel = curr_point.y;
				ysum = curr_point.y;
				points_in_pixel = 1;
			}
			prev_point.x = curr_point.x; // this line is sometimes redundant
			prev_point.y = curr_point.y; // this line is not redundant
		}
		// can only show threshold if xy coords are also being shown (show_graph = true)
	}

	private void DrawAvgLine(int graph_style, Graphics g, double heatmap_scaling, int plot_bottom_ypixel, int plot_top_ypixel, double coords_per_pixel) {
		if (graph_style == MINMAXAVG) {
			g.setColor(lighter);
		}
		int prev_index = 0;
		// find the first pixel position that has a real value in pixel_cache
		while ((prev_index < pixel_cache.length) && (pixel_cache[prev_index] == Integer.MIN_VALUE)) {
			prev_index++;
		}
		if (prev_index != pixel_cache.length) {
			// successfully found a real value in pixel cache
			int yval;
			for (int i = prev_index + 1; i < pixel_cache.length; i++) {
				yval = pixel_cache[i];
				if (graph_style == AVG_HEAT_MAP) {
					if (yval != Integer.MIN_VALUE) {
						g.setColor(state.getHeatMap().getColor((int) (heatmap_scaling * (plot_bottom_ypixel - yval))));
						g.fillRect(i, plot_top_ypixel, 3, plot_bottom_ypixel - plot_top_ypixel);
						prev_index = i;
					}
				}
				if (graph_style == MINMAXAVG) {
					if (yval != Integer.MIN_VALUE) {
						if (pixel_cache[i - 1] == Integer.MIN_VALUE && coords_per_pixel > 30) {
							// last pixel had no datapoints, so just draw a point at current pixel
							g.drawLine(i, yval, i, yval);
						} else {
							// last pixel had at least one datapoint, so connect with line
							g.drawLine(prev_index, pixel_cache[prev_index], i, yval);
						}
						prev_index = i;
					}
				}
			}
		}
	}

	/**
	 *  Retrieve the graph yvalue corresponding to a given ycoord.
	 */
	float getGraphValue(ViewI view, double coord_value) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getOffsetY();

		float graph_value = (float) ((offset - coord_value) / yscale) + getVisibleMinY();
		return graph_value;
	}

	/**
	 *  Retrieve the map y coord corresponding to a given graph yvalue.
	 */
	private double getCoordValue(ViewI view, float graph_value) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getOffsetY();

		float coord_value = (float) (offset - ((graph_value - getVisibleMinY()) * yscale));
		return coord_value;
	}

	private void drawThresholdedRegions(ViewI view) {
		drawThresholdedRegions(view, null, null);
	}

	/**
	 *  Draws thresholded regions.
	 *  Current set up so that if regions_parent != null, then instead of drawing to view,
	 *   populate regions_parent with child SeqSymmetries for each region that passes threshold,
	 *
	 */
	void drawThresholdedRegions(ViewI view, MutableSeqSymmetry region_holder, MutableAnnotatedBioSeq aseq) {
		/*
		 *  Should really eventually move the SeqSymmetry stuff out of this class, maybe have
		 *     drawThresholdedRegions() populate regions that pass threshold as two IntLists or
		 *     something...
		 */
		double max_gap_threshold = getMaxGapThreshold();
		double min_run_threshold = getMinRunThreshold();
		double span_start_shift = getThreshStartShift();
		double span_end_shift = getThreshEndShift();

		int thresh_direction = getThresholdDirection();
		float min_score_threshold = Float.NEGATIVE_INFINITY;
		float max_score_threshold = Float.POSITIVE_INFINITY;
		if (thresh_direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
			min_score_threshold = getMinScoreThreshold();
			max_score_threshold = Float.POSITIVE_INFINITY;
		} else if (thresh_direction == GraphState.THRESHOLD_DIRECTION_LESS) {
			min_score_threshold = Float.NEGATIVE_INFINITY;
			max_score_threshold = getMaxScoreThreshold();
		} else if (thresh_direction == GraphState.THRESHOLD_DIRECTION_BETWEEN) {
			min_score_threshold = getMinScoreThreshold();
			max_score_threshold = getMaxScoreThreshold();
		}

		// if neither min or max score thresholds have been set, assume that only using
		//     min score threshold and set so it is in the middle of visible score range
		if (Float.isInfinite(min_score_threshold) && Float.isInfinite(max_score_threshold)) {
			setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY()) / 2));
			min_score_threshold = getMinScoreThreshold();
			max_score_threshold = Float.POSITIVE_INFINITY;
		}

		Rectangle2D.Double view_coordbox = view.getCoordBox();
		double xmin = view_coordbox.x;
		double xmax = view_coordbox.x + view_coordbox.width;
		int draw_beg_index = 0;
		int draw_end_index;
		boolean make_syms = ((region_holder != null) && (aseq != null));
		int sym_count = 0;
		int draw_count = 0;
		if (make_syms) {
			// if writing to region_holder, then want to do _whole_ graph, not just
			//   what's in current view
			draw_end_index = xcoords.length - 1;
		} else {
			draw_beg_index = Arrays.binarySearch(xcoords, (int) xmin);
			draw_end_index = Arrays.binarySearch(xcoords, (int) xmax) + 1;
			if (draw_beg_index < 0) {
				// want draw_beg_index to be index of max xcoord <= view_start
				//  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
				draw_beg_index = (-draw_beg_index - 1) - 1;
				if (draw_beg_index < 0) {
					draw_beg_index = 0;
				}
			}
			if (draw_end_index < 0) {
				// want draw_end_index to be index of min xcoord >= view_end
				//   (insertion point)  [as defined in Arrays.binarySearch() docs]
				draw_end_index = -draw_end_index - 1;
				if (draw_end_index < 0) {
					draw_end_index = 0;
				} else if (draw_end_index >= xcoords.length) {
					draw_end_index = xcoords.length - 1;
				}
				if (draw_end_index < (xcoords.length - 1)) {
					draw_end_index++;
				}
			}
		}


		double thresh_ycoord;
		double thresh_score;
		if (!Float.isInfinite(min_score_threshold)) {
			thresh_score = min_score_threshold;
		} else if (!Float.isInfinite(max_score_threshold)) {
			thresh_score = max_score_threshold;
		} else {
			System.out.println("in SmartGraphGlyph.drawThresholdedRegions(), problem with setting up threshold line!");
			thresh_score = (getVisibleMinY() + (getVisibleMaxY() / 2));
		}
		if (thresh_score < getVisibleMinY() ||
				thresh_score > getVisibleMaxY()) {
			thresh_glyph.setVisibility(false);
		} else {
			thresh_glyph.setVisibility(true);
		}

		thresh_ycoord = getCoordValue(view, (float) thresh_score);
		thresh_glyph.setCoords(coordbox.x, thresh_ycoord, coordbox.width, 1);

		Graphics g = view.getGraphics();
		g.setColor(lighter);
		double x, w, y;
		double pass_thresh_start = 0;
		double pass_thresh_end = 0;

		boolean pass_threshold_mode = false;
		int min_index = 0;
		int max_index = xcoords.length - 1;

		// need to widen range searched to include previous and next points out of view that
		//   pass threshold (unless distance to view is > max_gap_threshold
		int new_beg = draw_beg_index;

		// GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
		while ((new_beg > min_index) &&
				// ((xcoords[draw_beg_index] - xcoords[new_beg]) <= max_gap_threshold)) {
				((xcoords[draw_beg_index] - xcoords[new_beg]) <= max_gap_threshold)) {
			new_beg--;
		}
		draw_beg_index = new_beg;

		int new_end = draw_end_index;
		boolean pass_score_thresh;
		boolean passes_max_gap;
		boolean draw_previous = false;

		// GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
		while ((new_end < max_index) && // end_index is really the maximum allowed draw_end_index
				((xcoords[new_end] - xcoords[draw_end_index]) <= max_gap_threshold)) {
			new_end++;
		}
		draw_end_index = new_end;

		if (draw_end_index >= xcoords.length) {
			draw_end_index = xcoords.length - 1;
		}


		// eight possible states:
		//
		//     pass_threshold_mode    [y >= min_score_threshold]   [x-pass_thresh_end <= max_dis_thresh]
		//
		//  prune previous region and draw when:
		//      true, false, false
		//      true, true, false
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			x = xcoords[i];
			w = 0;
			if (wcoords != null) {
				w = wcoords[i];
			}
			y = graf.getGraphYCoord(i);

			// GAH 2006-02-16 changed to > min_score instead of >= min_score, to better mirror Affy tiling array pipeline
			pass_score_thresh = ((y > min_score_threshold) &&
					(y <= max_score_threshold));
			passes_max_gap = ((x - pass_thresh_end) <= max_gap_threshold);
			if (pass_threshold_mode) {  // if currently keeping track of potential passed-threshold region
				if (!passes_max_gap) {
					draw_previous = true;
				} else if (pass_score_thresh) {
					// this point passes threshold test
					//  and within max distance
					// passes threshold test, within max distance, keep extending region
					pass_thresh_end = x + w;
				}
			} else {
				if (pass_score_thresh) {
					// switch into pass_threshold_mode
					// don't need to worry about distance thresh here
					pass_thresh_start = x;
					pass_thresh_end = x + w;
					pass_threshold_mode = true;
				}
			}

			if (draw_previous) {
				double draw_min = pass_thresh_start + span_start_shift;
				double draw_max = pass_thresh_end + span_end_shift;

				// make sure that length of region is > min_run_threshold
				// GAH 2006-02-16 changed to > min_run instead of >=, to better mirror Affy tiling array pipeline
				boolean passes_min_run = ((draw_max - draw_min) > min_run_threshold);
				if (passes_min_run) {  // make sure aren't drawing single points
					coord.x = draw_min;
					view.transformToPixels(coord, prev_point);
					coord.x = draw_max;
					view.transformToPixels(coord, curr_point);
					if (make_syms) {
						SeqSymmetry sym =
								new SingletonSeqSymmetry((int) draw_min, (int) draw_max, aseq);
						region_holder.addChild(sym);
						sym_count++;
					} else {
						draw_count++;
						g.fillRect(prev_point.x,
								pixelbox.y + pixelbox.height - thresh_contig_height,
								curr_point.x - prev_point.x + 1,
								thresh_contig_height);
					}
				}
				draw_previous = false;
				pass_threshold_mode = pass_score_thresh;
				if (pass_score_thresh) {  // current point passes threshold test, start new region scan
					pass_thresh_start = x;
					pass_thresh_end = x + w;
				}
			}
		}

		// clean up by doing a draw if exited loop while still in pass_threshold_mode
		if (pass_threshold_mode && (pass_thresh_end != pass_thresh_start)) {
			double draw_min = pass_thresh_start + span_start_shift;
			double draw_max = pass_thresh_end + span_end_shift;
			// GAH 2006-02-16 changed to > min_run instead of >=, to better mirror Affy tiling array pipeline
			boolean passes_min_run = ((draw_max - draw_min) > min_run_threshold);
			if (passes_min_run) {
				coord.x = draw_min;
				view.transformToPixels(coord, prev_point);
				coord.x = draw_max;
				view.transformToPixels(coord, curr_point);
				if (make_syms) {
					SeqSymmetry sym =
							new SingletonSeqSymmetry((int) pass_thresh_start, (int) pass_thresh_end, aseq);
					region_holder.addChild(sym);
				} else {
					g.fillRect(prev_point.x,
							pixelbox.y + pixelbox.height - thresh_contig_height,
							curr_point.x - prev_point.x + 1,
							thresh_contig_height);
				}
			}

		}
	}

	@Override
	public void setBackgroundColor(Color col) {
		super.setBackgroundColor(col);
		thresh_color = darker.darker();
		if (thresh_glyph != null) {
			thresh_glyph.setColor(thresh_color);
		}
	}

	public void setShowThreshold(boolean show) {
		state.setShowThreshold(show);
		thresh_glyph.setVisibility(show);
	}

	public boolean getShowThreshold() {
		return state.getShowThreshold();
	}

	private void resetThreshLabel() {
		float min_thresh = getMinScoreThreshold();
		float max_thresh = getMaxScoreThreshold();
		int direction = state.getThresholdDirection();
		if (direction == GraphState.THRESHOLD_DIRECTION_BETWEEN) {
			thresh_glyph.setLabel(nformat.format(min_thresh) + " -- " + nformat.format(max_thresh));
		} else if (direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
			thresh_glyph.setLabel(">= " + nformat.format(min_thresh));
		} else if (direction == GraphState.THRESHOLD_DIRECTION_LESS) {
			thresh_glyph.setLabel("<= " + nformat.format(max_thresh));
		}
	}

	void setThresholdDirection(int d) {
		state.setThresholdDirection(d);
		resetThreshLabel();
	}

	void setMinScoreThreshold(float thresh) {
		state.setMinScoreThreshold(thresh);
		resetThreshLabel();
	}

	void setMaxScoreThreshold(float thresh) {
		state.setMaxScoreThreshold(thresh);
		resetThreshLabel();
	}

	public void setMaxGapThreshold(int thresh) {
		state.setMaxGapThreshold(thresh);
	}

	public void setMinRunThreshold(int thresh) {
		state.setMinRunThreshold(thresh);
	}

	public void setThreshStartShift(double d) {
		state.setThreshStartShift(d);
	}

	public void setThreshEndShift(double d) {
		state.setThreshEndShift(d);
	}

	public int getThresholdDirection() {
		return state.getThresholdDirection();
	}

	public float getMinScoreThreshold() {
		return state.getMinScoreThreshold();
	}

	public float getMaxScoreThreshold() {
		return state.getMaxScoreThreshold();
	}

	public double getMaxGapThreshold() {
		return state.getMaxGapThreshold();
	}

	public double getMinRunThreshold() {
		return state.getMinRunThreshold();
	}

	public double getThreshStartShift() {
		return state.getThreshStartShift();
	}

	public double getThreshEndShift() {
		return state.getThreshEndShift();
	}

	/**
	 *  Same as GraphGlyph.getInternalLinearTransform(), except
	 *  also caclulates a bottom y offset for showing thresholded
	 *  regions, if showThresholdedRegions() == true.
	 */
	@Override
	protected double getLowerYCoordInset(ViewI view) {
		double bottom_ycoord_inset = super.getLowerYCoordInset(view);
		if (getShowThreshold()) {
			thresh_pix_box.height = thresh_contig_height + thresh_contig_yoffset;
			view.transformToCoords(thresh_pix_box, thresh_coord_box);
			bottom_ycoord_inset += thresh_coord_box.height;
		}
		return bottom_ycoord_inset;
	}
}
