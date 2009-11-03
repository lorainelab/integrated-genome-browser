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

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.awt.geom.Point2D;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.util.Timer;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

/*
 * These are replacing private copies which GraphGlyph used to keep
 */
import static com.affymetrix.genometryImpl.style.GraphStateI.BAR_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.DOT_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.BIG_DOT_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.LINE_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.STAIRSTEP_GRAPH;

import static com.affymetrix.genometryImpl.style.GraphStateI.AVG_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.EXT_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.MAX_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.MIN_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.MINMAXAVG;

/**
 *  An implementation of graphs for NeoMaps, capable of rendering graphs in a variety of styles
 *  Started with {@link com.affymetrix.genoviz.glyph.BasicGraphGlyph} and improved from there.
 *  ONLY MEANT FOR GRAPHS ON HORIZONTAL MAPS.
 */
public final class GraphGlyph extends Glyph {
	static final double default_transition_scale = 100;

	private static final boolean TIME_DRAWING = false;

	private static Font default_font = new Font("Courier", Font.PLAIN, 12);
	private static final Font axis_font = new Font("SansSerif", Font.PLAIN, 12);
	private static final NumberFormat nformat = new DecimalFormat();
	private int xpix_offset = 0;
	private final Point zero_point = new Point(0, 0);
	private final Point2D.Double coord = new Point2D.Double(0, 0);
	private final Point curr_point = new Point(0, 0);
	private final Point prev_point = new Point(0, 0);
	private final Timer tim = new Timer();
	/**
	 *  point_max_ycoord is the max ycoord (in graph coords) of all points in graph.
	 *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
	 *     be modified (except for resetting the points by calling setPointCoords() again)
	 */
	private float point_max_ycoord = Float.POSITIVE_INFINITY;
	private float point_min_ycoord = Float.NEGATIVE_INFINITY;
	// assumes sorted points, each x corresponding to y
	private final GraphSym graf;
	public static final int handle_width = 10;  // width of handle in pixels
	private static final int pointer_width = 10;
	private final Rectangle handle_pixbox = new Rectangle(); // caching rect for handle pixel bounds
	private final Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection
	private final GraphStateI state;
	private final LinearTransform scratch_trans = new LinearTransform();
	private static final boolean DEBUG = false;
	// specified in coords_per_pixel
	/**
	 * A variable used to hold some temporary calculations for LINE_GRAPH.
	 */
	private final Point last_point_temp = new Point(0, 0);
	// average # of points per entry in flat graph compression cache
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
	private int[] pixel_cache;
	private Color thresh_color;
	private static final int thresh_contig_height = 10;
	// in pixels, for calculating where to draw thresholded regions
	private static final int thresh_contig_yoffset = 2;
	private final Rectangle2D.Double thresh_coord_box = new Rectangle2D.Double();
	private ThreshGlyph thresh_glyph = new ThreshGlyph();
	private final Rectangle thresh_pix_box = new Rectangle();
	private double transition_scale = GraphGlyph.default_transition_scale;


	private Color lighter;
	private Color darker;

	public float getXCoord(int i) {
		return graf.getGraphXCoord(i);
	}

	public float getYCoord(int i) {
		return graf.getGraphYCoord(i);
	}

	private float getWCoord(int i) {
		return ((GraphIntervalSym) graf).getGraphWidthCoord(i);
	}

	public int[] getWCoords() {
		return ((GraphIntervalSym) graf).getGraphWidthCoords();
	}

	/**
	 * Temporary helper method.
	 * @return
	 */
	public float[] copyYCoords() {
		return graf.copyGraphYCoords();
	}

