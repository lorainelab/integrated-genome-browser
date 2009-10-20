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

/**
 *  An implementation of graphs for NeoMaps, capable of rendering graphs in a variety of styles
 *  Started with {@link com.affymetrix.genoviz.glyph.BasicGraphGlyph} and improved from there.
 *  ONLY MEANT FOR GRAPHS ON HORIZONTAL MAPS.
 */
public abstract class GraphGlyph extends Glyph {

	public boolean TIME_DRAWING = false;
	// THICK_OUTLINE: should the selection outline be thick?
	static final boolean THICK_OUTLINE = true;
	static Font default_font = new Font("Courier", Font.PLAIN, 12);
	static Font axis_font = new Font("SansSerif", Font.PLAIN, 12);
	static NumberFormat nformat = new DecimalFormat();
	static NumberFormat nformat2 = NumberFormat.getIntegerInstance();
	static int axis_bins = 10;
	int xpix_offset = 0;
	Point zero_point = new Point(0, 0);
	Point2D.Double coord = new Point2D.Double(0, 0);
	Point curr_point = new Point(0, 0);
	Point prev_point = new Point(0, 0);
	Point scratch_point = new Point(0, 0);
	Rectangle2D.Double label_coord_box = new Rectangle2D.Double();
	Rectangle label_pix_box = new Rectangle();
	com.affymetrix.genoviz.util.Timer tim = new com.affymetrix.genoviz.util.Timer();
	static final boolean LARGE_HANDLE = true;
	boolean show_min_max = false;  // drawing lines for getVisibleMinY() and getVisibleMaxY() for debugging
	/**
	 *  point_max_ycoord is the max ycoord (in graph coords) of all points in graph.
	 *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
	 *     be modified (except for resetting the points by calling setPointCoords() again)
	 */
	float point_max_ycoord = Float.POSITIVE_INFINITY;
	float point_min_ycoord = Float.NEGATIVE_INFINITY;
	// assumes sorted points, each x corresponding to y
	GraphSym graf;
	public static int handle_width = 10;  // width of handle in pixels
	public static final int pointer_width = 10;
	Rectangle handle_pixbox = new Rectangle(); // caching rect for handle pixel bounds
	Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection
	GraphStateI state;
	LinearTransform scratch_trans = new LinearTransform();

	public float getXCoord(int i) {
		return graf.getGraphXCoord(i);
	}

	public float getYCoord(int i) {
		return graf.getGraphYCoord(i);
	}

	protected float getWCoord(int i) {
		return ((GraphIntervalSym) graf).getGraphWidthCoord(i);
	}

