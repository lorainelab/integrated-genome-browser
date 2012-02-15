package com.affymetrix.igb.glyph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.ViewI;

public class LineGraphGlyph extends AbstractGraphGlyph {
	public LineGraphGlyph(GraphSym graf, GraphState gstate) {
		super(graf, gstate);
	}

	@Override
	public String getViewMode() {
		return "linegraph";
	}

	@Override
	protected void doBigDraw(Graphics g, GraphSym graphSym,
			Point curr_x_plus_width, Point max_x_plus_width,
			float ytemp, int draw_end_index, int i
		) {
		if (!graphSym.hasWidth()) {
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
	}

	@Override
	protected void DrawPoints(double offset, double yscale, ViewI view, Graphics g, int plot_bottom_ypixel, int plot_top_ypixel, float yzero, double coords_per_pixel) {
		if (yzero == 0) {
			g.setColor(Color.gray);
			g.drawLine(getPixelBox().x, zero_point.y, getPixelBox().width, zero_point.y);
		}
		g.setColor(getBackgroundColor());
		super.DrawPoints(offset, yscale, view, g, plot_bottom_ypixel, plot_top_ypixel, yzero, coords_per_pixel);
	}

	@Override
	protected void colorChange(Graphics g) {
		g.setColor(getBackgroundColor());
	}

	@Override
	protected void drawSingleRect(
			int ymin_pixel, int plot_bottom_ypixel, int plot_top_ypixel, int ymax_pixel, Graphics g, int ysum, int points_in_pixel, int i) {
		super.drawSingleRect(ymin_pixel, plot_bottom_ypixel, plot_top_ypixel, ymax_pixel, g, ysum, points_in_pixel, i);
		if (i > 0) {
			int y1 = Math.min(Math.max(prev_point.y, plot_top_ypixel), plot_bottom_ypixel);
			int y2 = Math.min(Math.max(curr_point.y, plot_top_ypixel), plot_bottom_ypixel);
			g.drawLine(prev_point.x, y1, curr_point.x, y2);
		}
	}

	@Override
	protected void doDraw(ViewI view) {
		double xpixels_per_coord = ( view.getTransform()).getScaleX();
		double xcoords_per_pixel = 1 / xpixels_per_coord;
		if ((xcoords_per_pixel < transition_scale)) {
			this.oldDraw(view);
		} else {
			drawSmart(view);
		}
	}
}
