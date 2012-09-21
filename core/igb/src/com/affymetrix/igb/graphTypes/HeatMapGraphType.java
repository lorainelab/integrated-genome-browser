
package com.affymetrix.igb.graphTypes;

import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.GraphGlyph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HeatMapGraphType extends GraphGlyph.GraphStyle {
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		temp.put("low_color", Color.class);
		temp.put("high_color", Color.class);
		PREFERENCES = Collections.unmodifiableMap(temp);
	}

	public HeatMapGraphType(GraphGlyph graphGlyph){
		graphGlyph.super();
	}
	
	@Override
	public String getName() {
		return "heatmapgraph";
	}

	@Override
	protected void doBigDraw(Graphics g, GraphSym graphSym, Point curr_x_plus_width, 
			Point max_x_plus_width, float ytemp, int draw_end_index, 
			double offset, double yscale, ViewI view, int i) {
		double heatmap_scaling = (double) (getHeatMap().getColors().length - 1) / (getVisibleMaxY() - getVisibleMinY());
		int heatmap_index = (int) (heatmap_scaling * (ytemp - getVisibleMinY()));
		if (heatmap_index < 0) {
			heatmap_index = 0;
		} else if (heatmap_index > 255) {
			heatmap_index = 255;
		}
		g.setColor(getHeatMap().getColor(heatmap_index));
		GraphGlyph.drawRectOrLine(g, curr_point.x, getPixelBox().y, 
				Math.max(1, curr_x_plus_width.x - curr_point.x), getPixelBox().height + 1);
	}

	@Override
	public boolean getShowAxis(){
		return false;
	}

	@Override
	protected void drawSingleRect(int ymin_pixel, int plot_bottom_ypixel, 
			int plot_top_ypixel, int ymax_pixel, Graphics g, int ysum, int points_in_pixel, 
			int width, int i) {
		int ystart = Math.max(Math.min(ymin_pixel, plot_bottom_ypixel), plot_top_ypixel);
		double heatmap_scaling = 1;
		if (getHeatMap() != null) {
			Color[] heatmap_colors = getHeatMap().getColors();
			// scale based on pixel position, not cooord position, since most calculations below are in pixels
			heatmap_scaling = (double) (heatmap_colors.length - 1) / (-plot_top_ypixel + plot_bottom_ypixel);
		}
		g.setColor(getHeatMap().getColor((int) (heatmap_scaling * (plot_bottom_ypixel - ystart))));
		//drawRectOrLine(g, prev_point.x, plot_top_ypixel, 1, plot_bottom_ypixel - plot_top_ypixel);
		GraphGlyph.drawRectOrLine(g, prev_point.x, 
				getPixelBox().y, width, getPixelBox().height + 1);
	}

	@Override
	protected void doDraw(ViewI view) {
		double xpixels_per_coord = (view.getTransform()).getScaleX();
		double xcoords_per_pixel = 1 / xpixels_per_coord;
		if (xcoords_per_pixel < transition_scale) {
			oldDraw(view);
		} else {
			drawSmart(view);
		}
	}

	public Map<String, Class<?>> getPreferences() {
		Map<String,Class<?>> preferences = new HashMap<String,Class<?>>(PREFERENCES);
		//preferences.putAll(super.getPreferences());
		return preferences;
	}


	public void setPreferences(Map<String, Object> preferences) {
	}

	@Override
	public GraphType getGraphStyle() {
		return GraphType.HEAT_MAP;
	}
}
