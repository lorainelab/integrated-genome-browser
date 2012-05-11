package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.igb.shared.AbstractGraphGlyph;

import java.awt.Graphics;
import java.awt.Point;

public class BarGraphGlyph extends AbstractGraphGlyph {

	public BarGraphGlyph(GraphSym graf, GraphState gstate) {
		super(graf, gstate);
	}

	@Override
	public String getName() {
		return "bargraph";
	}

	@Override
	protected void doBigDraw(Graphics g, GraphSym graphSym,
			Point curr_x_plus_width, Point max_x_plus_width,
			float ytemp, int draw_end_index, int i) {
		int ymin_pixel = Math.min(curr_point.y, zero_point.y);
		int yheight_pixel = Math.abs(curr_point.y - zero_point.y);
		yheight_pixel = Math.max(1, yheight_pixel);
		if (!graphSym.hasWidth()) {
			g.drawLine(curr_point.x, ymin_pixel, curr_point.x, ymin_pixel + yheight_pixel);
		} else {
			final int width = Math.max(1, curr_x_plus_width.x - curr_point.x);
			g.drawRect(curr_point.x, ymin_pixel, width, yheight_pixel);
		}
	}

	@Override
	public GraphType getGraphStyle() {
		return GraphType.BAR_GRAPH;
	}
}
