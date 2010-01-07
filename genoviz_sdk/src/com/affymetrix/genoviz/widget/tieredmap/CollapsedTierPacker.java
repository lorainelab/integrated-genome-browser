/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget.tieredmap;

import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class CollapsedTierPacker extends AbstractCoordPacker implements PaddedPackerI {
	public static final int ALIGN_TOP = 1000;
	public static final int ALIGN_BOTTOM = 1001;
	public static final int ALIGN_CENTER = 1002;

	int alignment = ALIGN_CENTER;
	double maxHeight = 0;

	protected double parent_spacer = 2;


	public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) {
		double height = child.getCoordBox().height;
		if (height > maxHeight) {
			maxHeight = height;
			// need to repack siblings to reflect new max height!
			adjustHeight(parent);
			moveAllChildren(parent);
		}
		else {
			// max height hasn't changed, just move specified child glyph
			moveOneChild(parent, child);
		}
		return null;
	}


	@Override
	public Rectangle pack(GlyphI parent, ViewI view) {
		List<GlyphI> children = parent.getChildren();
		if (children == null) { return null; }
		for (GlyphI child : children) {
			maxHeight = Math.max(maxHeight, child.getCoordBox().height);
		}
		adjustHeight(parent);
		moveAllChildren(parent);
		return null;
	}

	protected void adjustHeight(GlyphI parent) {
		parent.getCoordBox().height = maxHeight + (2 * parent_spacer);
	}

	protected void moveOneChild(GlyphI parent, GlyphI child) {
		Rectangle2D.Double pbox = parent.getCoordBox();
		Rectangle2D.Double cbox = child.getCoordBox();

		if (alignment == ALIGN_TOP) {
			double top = pbox.y + parent_spacer;
			child.moveAbsolute(cbox.x, top);
		}
		else if (alignment == ALIGN_BOTTOM) {
			double bottom = pbox.y + pbox.height - parent_spacer;
			child.moveAbsolute(cbox.x, bottom - cbox.height);
		}
		else  {  // alignment == ALIGN_CENTER
			double parent_height = maxHeight + (2 * parent_spacer);
			double center = pbox.y + parent_height / 2;
			child.moveAbsolute(cbox.x, center - cbox.height/2);
		}
	}

	protected void moveAllChildren(GlyphI parent) {
		List<GlyphI> children = parent.getChildren();
		if (children == null) { return; }
		Rectangle2D.Double pbox = parent.getCoordBox();
		double parent_height = pbox.height;

		if (alignment == ALIGN_TOP) {
			double top = pbox.y + parent_spacer;
			for (GlyphI child : children) {
				child.moveAbsolute(child.getCoordBox().x, top);
			}
		}

		else if (alignment == ALIGN_BOTTOM) {
			double bottom = pbox.y + pbox.height - parent_spacer;
			Rectangle2D.Double cbox;
			for (GlyphI child : children) {
				cbox = child.getCoordBox();
				child.moveAbsolute(cbox.x, bottom - cbox.height);
			}
		}
		else  {  // alignment == ALIGN_CENTER
			double center = pbox.y + parent_height / 2;
			Rectangle2D.Double cbox;
			for (GlyphI child : children) {
				cbox = child.getCoordBox();
				child.moveAbsolute(cbox.x, center - cbox.height/2);
			}
		}
	}

	public void setParentSpacer(double spacer) {
		this.parent_spacer = spacer;
	}

	public double getParentSpacer() {
		return parent_spacer;
	}

	public void setAlignment(int val) {
		alignment = val;
	}


}
