package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import java.awt.Graphics;
import java.awt.Point;

/**
 *
 * @author lfrohman
 */
class StairStepGraphGlyph extends AbstractGraphGlyph {

	public StairStepGraphGlyph(GraphSym graf, GraphState gstate) {
		super(graf, gstate);
	}

	@Override
	public String getName() {
		return "stairstepgraph";
	}

	@Override
	protected void doBigDraw(Graphics g, GraphSym graphSym, Point curr_x_plus_width, Point max_x_plus_width, float ytemp, int draw_end_index, int i) {
		int endx = curr_point.x;
		int stairwidth = endx - prev_point.x;
		if (stairwidth >= 0 && stairwidth <= 10000 && (i == 0 || graphSym.getGraphYCoord(i - 1) != 0)) {
			// skip drawing if width > 10000... (fix for linux problem?)
			// draw the same regardless of whether wcoords == null
			// skip drawing if width > 10000... (fix for linux problem?)
			// draw the same regardless of whether wcoords == null
			drawRectOrLine(g, prev_point.x, Math.min(zero_point.y, prev_point.y), Math.max(1, stairwidth), Math.max(1, Math.abs(prev_point.y - zero_point.y)));
		}
		// If this is the very last point, special rules apply
		// If this is the very last point, special rules apply
		if (i == draw_end_index) {
			stairwidth = (!graphSym.hasWidth()) ? 1 : curr_x_plus_width.x - curr_point.x;
			drawRectOrLine(g, curr_point.x, Math.min(zero_point.y, curr_point.y), Math.max(1, stairwidth), Math.max(1, Math.abs(curr_point.y - zero_point.y)));
		}
	}
    
}
