package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class MinMaxAvgGraphGlyph extends AbstractGraphGlyph {
	private final AbstractGraphGlyph tempViewModeBarGraphGlyph;

	public MinMaxAvgGraphGlyph(GraphSym graf, GraphState gstate) {
		super(graf, gstate);
		tempViewModeBarGraphGlyph = new BarGraphGlyph(graf, gstate);
	}

	@Override
	public String getName() {
		return "minmaxavggraph";
	}

	@Override
	protected void doBigDraw(Graphics g, GraphSym graphSym, Point curr_x_plus_width, Point max_x_plus_width, float ytemp, int draw_end_index, int i) {
	}

	@Override
	protected void drawSmart(ViewI view) {
		// could size cache to just the view's pixelbox, but then may end up creating a
		//   new int array every time the pixelbox changes (which with view damage or
		//   scrolling optimizations turned on could be often)
		// could size cache to just the view's pixelbox, but then may end up creating a
		//   new int array every time the pixelbox changes (which with view damage or
		//   scrolling optimizations turned on could be often)
		int comp_ysize = ((View) view).getComponentSize().width;
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
		g.setColor(lighter);
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
	protected void DrawPoints(double offset, double yscale, ViewI view, Graphics g, int plot_bottom_ypixel, int plot_top_ypixel, float yzero, double coords_per_pixel) {
		if (yzero == 0) {
			g.setColor(Color.gray);
			g.drawLine(getPixelBox().x, zero_point.y, getPixelBox().width, zero_point.y);
		}
		g.setColor(darker);
		super.DrawPoints(offset, yscale, view, g, plot_bottom_ypixel, plot_top_ypixel, yzero, coords_per_pixel);
		DrawAvgLine(g, coords_per_pixel);
	}

	@Override
	protected void colorChange(Graphics g) {
		g.setColor(darker);
	}

	@Override
	protected void drawSingleRect(int ymin_pixel, int plot_bottom_ypixel, int plot_top_ypixel, int ymax_pixel, Graphics g, int ysum, int points_in_pixel, int i) {
		// cache for drawing later
		// cache for drawing later
		if (prev_point.x > 0 && prev_point.x < pixel_avg_cache.length) {
			int yavg_pixel = ysum / points_in_pixel;
			pixel_avg_cache[prev_point.x] = Math.min(Math.max(yavg_pixel, plot_top_ypixel), plot_bottom_ypixel);
		}
		super.drawSingleRect(ymin_pixel, plot_bottom_ypixel, plot_top_ypixel, ymax_pixel, g, ysum, points_in_pixel, i);
	}

	@Override
	protected void doDraw(ViewI view) {
		double xpixels_per_coord = (view.getTransform()).getScaleX();
		double xcoords_per_pixel = 1 / xpixels_per_coord;
		if (xcoords_per_pixel < transition_scale) {
			tempViewModeBarGraphGlyph.oldDraw(view);
		} else {
			drawSmart(view);
		}
	}

	@Override
	public void setCoords(double x, double y, double width, double height)  {
		super.setCoords(x, y, width, height);
		if(tempViewModeBarGraphGlyph != null){
			tempViewModeBarGraphGlyph.setCoords(x, y, width, height);
		}
	}
	
	@Override
	public void setCoordBox(Rectangle2D.Double coordbox)   {
		super.setCoordBox(coordbox);
		if(tempViewModeBarGraphGlyph != null){
			tempViewModeBarGraphGlyph.setCoordBox(coordbox);
		}
	}
	
	@Override
	public void setPreferredHeight(double height, ViewI view) {
		super.setPreferredHeight(height, view);
		if(tempViewModeBarGraphGlyph != null){
			tempViewModeBarGraphGlyph.setPreferredHeight(height, view);
		}
	}
	
//	@Override
//	public void moveRelative(double diffx, double diffy) {
//		super.moveRelative(diffy, diffy);
//		tempViewModeBarGraphGlyph.moveRelative(diffy, diffy);
//	}
//	
//	@Override
//	public void moveAbsolute(double x, double y){
//		super.moveAbsolute(x, y);
//		tempViewModeBarGraphGlyph.moveAbsolute(x, y);
//	}
	
	@Override
	public GraphType getGraphStyle() {
		return GraphType.MINMAXAVG;
	}
}
