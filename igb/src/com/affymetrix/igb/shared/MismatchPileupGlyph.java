package com.affymetrix.igb.shared;

/**
 *
 * @author lfrohman
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genoviz.bioviews.ViewI;

public final class MismatchPileupGlyph extends GraphGlyph {
	private static final String BASE_ORDER = "ATGCN";
	private static final Map<Character, int[]> BAR_ORDERS = new HashMap<Character, int[]>();
	static {
		BAR_ORDERS.put('A', new int[]{1,2,3,0});
		BAR_ORDERS.put('T', new int[]{0,2,3,1});
		BAR_ORDERS.put('G', new int[]{0,1,3,2});
		BAR_ORDERS.put('C', new int[]{0,1,2,3});
		BAR_ORDERS.put('N', new int[]{0,1,2,3});
	}
	private static final Color MATCH_COLOR = Color.GRAY;
	private static final Color[] baseColors = {
		ResidueColorHelper.getColorHelper().determineResidueColor('A'),
		ResidueColorHelper.getColorHelper().determineResidueColor('T'),
		ResidueColorHelper.getColorHelper().determineResidueColor('G'),
		ResidueColorHelper.getColorHelper().determineResidueColor('C'),
		ResidueColorHelper.getColorHelper().determineResidueColor('N')
	};
	public MismatchPileupGlyph(GraphSym graf, GraphState gstate) {
		super(graf, gstate);
	}
	public GraphType getGraphStyle() {
		return GraphType.FILL_BAR_GRAPH;
	}
	protected void bigDrawLoop(
			int draw_beg_index, int draw_end_index, double offset, double yscale, ViewI view, Point curr_x_plus_width,
			GraphType graph_style, Graphics g, Point max_x_plus_width) {
		MisMatchGraphSym mmgs = (MisMatchGraphSym)graf;
		Color saveColor = g.getColor();
//		Point p = new Point();
//		view.transformToPixels(new Point2D.Double(1,1), p);
//		int width = Math.max(1, (int)Math.round(p.getX()));
		Rectangle r = new Rectangle();
		view.transformToPixels(new Rectangle2D.Double(1,1,1,1), r);
		int width = Math.max(1, (int)Math.round(r.getWidth()));
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		seq.getResidues(graf.getMinXCoord(), graf.getMaxXCoord()); // so all are loaded, not one by one
		setVisibleMinY(0);
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			// flipping about yaxis... should probably make this optional
			// also offsetting to place within glyph bounds
			int xtemp = graf.getGraphXCoord(i);
			coord.x = xtemp;
			Point2D.Double x_plus_width2D = new Point2D.Double(0, 0);
			x_plus_width2D.x = xtemp + 1;
			int savey = zero_point.y;
			char referenceBase = seq.getResidues(xtemp,xtemp + 1).toUpperCase().charAt(0);
			float y_accum = getVisibleMinY();
			int[] barOrder = BAR_ORDERS.get(referenceBase);
			if (barOrder == null) {
				continue;
			}
			float[] residuesY = mmgs.getAllResiduesY(i);
			for (int j = 0; j < barOrder.length; j++) {
				int loopIndex = barOrder[j];
				float ytemp = residuesY[loopIndex];
				if (ytemp == 0) {
					continue;
				}
				if (Double.isNaN(ytemp) || Double.isInfinite(ytemp)) {
					return;
				}
				ytemp += y_accum;
				// flattening any points > getVisibleMaxY() or < getVisibleMinY()...
				ytemp = Math.min(ytemp, getVisibleMaxY());
				ytemp = Math.max(ytemp, getVisibleMinY());
	
				coord.y = offset - ((ytemp - getVisibleMinY()) * yscale);
				view.transformToPixels(coord, curr_point);
				x_plus_width2D.y = coord.y;
				view.transformToPixels(x_plus_width2D, curr_x_plus_width);

				int ymin_pixel = Math.min(curr_point.y, savey);
				int yheight_pixel = Math.abs(curr_point.y - savey);
				yheight_pixel = Math.max(1, yheight_pixel);
				if (BASE_ORDER.charAt(loopIndex) == referenceBase) {
					g.setColor(MATCH_COLOR);
				}
				else {
					g.setColor(baseColors[loopIndex]);
				}
				g.fillRect(curr_point.x, ymin_pixel, width, yheight_pixel);
				y_accum = ytemp;
				savey = curr_point.y;
			}
		}
		g.setColor(saveColor);
	}
}

