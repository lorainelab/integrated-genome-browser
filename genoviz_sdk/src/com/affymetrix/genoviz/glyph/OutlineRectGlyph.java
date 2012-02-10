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

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;


/**
 * A glyph drawn as a filled or empty outlined rectangle.
 */
public class OutlineRectGlyph extends SolidGlyph  {

	private static final boolean DEBUG_EDGES = false;
	private static final boolean CHECK_EDGES = true;

	boolean fill_rect = false;

	/** Draws the glyph.
	 *  Something goes wrong in Graphics draw routines if
	 *   x/y/width/height are too large (negative or positive)
	 * Therefore need to check:
	 * If whole glyph is within view, just call drawRect()
	 * But if part of glyph is outside of view, then need to
	 * adjust pixelbox and use drawLine() to draw just the parts of
	 * the outline that are visible
	 */
	public void draw(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());
		if (getPixelBox().width <= 0) { getPixelBox().width = 0; }
		if (getPixelBox().height <= 0) { getPixelBox().height = 0; }
		Graphics g = view.getGraphics();

		if (fill_rect) {
			g.setColor(getBackgroundColor());
			g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height);
		}

		g.setColor(getForegroundColor());
		Rectangle vpix = view.getPixelBox();

		//    Trying a check on component size rather than on view, to avoid
		//    conflicts with scrolling & damage optimizations
		//      (or should this be checking component.bounds()???
		Dimension comp_size = view.getComponentSize();
		// this check shouldn't be necessary, but just in case...
		if (comp_size == null) { return; }

		if ((!CHECK_EDGES) ||
				((getPixelBox().x >= 0) &&
				 (getPixelBox().x+getPixelBox().width <= comp_size.width) &&
				 (getPixelBox().y >= 0) &&
				 (getPixelBox().y+getPixelBox().height <= comp_size.height))) {
			g.drawRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height);
				 }
		else {
			if (DEBUG_EDGES) {
				System.err.println("OutlineRectGlyph not entirely within view, " +
						"calculating edges");
			}
			boolean drawXBegEdge, drawXEndEdge, drawYBegEdge, drawYEndEdge;

			//Checking against component dimensions instead of view, to avoid
			//    conflicts with scrolling & damage optimizations
			drawXBegEdge = (getPixelBox().x >= 0);
			drawXEndEdge = (getPixelBox().x+getPixelBox().width <= comp_size.width);
			drawYBegEdge = (getPixelBox().y >= 0);
			drawYEndEdge = (getPixelBox().y+getPixelBox().height <= comp_size.height);

			if (!drawXBegEdge) {
				if (DEBUG_EDGES) { System.out.println("Not drawing x beg edge"); }
				getPixelBox().width = getPixelBox().x + getPixelBox().width - vpix.x;
				getPixelBox().x = vpix.x;
			}
			if (!drawYBegEdge) {
				if (DEBUG_EDGES) { System.out.println("Not drawing y beg edge"); }
				getPixelBox().height = getPixelBox().y + getPixelBox().height - vpix.y;
				getPixelBox().y = vpix.y;
			}
			if (!drawXEndEdge) {
				if (DEBUG_EDGES) { System.out.println("Not drawing x end edge"); }
				getPixelBox().width = vpix.x+vpix.width-getPixelBox().x;
			}
			if (!drawYEndEdge) {
				if (DEBUG_EDGES) { System.out.println("Not drawing y end edge"); }
				getPixelBox().height = vpix.y+vpix.height-getPixelBox().y;
			}

			if (drawYBegEdge) {
				g.drawLine(getPixelBox().x, getPixelBox().y,
						getPixelBox().x+getPixelBox().width-1, getPixelBox().y);
			}
			if (drawXEndEdge) {
				g.drawLine(getPixelBox().x+getPixelBox().width, getPixelBox().y,
						getPixelBox().x+getPixelBox().width, getPixelBox().y+getPixelBox().height-1);
			}
			if (drawYEndEdge) {
				g.drawLine(getPixelBox().x+getPixelBox().width, getPixelBox().y+getPixelBox().height,
						getPixelBox().x, getPixelBox().y+getPixelBox().height);
			}
			if (drawXBegEdge) {
				g.drawLine(getPixelBox().x, getPixelBox().y+getPixelBox().height,
						getPixelBox().x, getPixelBox().y+1);
			}

		}
		super.draw(view);
	}

	/**
	 * Sets the color of the outline.
	 * Use {@link #setBackgroundColor(Color)} to
	 *  set the fill color, if you have also
	 *  called {@link #setFillRect(boolean)} to true.
	 *
	 * @param color the outline color.
	 * @deprecated use setForegroundColor(Color) for the outline color and
	 *  setBackgroundColor(Color) for the fill color.
	 */
	@Deprecated
		public void setColor(Color color)  {
			this.setForegroundColor( color );
		}

	/**
	 * Returns {@link #getForegroundColor()}.
	 * @deprecated use {@link #getForegroundColor}.
	 */
	@Deprecated
		public Color getColor() {
			return getForegroundColor();
		}

	/**
	 * Set whether the rectangle should be filled as well as outlined.
	 * Default is false.  Fill color is set with {@link #setBackgroundColor(Color)}.
	 */
	public void setFillRect(boolean b) {
		fill_rect = b;
	}

	public boolean getFillRect() {
		return fill_rect;
	}

}
