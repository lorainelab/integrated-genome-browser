package com.affymetrix.igb.graphTypes;

import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.GraphGlyph;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Arrays;

public class MinMaxAvgGraphType extends GraphGlyph.GraphStyle {
	private int[] pixel_avg_cache;
	private final GraphGlyph.GraphStyle tempBarGraphStyle;

	public MinMaxAvgGraphType(GraphGlyph graphGlyph){
		graphGlyph.super();
		tempBarGraphStyle = new EmptyBarGraphType(graphGlyph);
	}

	@Override
	public String getName() {
		return "minmaxavggraph";
	}

	@Override
	protected void doBigDraw(Graphics g, GraphSym graphSym, 
			Point curr_x_plus_width, Point max_x_plus_width, float ytemp, 
			int draw_end_index, double offset, double yscale, ViewI view, int i) {
	}

	@Override
	protected void drawSmart(ViewI view) {
		// could size cache to just the view's pixelbox, but then may end up creating a
		//   new int array every time the pixelbox changes (which with view damage or
		//   scrolling optimizations turned on could be often)
		// could size cache to just the view's pixelbox, but then may end up creating a
		//   new int array every time the pixelbox changes (which with view damage or
		//   scrolling optimizations turned on could be often)
		int comp_ysize = view.getComponentSize().width;
		// could check for exact match with comp_ysize, but allowing larger comp size here
		//    may be good for multiple maps that share the same scene, so that new int array
		//    isn't created every time paint switches from mapA to mapB -- the array will
		//    be reused and be the length of the component with greatest width...
		// could check for exact match with comp_ysize, but allowing larger comp size here
		//    may be good for multiple maps that share the same scene, so that new int array
		//    isn't created every time paint switches from mapA to mapB -- the array will
		//    be reused and be the length of the component with greatest width...
		if ((pixel_avg_cache == null) || (pixel_avg_cache.length < comp_ysize)) {
			pixel_avg_cache = new int[comp_ysize];
		}
		Arrays.fill(pixel_avg_cache, 0, comp_ysize - 1, Integer.MIN_VALUE);
		super.drawSmart(view);
	}

	private void DrawAvgLine(Graphics g, double coords_per_pixel) {
		g.setColor(getLighterColor());
		int prev_index = 0;
		// find the first pixel position that has a real value in pixel_cache
		// find the first pixel position that has a real value in pixel_cache
		while ((prev_index < pixel_avg_cache.length) && (pixel_avg_cache[prev_index] == Integer.MIN_VALUE)) {
			prev_index++;
		}
		for (int i = prev_index + 1; i < pixel_avg_cache.length; i++) {
			// successfully found a real value in pixel cache
			// successfully found a real value in pixel cache
			int yval = pixel_avg_cache[i];
			if (yval == Integer.MIN_VALUE) {
				continue;
			}
			if (pixel_avg_cache[i - 1] == Integer.MIN_VALUE && coords_per_pixel > 30) {
				g.drawLine(i, yval, i, yval);
			} else {
				// last pixel had at least one datapoint, so connect with line
				// last pixel had at least one datapoint, so connect with line
				g.drawLine(prev_index, pixel_avg_cache[prev_index], i, yval);
			}
			prev_index = i;
		}
	}

	@Override
	protected void DrawPoints(double offset, double yscale, 
			ViewI view, Graphics g, int plot_bottom_ypixel, int plot_top_ypixel, 
			float yzero, double coords_per_pixel) {
		if (yzero == 0) {
			g.setColor(Color.gray);
			g.drawLine(getPixelBox().x, zero_point.y, getPixelBox().width, zero_point.y);
		}
		g.setColor(getDarkerColor());
		super.DrawPoints(offset, yscale, view, g, plot_bottom_ypixel, plot_top_ypixel, yzero, coords_per_pixel);
		DrawAvgLine(g, coords_per_pixel);
	}

	@Override
	protected void colorChange(Graphics g) {
		g.setColor(getDarkerColor());
	}

	@Override
	protected void drawSingleRect(int ymin_pixel, 
			int plot_bottom_ypixel, int plot_top_ypixel, int ymax_pixel, 
			Graphics g, int ysum, int points_in_pixel, int width, int i) {
		// cache for drawing later
		// cache for drawing later
		if (prev_point.x > 0 && prev_point.x < pixel_avg_cache.length) {
			int yavg_pixel = ysum / points_in_pixel;
			pixel_avg_cache[prev_point.x] = Math.min(Math.max(yavg_pixel, plot_top_ypixel), plot_bottom_ypixel);
		}
		super.drawSingleRect(ymin_pixel, plot_bottom_ypixel, plot_top_ypixel, ymax_pixel, g, ysum, points_in_pixel, width, i);
	}

	@Override
	protected void doDraw(ViewI view) {
		double xpixels_per_coord = (view.getTransform()).getScaleX();
		double xcoords_per_pixel = 1 / xpixels_per_coord;
		if (xcoords_per_pixel < transition_scale) {
			tempBarGraphStyle.oldDraw(view);
		} else {
			drawSmart(view);
		}
	}
		
	@Override
	public GraphType getGraphStyle() {
		return GraphType.MINMAXAVG;
	}
}