	public int[] getWCoords() {
		return ((GraphIntervalSym) graf).getGraphWidthCoords();
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
		
		if (graf instanceof GraphIntervalSym) {
			if (((GraphIntervalSym) graf).getGraphWidthCount() > 0) {
				if (((GraphIntervalSym) graf).getGraphWidthCount() != graf.getPointCount()) {
					return;
				}
			}
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
	}

	public boolean hasWidth() {
		return (graf instanceof GraphIntervalSym) && ((GraphIntervalSym)graf).getGraphWidthCount() > 0;
	}

	public float[] getVisibleYRange() {
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

	public boolean isUninitialized() {
		return getVisibleMinY() == Float.POSITIVE_INFINITY ||
				getVisibleMinY() == Float.NEGATIVE_INFINITY ||
				getVisibleMaxY() == Float.POSITIVE_INFINITY ||
				getVisibleMaxY() == Float.NEGATIVE_INFINITY;
	}

	protected void checkVisibleBoundsY() {
		if (isUninitialized()) {
			setVisibleMaxY(point_max_ycoord);
			setVisibleMinY(point_min_ycoord);
		}
	}

	public String getID() {
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
	// temporary variables used in draw()
	Point2D.Double x_plus_width2D = new Point2D.Double(0, 0);
	Point curr_x_plus_width = new Point(0, 0);

	@Override
	public void draw(ViewI view) {
		draw(view, getGraphStyle());
	}

	public void draw(ViewI view, int graph_style) {
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
		int beg_index = 0;
		if (show_min_max) {
			//	Point scratch_point = new Point();
			coord.y = offset; // visible min, since = offset - ((getVisibleMinY() - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, scratch_point);
			g.setColor(Color.yellow);
			g.drawLine(pixelbox.x, scratch_point.y, pixelbox.x + pixelbox.width, scratch_point.y);
			coord.y = offset - ((getVisibleMaxY() - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, scratch_point);
			g.setColor(Color.blue);
			g.drawLine(pixelbox.x, scratch_point.y, pixelbox.x + pixelbox.width, scratch_point.y);
		}

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
		coord.x = graf.getGraphXCoord(beg_index);
		coord.y = offset - ((graf.getGraphYCoord(beg_index) - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, prev_point);
		float prev_ytemp = graf.getGraphYCoord(beg_index);

		Point max_x_plus_width = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
		int draw_beg_index = determineBegIndex(xmin);
		int draw_end_index = determineEndIndex(xmax);
		// figure out what is the last x index value for the loop
		if (draw_end_index >= this.getPointCount()) {
			if (graph_style == HEAT_MAP || graph_style == DOT_GRAPH) {
				draw_end_index = this.getPointCount() - 1;
			} else {
				draw_end_index = this.getPointCount() - 2;
			}
		}

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

	public void drawLabel(ViewI view) {
		if (state.getShowLabelOnRight()) {
			drawLabelRight(view);
		} else {
			drawLabelLeft(view);
		}
	}

	public void drawLabelRight(ViewI view) {
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

	public void drawLabelLeft(ViewI view) {
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

	protected void drawHandle(ViewI view) {
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
	BasicStroke grid_stroke = new BasicStroke(0.5f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10.0f,
			new float[]{1.0f, 10.0f}, 0.0f);

	public void drawHorizontalGridLines(ViewI view) {
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
		g.setStroke(grid_stroke);
		g.setFont(axis_font);

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

	public void drawAxisLabel(ViewI view) {
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
	public Double[] determineYTickCoords() {
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
	public double[] convertToPixels(ViewI view, Double[] y_coords) {
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
			if (THICK_OUTLINE) {
				g.drawRect(view_pixbox.x + 1, pixelbox.y + 1,
						handle_width - 3, pixelbox.height - 3);
			}
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
	boolean mutable_xcoords = true;

	@Override
	public void moveRelative(double xdelta, double ydelta) {
		super.moveRelative(xdelta, ydelta);
		state.getTierStyle().setHeight(coordbox.height);
		state.getTierStyle().setY(coordbox.y);
		if (mutable_xcoords && xdelta != 0.0f) {
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

	protected Rectangle calcHandlePix(ViewI view) {
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
		FontMetrics fm = g.getFontMetrics();
		int h = Math.min(fm.getMaxAscent(), pixelbox.height);
		if (LARGE_HANDLE) {
			handle_pixbox.setBounds(xbeg, pixelbox.y, handle_width, pixelbox.height);
		} else {
			handle_pixbox.setBounds(xbeg, pixelbox.y, handle_width, h);
		}
		return handle_pixbox;
	}

	/**
	 *  getGraphMaxY() returns max ycoord (in graph coords) of all points in graph.
	 *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
	 *     be modified (except for resetting the points by calling setPointCoords() again)
	 */
	public float getGraphMinY() {
		return point_min_ycoord;
	}

	public float getGraphMaxY() {
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

	public boolean getShowHandle() {
		return state.getShowHandle();
	}

	public boolean getShowLabel() {
		return state.getShowLabel();
	}

	public boolean getShowAxis() {
		return state.getShowAxis();
	}

	public boolean getShowGrid() {
		return state.getShowGrid();
	}

	public int getXPixelOffset() {
		return xpix_offset;
	}

	public void setShowGraph(boolean show) {
		state.setShowGraph(show);
	}

	public void setShowHandle(boolean show) {
		state.setShowHandle(show);
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

	public void setShowGrid(boolean b) {
		state.setShowGrid(b);
	}

	public void setXPixelOffset(int offset) {
		xpix_offset = offset;
	}

	public void setGridLinesYValues(float[] f) {
		state.setGridLinesYValues(f);
	}

	public float[] getGridLinesYValues() {
		return state.getGridLinesYValues();
	}
	protected Color lighter;
	protected Color darker;

	@Override
	public void setBackgroundColor(Color col) {
		super.setBackgroundColor(col);
		lighter = col.brighter();
		darker = col.darker();
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

	public int[] getXCoords() {
		return graf.getGraphXCoords();
	}

	public int getPointCount() {
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

	protected double getUpperYCoordInset(ViewI view) {
		double top_ycoord_inset = 0;
		if (getShowLabel()) {
			Graphics g = view.getGraphics();
			g.setFont(default_font);
			FontMetrics fm = g.getFontMetrics();
			label_pix_box.height = fm.getAscent() + fm.getDescent();
			view.transformToCoords(label_pix_box, label_coord_box);
			top_ycoord_inset = label_coord_box.height;
		}
		return top_ycoord_inset;
	}

	protected double getLowerYCoordInset(ViewI view) {
		//    return 0;
		return 5;  // GAH 3-21-2005
	}

	protected void getInternalLinearTransform(ViewI view, LinearTransform lt) {
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

	public static void setLabelFont(Font f) {
		default_font = f;
	}

	protected int determineBegIndex(double xmin) {
		int draw_beg_index = Arrays.binarySearch(graf.getGraphXCoords(), (int) xmin);
		if (draw_beg_index < 0) {
			// want draw_beg_index to be index of max xcoord <= view_start
			//  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
			draw_beg_index = (-draw_beg_index - 1) - 1;
			if (draw_beg_index < 0) {
				draw_beg_index = 0;
			}
		}
		return draw_beg_index;
	}

	protected int determineEndIndex(double xmax) {
		int draw_end_index = Arrays.binarySearch(graf.getGraphXCoords(), (int) xmax) + 1;
		if (draw_end_index < 0) {
			// want draw_end_index to be index of min xcoord >= view_end
			//   (insertion point)  [as defined in Arrays.binarySearch() docs]
			draw_end_index = -draw_end_index - 1;
			if (draw_end_index < 0) {
				draw_end_index = 0;
			} else if (draw_end_index >= this.getPointCount()) {
				draw_end_index = this.getPointCount() - 1;
			}
			if (draw_end_index < (this.getPointCount() - 1)) {
				draw_end_index++;
			}
		}
		return draw_end_index;
	}
}