	public GraphGlyph(GraphSym graf, GraphStateI gstate) {
		super();
		state = gstate;
		if (state == null) {
			throw new NullPointerException();
		}

		setCoords(coordbox.x, state.getTierStyle().getY(), coordbox.width, state.getTierStyle().getHeight());

		Map map = graf.getProperties();
		boolean toInitialize = isUninitialized();
		if (toInitialize && map != null) {
			Object value = map.get(ViewPropertyNames.INITIAL_COLOR);
			if (value != null) {
				setColor(Color.decode(value.toString()));
			} else {
				setColor(state.getTierStyle().getColor());
			}
			value = map.get(ViewPropertyNames.INITIAL_BACKGROUND);
			if (value != null) {
				setBackgroundColor(Color.decode(value.toString()));
			} else {
				setBackgroundColor(state.getTierStyle().getBackground());
			}
			value = map.get(ViewPropertyNames.INITIAL_GRAPH_STYLE);
			if (value != null) {
				setGraphStyle(GraphType.fromString(value.toString()));
			} else {
				setGraphStyle(state.getGraphStyle());
			}
		} else {
			setGraphStyle(state.getGraphStyle());
			setColor(state.getTierStyle().getColor());
		}
		//must call again to get it to properly render
		setColor(state.getTierStyle().getColor());

		this.graf = graf;

		if (graf.getPointCount() == 0) {
			return;
		}

		boolean rangeInit = false;
		if (toInitialize && map != null) {
			Object value = map.get(ViewPropertyNames.INITIAL_MAX_Y);
			if (value != null) {
				point_max_ycoord = Float.parseFloat(value.toString());
				rangeInit = true;
			}
			value = map.get(ViewPropertyNames.INITIAL_MIN_Y);
			if (value != null) {
				point_min_ycoord = Float.parseFloat(value.toString());
				rangeInit = true;
			}
		}
		if (!rangeInit) {
			float[] range = getVisibleYRange();
			if (point_max_ycoord == Float.POSITIVE_INFINITY) {
				point_max_ycoord = range[1];
			}
			if (point_min_ycoord == Float.NEGATIVE_INFINITY) {
				point_min_ycoord = range[0];
			}
		}

		if (point_max_ycoord <= point_min_ycoord) {
			point_min_ycoord = point_max_ycoord - 1;
		}
		checkVisibleBoundsY();

		/* Code below comes from old SmartGraphGlyph Constructor */
		setDrawOrder(Glyph.DRAW_SELF_FIRST);

		thresh_glyph.setVisibility(getShowThreshold());
		thresh_glyph.setSelectable(false);
		if (thresh_color != null) {
			thresh_glyph.setColor(thresh_color);
		}
		this.addChild(thresh_glyph);

		if (this.getPointCount() == 0) {
			return;
		}

		if (Float.isInfinite(getMinScoreThreshold()) && Float.isInfinite(getMaxScoreThreshold())) {
			setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY()) / 2));
		}
		resetThreshLabel();
	}

	public boolean hasWidth() {
		return GraphSymUtils.hasWidth(graf);
	}

	private float[] getVisibleYRange() {
		float[] result = new float[2];
		float min_ycoord = Float.POSITIVE_INFINITY;
		float max_ycoord = Float.NEGATIVE_INFINITY;
		float f;

		// evaluate outside of loop conditional -- saves time when there are a lot of points.
		int pointCount = graf.getPointCount();

		for (int i = 0; i < pointCount; i++) {
			f = graf.getGraphYCoord(i);
			if (f < min_ycoord) {
				min_ycoord = f;
			}
			if (f > max_ycoord) {
				max_ycoord = f;
			}
		}
		result[0] = min_ycoord;
		result[1] = max_ycoord;
		return result;
	}

	private boolean isUninitialized() {
		return getVisibleMinY() == Float.POSITIVE_INFINITY ||
				getVisibleMinY() == Float.NEGATIVE_INFINITY ||
				getVisibleMaxY() == Float.POSITIVE_INFINITY ||
				getVisibleMaxY() == Float.NEGATIVE_INFINITY;
	}

	private void checkVisibleBoundsY() {
		if (isUninitialized()) {
			setVisibleMaxY(point_max_ycoord);
			setVisibleMinY(point_min_ycoord);
		}
	}

	private String getID() {
		Object mod = this.getInfo();
		String ident = null;
		if (mod instanceof SeqSymmetry) {
			ident = ((SeqSymmetry) mod).getID();
		}
		if (ident == null) {
			ident = state.getTierStyle().getUniqueName();
		}
		return ident;
	}

	public GraphStateI getGraphState() {
		return state;
	}

	private void draw(ViewI view, int graph_style) {
		if (TIME_DRAWING) {
			tim.start();
		}
		view.transformToPixels(coordbox, pixelbox);

		Graphics g = view.getGraphics();
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getOffsetY();

		// calculate slope (m)
		Color[] heatmap_colors = null;
		double heatmap_scaling = 1;
		if (state.getHeatMap() != null) {
			heatmap_colors = state.getHeatMap().getColors();
			heatmap_scaling = (double) (heatmap_colors.length - 1) / (getVisibleMaxY() - getVisibleMinY());
		}

		Rectangle2D.Double view_coordbox = view.getCoordBox();
		double xmin = view_coordbox.x;
		double xmax = view_coordbox.x + view_coordbox.width;

		if (getShowGrid() && !GraphState.isHeatMapStyle(getGraphStyle())) {
			drawHorizontalGridLines(view);
		}

		if (getShowGraph() && graf != null && graf.getPointCount() > 0) {
			DrawTheGraph(offset, view, g, yscale, graph_style, xmin, xmax, heatmap_scaling, heatmap_colors);
		}

		// drawing the "handle", which is the only part of the graph that recognizes hits
		// not a normal "child", so if it is hit then graph is considered to be hit...
		if (getShowHandle()) {
			drawHandle(view);
		}
		if (getShowAxis()) {
			drawAxisLabel(view);
		}

		// drawing outline around bounding box
		if (getShowBounds()) {
			g.setColor(Color.green);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width - 1, pixelbox.height - 1);
		}

		if (getShowLabel()) {
			drawLabel(view);
		}
		if (TIME_DRAWING) {
			System.out.println("graph draw time: " + tim.read());
		}
	}

	private void DrawTheGraph(double offset, ViewI view, Graphics g, double yscale, int graph_style, double xmin, double xmax, double heatmap_scaling, Color[] heatmap_colors) {
		float yzero = 0;
		if (getVisibleMinY() > yzero) {
			yzero = getVisibleMinY();
		} else if (getVisibleMaxY() < yzero) {
			yzero = getVisibleMaxY();
		}
		coord.y = offset - ((yzero - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, zero_point);

		if (getGraphState().getShowZeroLine() && graph_style != HEAT_MAP && yzero == 0) {
			// zero_point within min/max, so draw
			g.setColor(Color.gray);
			g.drawLine(pixelbox.x, zero_point.y, pixelbox.x + pixelbox.width, zero_point.y);
		}

		g.setColor(this.getColor());

		// set up prev_point before starting loop
		coord.x = graf.getMinXCoord();
		float prev_ytemp = graf.getGraphYCoord(0);
		coord.y = offset - ((prev_ytemp - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, prev_point);

		Point max_x_plus_width = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
		int draw_beg_index = GraphSymUtils.determineBegIndex(graf, xmin);
		int draw_end_index = GraphSymUtils.determineEndIndex(graf, xmax);
		
		float ytemp;
		int ymin_pixel;
		int yheight_pixel;
		int curr_max_index = 0; // used for heatmaps
		g.translate(xpix_offset, 0);

		RenderingHints original_render_hints = null;
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			original_render_hints = g2.getRenderingHints();
			Map<Object, Object> my_render_hints = new HashMap<Object, Object>();
			my_render_hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			g2.addRenderingHints(my_render_hints);
		}

		Point curr_x_plus_width = new Point(0, 0);

		// START OF BIG LOOP:
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			// flipping about yaxis... should probably make this optional
			// also offsetting to place within glyph bounds
			coord.x = graf.getGraphXCoord(i);
			ytemp = graf.getGraphYCoord(i);

			if (Double.isNaN(ytemp) || Double.isInfinite(ytemp)) {
				continue; //The data shouldn't contain any bad values, but it might.
			}

			// flattening any points > getVisibleMaxY() or < getVisibleMinY()...
			if (ytemp > getVisibleMaxY()) {
				ytemp = getVisibleMaxY();
			} else if (ytemp < getVisibleMinY()) {
				ytemp = getVisibleMinY();
			}
			coord.y = offset - ((ytemp - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, curr_point);

			if (this.hasWidth()) {
				Point2D.Double x_plus_width2D = new Point2D.Double(0, 0);
				x_plus_width2D.x = graf.getGraphXCoord(i) + this.getWCoord(i);
				x_plus_width2D.y = coord.y;
				view.transformToPixels(x_plus_width2D, curr_x_plus_width);
			}

			if (graph_style == LINE_GRAPH) {
				if (!this.hasWidth()) {
					g.drawLine(prev_point.x, prev_point.y, curr_point.x, curr_point.y);
				} else {
					// Draw a line representing the width: (x,y) to (x + width,y)
					g.drawLine(curr_point.x, curr_point.y, curr_x_plus_width.x, curr_x_plus_width.y);

					// Usually draw a line from (xA + widthA,yA) to next (xB,yB), but when there
					// are overlapping spans, only do this from the largest previous (x+width) value
					// to an xA that is larger than that.
					if (curr_point.x >= max_x_plus_width.x && max_x_plus_width.x != Integer.MIN_VALUE) {
						g.drawLine(max_x_plus_width.x, max_x_plus_width.y, curr_point.x, curr_point.y);
					}
					if (curr_x_plus_width.x >= max_x_plus_width.x) {
						max_x_plus_width.x = curr_x_plus_width.x; // xB + widthB
						max_x_plus_width.y = curr_x_plus_width.y; // yB
					}
				}
			} else if (graph_style == BAR_GRAPH) {
				if (curr_point.y > zero_point.y) {
					ymin_pixel = zero_point.y;
					yheight_pixel = curr_point.y - zero_point.y;
				} else {
					ymin_pixel = curr_point.y;
					yheight_pixel = zero_point.y - curr_point.y;
				}
				if (yheight_pixel < 1) {
					yheight_pixel = 1;
				}

				if (!this.hasWidth()) {
					g.drawLine(curr_point.x, ymin_pixel, curr_point.x, ymin_pixel + yheight_pixel);
				} else {
					final int width = Math.max(1, curr_x_plus_width.x - curr_point.x);
					g.drawRect(curr_point.x, ymin_pixel, width, yheight_pixel);
				}
			} else if (graph_style == DOT_GRAPH || graph_style == BIG_DOT_GRAPH) {
				if (!this.hasWidth()) {
					if (graph_style == BIG_DOT_GRAPH) {
						g.fillRect(curr_point.x - 1, curr_point.y - 1, 3, 3);
					} else {
						g.drawLine(curr_point.x, curr_point.y, curr_point.x, curr_point.y);	// point
					}
				} else {
					if (graph_style == BIG_DOT_GRAPH) {
						final int width = Math.max(1, curr_x_plus_width.x - curr_point.x);
						g.fillRect(curr_point.x, curr_point.y - 1, width, 3);
					} else {
						g.drawLine(curr_point.x, curr_point.y, curr_x_plus_width.x, curr_point.y);
					}
				}
			} else if (graph_style == HEAT_MAP) {


				if (!this.hasWidth()) {
					// there are no wcoords, so bars go from previous x to current x (like stairstep graphs)
					// When multiple coords map to one pixel, use the color corresponding to the max value.
					float the_y = prev_ytemp;
					int heatmap_index = (int) (heatmap_scaling * (the_y - getVisibleMinY()));

					if (heatmap_index < 0) {
						heatmap_index = 0;
					} else if (heatmap_index > 255) {
						heatmap_index = 255;
					}

					if (heatmap_index > curr_max_index) {
						curr_max_index = heatmap_index;
					}

					if (curr_point.x == prev_point.x) {
						g.setColor(heatmap_colors[curr_max_index]);
						g.drawLine(prev_point.x, pixelbox.y, prev_point.x, pixelbox.y + pixelbox.height);
					} else {
						g.setColor(heatmap_colors[heatmap_index]);
						// the x+1 start point prevents this from over-writing the last rectangle
						g.fillRect(prev_point.x + 1, pixelbox.y, curr_point.x - prev_point.x, pixelbox.height + 1);
						curr_max_index = 0;
					}
				} else {
					// the wcoords are not null, so the bars have width
					float the_y = ytemp;
					int heatmap_index = (int) (heatmap_scaling * (the_y - getVisibleMinY()));
					if (heatmap_index < 0) {
						heatmap_index = 0;
					} else if (heatmap_index > 255) {
						heatmap_index = 255;
					}
					int pixel_width = curr_x_plus_width.x - curr_point.x;
					g.setColor(heatmap_colors[heatmap_index]);
					if (pixel_width <= 1) {
						g.drawLine(curr_point.x, pixelbox.y, curr_point.x, pixelbox.y + pixelbox.height);
					} else {
						g.fillRect(curr_point.x, pixelbox.y, pixel_width, pixelbox.height + 1);
					}
				}
			} else if (graph_style == STAIRSTEP_GRAPH) {
				if (i <= 0 || (graf.getGraphYCoord(i - 1) != 0)) {
					int stairwidth = curr_point.x - prev_point.x;
					if ((stairwidth < 0) || (stairwidth > 10000)) {
						// skip drawing if width > 10000?  testing fix for linux problem
					} else {
						// draw the same regardless of whether wcoords == null
						g.fillRect(prev_point.x, Math.min(zero_point.y, prev_point.y), Math.max(1, stairwidth), Math.max(1, Math.abs(prev_point.y - zero_point.y)));
					}
				}
				// If this is the very last point, special rules apply
				if (i == draw_end_index) {
					int stairwidth = (!this.hasWidth()) ? 1 : curr_x_plus_width.x - curr_point.x;
					g.fillRect(curr_point.x, Math.min(zero_point.y, curr_point.y), Math.max(1, stairwidth), Math.max(1, Math.abs(curr_point.y - zero_point.y)));
				}
			}
			prev_point.x = curr_point.x;
			prev_point.y = curr_point.y;
			prev_ytemp = ytemp;
		}
		// END: big loop
		g.translate(-xpix_offset, 0);
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			if (original_render_hints != null) {
				g2.setRenderingHints(original_render_hints);
			}
		}

	}

	private void drawLabel(ViewI view) {
		if (state.getShowLabelOnRight()) {
			drawLabelRight(view);
		} else {
			drawLabelLeft(view);
		}
	}

	private void drawLabelRight(ViewI view) {
		if (getLabel() == null) {
			return;
		}

		// if full view differs from current view, and current view doesn't right align with full view,
		//   don't draw handle (only want handle at right side of full view)
		if (view.getFullView().getCoordBox().x + view.getFullView().getCoordBox().width != view.getCoordBox().x + view.getCoordBox().width) {
			return;
		}

		view.transformToPixels(coordbox, pixelbox);
		Rectangle view_pixbox = view.getPixelBox();

		if (view_pixbox != null) {
			Graphics g = view.getGraphics();
			g.setFont(default_font);
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D.Double sb = (Rectangle2D.Double) fm.getStringBounds(getLabel(), g);
			int stringWidth = (int) sb.getWidth() + 1;

			g.setColor(getColor());
			g.drawString(getLabel(), (view_pixbox.x + view_pixbox.width - stringWidth), (pixelbox.y + fm.getMaxAscent() - 1));
		}
	}

	private void drawLabelLeft(ViewI view) {
		if (getLabel() == null) {
			return;
		}
		Rectangle hpix = calcHandlePix(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			g.setColor(this.getColor());
			g.setFont(default_font);
			FontMetrics fm = g.getFontMetrics();
			g.drawString(getLabel(), (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
		}
	}

	private void drawHandle(ViewI view) {
		if (handle_width <= 0) {
			return;
		}
		Rectangle hpix = calcHandlePix(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			g.setColor(this.getColor());
			g.fillRect(hpix.x, hpix.y, hpix.width, hpix.height);
		}
	}

	private void drawHorizontalGridLines(ViewI view) {
		float[] grid = getGridLinesYValues();
		if (grid == null || grid.length == 0) {
			return;
		}

		Graphics2D g = view.getGraphics();

		view.transformToPixels(coordbox, pixelbox);
		Rectangle view_pixbox = view.getPixelBox();

		int xbeg = Math.max(view_pixbox.x, pixelbox.x);
		int xend = Math.max(view_pixbox.x + view_pixbox.width, pixelbox.x + view_pixbox.width);
		g.setColor(lighter);

		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getOffsetY();

		Stroke old_stroke = g.getStroke();
		BasicStroke grid_stroke = new BasicStroke(0.5f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10.0f,
			new float[]{1.0f, 10.0f}, 0.0f);
		g.setStroke(grid_stroke);
		g.setFont(axis_font);

		Point scratch_point = new Point(0, 0);

		for (int i = 0; i < grid.length; i++) {
			float gridY = grid[i];
			coord.x = 5;
			coord.y = offset - ((gridY - getVisibleMinY()) * yscale);
			if (gridY >= getVisibleMinY() && gridY <= getVisibleMaxY()) {
				view.transformToPixels(coord, scratch_point);
				g.drawLine(xbeg, scratch_point.y, xend, scratch_point.y);
			}
		}
		g.setStroke(old_stroke); // reset to orignial stroke
	}

	private void drawAxisLabel(ViewI view) {
		if (GraphState.isHeatMapStyle(getGraphStyle())) {
			return;
		}

		Rectangle hpix = calcHandlePix(view);
		if (hpix == null) {
			return;
		}

		Graphics g = view.getGraphics();
		g.setColor(this.getColor());
		g.setFont(axis_font);
		FontMetrics fm = g.getFontMetrics();
		int font_height = fm.getHeight();
		double last_pixel = Double.NaN; // the y-value at which the last tick String was drawn

		Double[] tick_coords = determineYTickCoords();
		double[] tick_pixels = convertToPixels(view, tick_coords);
		for (int i = 0; i < tick_pixels.length; i++) {
			double mark_ypix = tick_pixels[i];
			g.fillRect(hpix.x, (int) mark_ypix, hpix.width + 8, 1);
			// Always draw the lowest tick value, and indicate the others only
			// if there is enough room between them that the text won't overlap
			if (Double.isNaN(last_pixel) || Math.abs(mark_ypix - last_pixel) > font_height) {
				AttributedString minString = new AttributedString(nformat.format(tick_coords[i]));
				minString.addAttribute(TextAttribute.BACKGROUND, state.getTierStyle().getBackground());
				minString.addAttribute(TextAttribute.FOREGROUND, lighter);
				minString.addAttribute(TextAttribute.FONT, axis_font);
				g.drawString(minString.getIterator(), hpix.x + 15, (int) mark_ypix + fm.getDescent());
				last_pixel = mark_ypix;
			}
		}

	}

	/** Creates an array of about 4 to 10 coord values evenly spaced between 
	 * {@link #getVisibleMinY()} and {@link #getVisibleMaxY()}. 
	 */
	private Double[] determineYTickCoords() {
		float min = getVisibleMinY();
		float max = getVisibleMaxY();
		return determineYTickCoords(min, max);
	}

	/** Creates an array of about 4 to 10 coord values evenly spaced between min and max. */
	private static Double[] determineYTickCoords(double min, double max) {
		double range = max - min;
		double interval = Math.pow(10, Math.floor(Math.log10(range)));
		double start = Math.floor(min / interval) * interval;

		ArrayList<Double> coords = new ArrayList<Double>(10);
		for (double d = start; d <= max; d += interval) {
			if (d >= min && d <= max) {
				coords.add(d);
			}
		}

		// If there are not at least 4 ticks, then
		if (coords.size() < 4) { // try original interval divided by 2
			coords.clear();
			interval = interval / 2;
			start = Math.floor(min / interval) * interval;
			for (double d = start; d <= max; d += interval) {
				if (d >= min && d <= max) {
					coords.add(d);
				}
			}
		}

		// If there are not at least 4 ticks, then
		if (coords.size() < 4) { // take original interval divided by 5
			coords.clear();
			interval = (2 * interval) / 5;
			start = Math.floor(min / interval) * interval;
			for (double d = start; d <= max; d += interval) {
				if (d >= min && d <= max) {
					coords.add(d);
				}
			}
		}

		return coords.toArray(new Double[coords.size()]);
	}

	/** Calculate tick pixel positions based on tick coord positions. */
	private double[] convertToPixels(ViewI view, Double[] y_coords) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double yoffset = scratch_trans.getOffsetY();

		double[] y_pixels = new double[y_coords.length];
		for (int i = 0; i < y_coords.length; i++) {
			double tickY = y_coords[i];

			coord.y = yoffset - ((tickY - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, curr_point);
			y_pixels[i] = curr_point.y;
		}
		return y_pixels;
	}

	/** Draws the outline in a way that looks good for tiers.  With other glyphs,
	 *  the outline is usually drawn a pixel or two larger than the glyph.
	 *  With TierGlyphs, it is better to draw the outline inside of or contiguous
	 *  with the glyphs borders.
	 *  This method assumes the tiers are horizontal.
	 *  The left and right border are taken from the view's pixel box,
	 *  the top and bottom border are from the coord box.
	 **/
	@Override
	protected void drawSelectedOutline(ViewI view) {
		draw(view);
		Rectangle view_pixbox = view.getPixelBox();
		Graphics g = view.getGraphics();
		Color sel_color = view.getScene().getSelectionColor();
		g.setColor(sel_color);
		view.transformToPixels(getPositiveCoordBox(), pixelbox);

		// only outline the handle, not the whole graph
		if (handle_width > 0) {
			g.drawRect(view_pixbox.x, pixelbox.y,
					handle_width - 1, pixelbox.height - 1);
			g.drawRect(view_pixbox.x + 1, pixelbox.y + 1,
					handle_width - 3, pixelbox.height - 3);
		}

		// also draw a little pointing triangle to make the selection stand-out more
		int[] xs = {view_pixbox.x + handle_width,
			view_pixbox.x + handle_width + pointer_width,
			view_pixbox.x + handle_width};
		int[] ys = {pixelbox.y,
			pixelbox.y + (int) (0.5 * (pixelbox.height - 1)),
			pixelbox.y + pixelbox.height - 1};
		Color c = new Color(sel_color.getRed(), sel_color.getGreen(), sel_color.getBlue(), 128);
		g.setColor(c);
		g.fillPolygon(xs, ys, 3);
	}

	@Override
	public void moveRelative(double xdelta, double ydelta) {
		super.moveRelative(xdelta, ydelta);
		state.getTierStyle().setHeight(coordbox.height);
		state.getTierStyle().setY(coordbox.y);
		if (xdelta != 0.0f) {
			int[] xcoords = graf.getGraphXCoords();
			int maxi = this.getPointCount();
			for (int i = 0; i < maxi; i++) {
				xcoords[i] += xdelta;
			}
		}
	}

	@Override
	public void setCoords(double newx, double newy, double newwidth, double newheight) {
		super.setCoords(newx, newy, newwidth, newheight);
		state.getTierStyle().setHeight(newheight);
		state.getTierStyle().setY(newy);
	}

	/**
	 *  Designed to work in combination with pickTraversal().
	 *  If called outside of pickTraversal(), may get the wrong answer
	 *      since won't currently take account of nested transforms, etc.
	 */
	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		// within bounds of graph ?
		if (getShowHandle() && isVisible() && coord_hitbox.intersects(coordbox)) {
			// overlapping handle ?  (need to do this one in pixel space?)
			view.transformToPixels(coord_hitbox, pixel_hitbox);
			Rectangle hpix = calcHandlePix(view);
			if (hpix != null && (hpix.intersects(pixel_hitbox))) {
				return true;
			}
		}
		return false;
	}

	private Rectangle calcHandlePix(ViewI view) {
		// could cache pixelbox of handle, but then will have problems if try to
		//    have multiple views on same scene / glyph hierarchy
		// therefore reconstructing handle pixel bounds here... (although reusing same object to
		//    cut down on object creation)
		//    System.out.println("comparing full view cbox.x: " + view.getFullView().getCoordBox().x +
		//		       ", view cbox.x: " + view.getCoordBox().x);

		// if full view differs from current view, and current view doesn't left align with full view,
		//   don't draw handle (only want handle at left side of full view)
		if (view.getFullView().getCoordBox().x != view.getCoordBox().x) {
			return null;
		}
		view.transformToPixels(coordbox, pixelbox);
		Rectangle view_pixbox = view.getPixelBox();
		int xbeg = Math.max(view_pixbox.x, pixelbox.x);
		Graphics g = view.getGraphics();
		g.setFont(default_font);
		handle_pixbox.setBounds(xbeg, pixelbox.y, handle_width, pixelbox.height);

		return handle_pixbox;
	}

	/**
	 *  getGraphMaxY() returns max ycoord (in graph coords) of all points in graph.
	 *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
	 *     be modified (except for resetting the points by calling setPointCoords() again)
	 */
	float getGraphMinY() {
		return point_min_ycoord;
	}

	float getGraphMaxY() {
		return point_max_ycoord;
	}

	/**
	 *  getVisibleMaxY() returns max ycoord (in graph coords) that is visible (rendered).
	 *  This number can be modified via calls to setVisibleMaxY, and the visual effect is
	 *     to threhsold the graph drawing so that any points above max_ycoord render as max_ycoord
	 */
	public float getVisibleMaxY() {
		return state.getVisibleMaxY();
	}

	public float getVisibleMinY() {
		return state.getVisibleMinY();
	}

	/**
	 *  If want to override default setting of y min and max (based on setPointCoords()),
	 *    then must call setMaxY() and setMinY() _after_ call to setPointCoords() --
	 *    any subsequent call to setPointCoords will again reset y min and max.
	 */
	public void setVisibleMaxY(float ymax) {
		state.setVisibleMaxY(ymax);
	}

	/**
	 *  If want to override default setting of y min and max (based on setPointCoords()),
	 *    then must call setMaxY() and setMinY() _after_ call to setPointCoords() --
	 *    any subsequent call to setPointCoords will again reset y min and max.
	 */
	public void setVisibleMinY(float ymin) {
		state.setVisibleMinY(ymin);
	}

	@Override
	public void setColor(Color c) {
		setBackgroundColor(c);
		setForegroundColor(c);
		state.getTierStyle().setColor(c);
	}

	public String getLabel() {
		String lab = state.getTierStyle().getHumanName();
		// If it has a combo style and that is collapsed, then only use the label
		// from the combo style.  Otherwise use the individual tier style.
		if (state.getComboStyle() != null && state.getComboStyle().getCollapsed()) {
			lab = state.getComboStyle().getHumanName();
		}
		if (lab == null) {
			// if no label was set, try using ID
			lab = getID();
		}

		lab += " (" + nformat.format(state.getVisibleMinY()) + ", " + nformat.format(state.getVisibleMaxY()) + ")";

		return lab;
	}

	public boolean getShowGraph() {
		return state.getShowGraph();
	}

	public boolean getShowBounds() {
		return state.getShowBounds();
	}

	private boolean getShowHandle() {
		return state.getShowHandle();
	}

	public boolean getShowLabel() {
		return state.getShowLabel();
	}

	public boolean getShowAxis() {
		return state.getShowAxis();
	}

	private boolean getShowGrid() {
		return state.getShowGrid();
	}

	public int getXPixelOffset() {
		return xpix_offset;
	}

	public void setShowGraph(boolean show) {
		state.setShowGraph(show);
	}

	public void setShowBounds(boolean show) {
		state.setShowBounds(show);
	}

	public void setShowLabel(boolean show) {
		state.setShowLabel(show);
	}

	public void setShowAxis(boolean b) {
		state.setShowAxis(b);
	}

	private float[] getGridLinesYValues() {
		return state.getGridLinesYValues();
	}

	@Override
	public void setBackgroundColor(Color col) {
		super.setBackgroundColor(col);
		lighter = col.brighter();
		darker = col.darker();
		thresh_color = darker.darker();
		if (thresh_glyph != null) {
			thresh_glyph.setColor(thresh_color);
		}
	}

	public void setGraphStyle(int type) {
		state.setGraphStyle(type);
		if (type == HEAT_MAP) {
			setHeatMap(state.getHeatMap());
		}
	}

	public int getGraphStyle() {
		return state.getGraphStyle();
	}

	public final int[] getXCoords() {
		return graf.getGraphXCoords();
	}

	public final int getPointCount() {
		return graf.getPointCount();
	}

	public void setHeatMap(HeatMap hmap) {
		state.setHeatMap(hmap);
	}

	public HeatMap getHeatMap() {
		return state.getHeatMap();
	}

	@Override
	public void getChildTransform(ViewI view, LinearTransform trans) {
		double external_yscale = trans.getScaleY();
		double external_offset = trans.getOffsetY();
		double internal_yscale = coordbox.height / (getVisibleMaxY() - getVisibleMinY());
		double internal_offset = coordbox.y + coordbox.height;
		double new_yscale = internal_yscale * external_yscale * -1;
		double new_yoffset =
				(external_yscale * internal_offset) +
				(external_yscale * internal_yscale * getVisibleMinY()) +
				external_offset;
		trans.setScaleY(new_yscale);
		trans.setOffsetY(new_yoffset);
	}

	private double getUpperYCoordInset(ViewI view) {
		double top_ycoord_inset = 0;
		if (getShowLabel()) {
			Graphics g = view.getGraphics();
			g.setFont(default_font);
			FontMetrics fm = g.getFontMetrics();
			Rectangle label_pix_box = new Rectangle(0,fm.getAscent() + fm.getDescent());
			Rectangle2D.Double label_coord_box = new Rectangle2D.Double();
			view.transformToCoords(label_pix_box, label_coord_box);
			top_ycoord_inset = label_coord_box.height;
		}
		return top_ycoord_inset;
	}

	/**
	 *  Same as GraphGlyph.getInternalLinearTransform(), except
	 *  also caclulates a bottom y offset for showing thresholded
	 *  regions, if showThresholdedRegions() == true.
	 */
	private double getLowerYCoordInset(ViewI view) {
		/* This original super to this function had had its return value
		 * changed from 0 to 5 by GAH 3-21-2005.  bottom_ycoord_inset
		 * is set to five to mirror the original call to super */
		double bottom_ycoord_inset = 5;
		if (getShowThreshold()) {
			thresh_pix_box.height = thresh_contig_height + thresh_contig_yoffset;
			view.transformToCoords(thresh_pix_box, thresh_coord_box);
			bottom_ycoord_inset += thresh_coord_box.height;
		}
		return bottom_ycoord_inset;
	}

	private void getInternalLinearTransform(ViewI view, LinearTransform lt) {
		double top_ycoord_inset = getUpperYCoordInset(view);
		double bottom_ycoord_inset = getLowerYCoordInset(view);

		double num = getVisibleMaxY() - getVisibleMinY();
		if (num <= 0) {
			num = 0.1;
		} // if scale is 0 or negative, set to a small default instead

		double yscale = (coordbox.height - top_ycoord_inset - bottom_ycoord_inset) / num;
		double yoffset = coordbox.y + coordbox.height - bottom_ycoord_inset;
		lt.setScaleY(yscale);
		lt.setOffsetY(yoffset);
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

	private void DrawPoints(double xmin, double xmax, double offset, double yscale, ViewI view, int graph_style, Graphics g, int plot_bottom_ypixel, int plot_top_ypixel, double heatmap_scaling) {
		int draw_beg_index = GraphSymUtils.determineBegIndex(graf, xmin);
		int draw_end_index = GraphSymUtils.determineEndIndex(graf, xmax);
		coord.x = graf.getGraphXCoord(draw_beg_index);
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
			coord.x = graf.getGraphXCoord(i);
			coord.y = offset - ((graf.getGraphYCoord(i) - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, curr_point);
			if (prev_point.x == curr_point.x) {
				ymin_pixel = Math.min(ymin_pixel, curr_point.y);
				ymax_pixel = Math.max(ymax_pixel, curr_point.y);
				ysum += curr_point.y;
				points_in_pixel++;
			} else {
				// draw previous pixel position
				if ((graph_style == MINMAXAVG) || graph_style == LINE_GRAPH || graph_style == MIN_HEAT_MAP || graph_style == MAX_HEAT_MAP || graph_style == EXT_HEAT_MAP) {
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
							g.setColor(state.getHeatMap().getColor(max));
						} else {
							g.setColor(state.getHeatMap().getColor(min));
						}
						g.fillRect(prev_point.x, plot_top_ypixel, 3, plot_bottom_ypixel - plot_top_ypixel);
					}
					draw_count++;
				}
				yavg_pixel = ysum / points_in_pixel;
				if (graph_style == LINE_GRAPH) {
					// cache for drawing later
					if (prev_point.x > 0 && prev_point.x < pixel_cache.length) {
						pixel_cache[prev_point.x] = Math.min(Math.max(yavg_pixel, plot_top_ypixel), plot_bottom_ypixel);
					}
				}
				if (graph_style == LINE_GRAPH && i > 0 && i <= graf.getPointCount()) {
					coord.x = graf.getGraphXCoord(i - 1);
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
			prev_point.x = curr_point.x;
			// this line is sometimes redundant
			prev_point.y = curr_point.y;
		}
	}

	private void drawGraph(ViewI view) {
		if (this.getPointCount() == 0) {
			return;
		}
		int graph_style = getGraphStyle();
		view.transformToPixels(coordbox, pixelbox);
		Graphics g = view.getGraphics();
		double coords_per_pixel = 1.0F / ((LinearTransform) view.getTransform()).getScaleX();
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
		Point scratch_point = new Point(0, 0);
		view.transformToPixels(coord, scratch_point);
		int plot_top_ypixel = scratch_point.y;
		// replaces pixelbox.y
		coord.y = offset;
		view.transformToPixels(coord, scratch_point);
		int plot_bottom_ypixel = scratch_point.y;
		// replaces pbox_yheight
		double heatmap_scaling = 1;
		if (state.getHeatMap() != null) {
			Color[] heatmap_colors = state.getHeatMap().getColors();
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
		DrawAvgLine(graph_style, g, heatmap_scaling, plot_bottom_ypixel, plot_top_ypixel, coords_per_pixel);
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

	/**
	 * Draws thresholded regions.
	 * Current set up so that if regions_parent != null, then instead of drawing to view,
	 * populate regions_parent with child SeqSymmetries for each region that passes threshold,
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
		
		int draw_beg_index = 0;
		int draw_end_index;
		boolean make_syms = (region_holder != null) && (aseq != null);
		if (make_syms) {
			draw_end_index = this.getPointCount() - 1;
		} else {
			Rectangle2D.Double view_coordbox = view.getCoordBox();
			double xmin = view_coordbox.x;
			double xmax = view_coordbox.x + view_coordbox.width;
			draw_beg_index = GraphSymUtils.determineBegIndex(graf, xmin);
			draw_end_index = GraphSymUtils.determineEndIndex(graf, xmax);
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
		if (thresh_score < getVisibleMinY() || thresh_score > getVisibleMaxY()) {
			thresh_glyph.setVisibility(false);
		} else {
			thresh_glyph.setVisibility(true);
		}
		thresh_ycoord = getCoordValue(view, (float) thresh_score);
		thresh_glyph.setCoords(coordbox.x, thresh_ycoord, coordbox.width, 1);
		Graphics g = view.getGraphics();
		g.setColor(lighter);

		double pass_thresh_start = 0;
		double pass_thresh_end = 0;
		boolean pass_threshold_mode = false;
		int min_index = 0;
		int max_index = this.getPointCount() - 1;
		// need to widen range searched to include previous and next points out of view that
		//   pass threshold (unless distance to view is > max_gap_threshold
		int new_beg = draw_beg_index;
		// GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
		while ((new_beg > min_index) && ((graf.getGraphXCoord(draw_beg_index) - graf.getGraphXCoord(new_beg)) <= max_gap_threshold)) {
			new_beg--;
		}
		draw_beg_index = new_beg;
		int new_end = draw_end_index;
		boolean draw_previous = false;
		// GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
		while ((new_end < max_index) && ((graf.getGraphXCoord(new_end) - graf.getGraphXCoord(draw_end_index)) <= max_gap_threshold)) {
			new_end++;
		}
		draw_end_index = new_end;
		if (draw_end_index >= this.getPointCount()) {
			draw_end_index = this.getPointCount() - 1;
		}
		// eight possible states:
		//
		//     pass_threshold_mode    [y >= min_score_threshold]   [x-pass_thresh_end <= max_dis_thresh]
		//
		//  prune previous region and draw when:
		//      true, false, false
		//      true, true, false
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			double x = graf.getGraphXCoord(i);
			double w = 0;
			if (this.hasWidth()) {
				w = this.getWCoord(i);
			}
			double y = graf.getGraphYCoord(i);
			// GAH 2006-02-16 changed to > min_score instead of >= min_score, to better mirror Affy tiling array pipeline
			boolean pass_score_thresh = ((y > min_score_threshold) && (y <= max_score_threshold));
			boolean passes_max_gap = ((x - pass_thresh_end) <= max_gap_threshold);
			if (pass_threshold_mode) {
				if (!passes_max_gap) {
					draw_previous = true;
				} else if (pass_score_thresh) {
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
				boolean passes_min_run = (draw_max - draw_min) > min_run_threshold;
				if (passes_min_run) {
					// make sure aren't drawing single points
					coord.x = draw_min;
					view.transformToPixels(coord, prev_point);
					coord.x = draw_max;
					view.transformToPixels(coord, curr_point);
					if (make_syms) {
						SeqSymmetry sym = new SingletonSeqSymmetry((int) draw_min, (int) draw_max, aseq);
						region_holder.addChild(sym);
					} else {
						g.fillRect(prev_point.x, pixelbox.y + pixelbox.height - thresh_contig_height, curr_point.x - prev_point.x + 1, thresh_contig_height);
					}
				}
				draw_previous = false;
				pass_threshold_mode = pass_score_thresh;
				if (pass_score_thresh) {
					// current point passes threshold test, start new region scan
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
			boolean passes_min_run = (draw_max - draw_min) > min_run_threshold;
			if (passes_min_run) {
				coord.x = draw_min;
				view.transformToPixels(coord, prev_point);
				coord.x = draw_max;
				view.transformToPixels(coord, curr_point);
				if (make_syms) {
					SeqSymmetry sym = new SingletonSeqSymmetry((int) pass_thresh_start, (int) pass_thresh_end, aseq);
					region_holder.addChild(sym);
				} else {
					g.fillRect(prev_point.x, pixelbox.y + pixelbox.height - thresh_contig_height, curr_point.x - prev_point.x + 1, thresh_contig_height);
				}
			}
		}
	}

	/**
	 * Retrieve the map y coord corresponding to a given graph yvalue.
	 */
	private double getCoordValue(ViewI view, float graph_value) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getOffsetY();
		float coord_value = (float) (offset - ((graph_value - getVisibleMinY()) * yscale));
		return coord_value;
	}

	/**
	 * Retrieve the graph yvalue corresponding to a given ycoord.
	 */
	float getGraphValue(ViewI view, double coord_value) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getOffsetY();
		float graph_value = (float) ((offset - coord_value) / yscale) + getVisibleMinY();
		return graph_value;
	}

	public double getMaxGapThreshold() {
		return state.getMaxGapThreshold();
	}

	public float getMaxScoreThreshold() {
		return state.getMaxScoreThreshold();
	}

	public double getMinRunThreshold() {
		return state.getMinRunThreshold();
	}

	public float getMinScoreThreshold() {
		return state.getMinScoreThreshold();
	}

	public boolean getShowThreshold() {
		return state.getShowThreshold();
	}

	double getThreshEndShift() {
		return state.getThreshEndShift();
	}

	double getThreshStartShift() {
		return state.getThreshStartShift();
	}

	public int getThresholdDirection() {
		return state.getThresholdDirection();
	}

	private void resetThreshLabel() {
		float min_thresh = getMinScoreThreshold();
		float max_thresh = getMaxScoreThreshold();
		int direction = state.getThresholdDirection();
		if (direction == GraphState.THRESHOLD_DIRECTION_BETWEEN) {
			thresh_glyph.setLabel(GraphGlyph.nformat.format(min_thresh) + " -- " + GraphGlyph.nformat.format(max_thresh));
		} else if (direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
			thresh_glyph.setLabel(">= " + GraphGlyph.nformat.format(min_thresh));
		} else if (direction == GraphState.THRESHOLD_DIRECTION_LESS) {
			thresh_glyph.setLabel("<= " + GraphGlyph.nformat.format(max_thresh));
		}
	}

	void setMaxGapThreshold(int thresh) {
		state.setMaxGapThreshold(thresh);
	}

	void setMaxScoreThreshold(float thresh) {
		state.setMaxScoreThreshold(thresh);
		resetThreshLabel();
	}

	void setMinRunThreshold(int thresh) {
		state.setMinRunThreshold(thresh);
	}

	void setMinScoreThreshold(float thresh) {
		state.setMinScoreThreshold(thresh);
		resetThreshLabel();
	}

	void setShowThreshold(boolean show) {
		state.setShowThreshold(show);
		thresh_glyph.setVisibility(show);
	}

	void setThreshEndShift(double d) {
		state.setThreshEndShift(d);
	}

	void setThreshStartShift(double d) {
		state.setThreshStartShift(d);
	}

	void setThresholdDirection(int d) {
		state.setThresholdDirection(d);
		resetThreshLabel();
	}

	/**
	 * Sets the scale at which the drawing routine will switch between the
	 * style that is optimized for large genomic regions and the normal style.
	 */
	void setTransitionScale(double d) {
		transition_scale = d;
	}

	@Override
	public void draw(ViewI view) {
		if (DEBUG) {
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
			if ((xcoords_per_pixel < transition_scale)) {
				if ((graph_style == MINMAXAVG) && (Application.getSingleton() instanceof IGB)) {
					this.draw(view, BAR_GRAPH);
				} else {
					this.draw(view, LINE_GRAPH);
				}
			} else {
				drawSmart(view);
			}
		} else if (graph_style == MIN_HEAT_MAP || graph_style == MAX_HEAT_MAP || graph_style == AVG_HEAT_MAP || graph_style == EXT_HEAT_MAP) {
			drawSmart(view);
		} else {
			// Not one of the special styles, so default to regular GraphGlyph.draw method.
			this.draw(view, graph_style);
		}
		if (getShowThreshold()) {
			drawThresholdedRegions(view, null, null);
		} else {
			thresh_glyph.setVisibility(false);
		}
	}
}


