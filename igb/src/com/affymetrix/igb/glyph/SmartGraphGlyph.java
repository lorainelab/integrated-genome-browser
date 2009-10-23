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

import java.awt.Color;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;

/*
 * These are replacing private copies which (Smart)GraphGlyph used to keep
 */
import static com.affymetrix.genometryImpl.style.GraphStateI.BAR_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.AVG_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.EXT_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.MAX_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.MIN_HEAT_MAP;
import static com.affymetrix.genometryImpl.style.GraphStateI.LINE_GRAPH;
import static com.affymetrix.genometryImpl.style.GraphStateI.MINMAXAVG;

/**
 *  A smarter graph glyph.
 *  It:
 * <ol>
 * <li> improves efficiency of drawing zoomed-out large graphs (via max once-per-pixel drawing)
 * <li> improves efficiency of drawing zoomed-in large graphs (via binary search for draw end-points)
 * <li> allows for summarizing min, max, and average separately for zoomed-out graphs
 * <li> allows for on-the-fly thresholding and "glyphification" of regions that pass threshold
 * </ol>
 *
 *  Also, now trying to further improve efficiency for zoomed-out large graphs via data structure
 *      enhancements.
 *  And, working on drawing y-axis....
 *
 *  ONLY MEANT FOR GRAPHS ON HORIZONTAL MAPS
 */
public final class SmartGraphGlyph extends GraphGlyph {

	public SmartGraphGlyph(GraphSym graf, GraphStateI gstate) {
		super(graf, gstate);
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

	@Override
	public void draw(ViewI view) {
		if (NEWDEBUG) {
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
			if (TRANSITION_TO_BARS && (xcoords_per_pixel < transition_scale)) {
				// if at resolution where bars should be displayed, then draw as LINE or BAR graph
				if ((graph_style == MINMAXAVG) &&
						(Application.getSingleton() instanceof IGB)) {
					super.draw(view, BAR_GRAPH);
				} else {
					super.draw(view, LINE_GRAPH);
				}
			} else {
				drawSmart(view);
			}

		} else if (graph_style == MIN_HEAT_MAP || graph_style == MAX_HEAT_MAP || graph_style == AVG_HEAT_MAP || graph_style == EXT_HEAT_MAP) {

			drawSmart(view);

		} else {
			// Not one of the special styles, so default to regular GraphGlyph.draw method.
			super.draw(view);
		}

		if (getShowThreshold()) {
			drawThresholdedRegions(view);
		} else {
			thresh_glyph.setVisibility(false);
		}
	}

	@Override
	public void setBackgroundColor(Color col) {
		super.setBackgroundColor(col);
		thresh_color = darker.darker();
		if (thresh_glyph != null) {
			thresh_glyph.setColor(thresh_color);
		}
	}

	/**
	 *  Same as GraphGlyph.getInternalLinearTransform(), except
	 *  also caclulates a bottom y offset for showing thresholded
	 *  regions, if showThresholdedRegions() == true.
	 */
	@Override
	protected double getLowerYCoordInset(ViewI view) {
		double bottom_ycoord_inset = super.getLowerYCoordInset(view);
		if (getShowThreshold()) {
			thresh_pix_box.height = thresh_contig_height + thresh_contig_yoffset;
			view.transformToCoords(thresh_pix_box, thresh_coord_box);
			bottom_ycoord_inset += thresh_coord_box.height;
		}
		return bottom_ycoord_inset;
	}
}
