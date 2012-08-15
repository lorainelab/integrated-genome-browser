
package com.affymetrix.igb.graphTypes;

import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.GraphGlyph;

import java.awt.Graphics;
import java.awt.Point;

public class DotGraphGlyph extends GraphGlyph.GraphStyle {

	public DotGraphGlyph(GraphGlyph graphGlyph){
		graphGlyph.super();
	}
	
	@Override
	public String getName() {
		return "dotgraph";
	}

	@Override
	protected void doBigDraw(Graphics g, GraphSym graphSym, 
			Point curr_x_plus_width, Point max_x_plus_width, float ytemp, 
			int draw_end_index, double offset, double yscale, ViewI view, int i) {
		if (!graphSym.hasWidth()) {
			g.drawLine(curr_point.x, curr_point.y, curr_point.x, curr_point.y); // point
		} else {
			g.drawLine(curr_point.x, curr_point.y, curr_x_plus_width.x, curr_point.y);
		}
	}

	@Override
	public GraphType getGraphStyle() {
		return GraphType.DOT_GRAPH;
	}

}
