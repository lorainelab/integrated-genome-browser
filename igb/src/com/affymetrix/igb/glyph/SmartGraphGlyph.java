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
import com.affymetrix.genoviz.bioviews.ViewI;

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
