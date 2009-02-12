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

package com.affymetrix.genoviz.widget.neotracer;
import com.affymetrix.genoviz.glyph.*;

import java.awt.*;
import java.util.Vector;
import java.util.Enumeration;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.datamodel.*;

/**
 * AsymAxisGlyph is a modification of the TraceBaseGlyph
 * with the Base Call drawing code ripped out ( Not particularly neatly ).
 * This allows the Axis to be positioned independently from the base calls,
 * particularly in the NeoTracer.
 * The Axis still uses a BaseCalls datamodel to get its spacing,
 * and the Mapping from that BaseCalls to get its numbering.
 * Perhaps it should have a reference to the Mapping independent of the BaseCalls?
 * ( Having the BaseCalls datamodel know about its Mapping it a little shaky,
 * but the axis needs it. )
 */
public class AsymAxisGlyph extends Glyph  {

	private BaseCalls base_calls;
	protected int dataCount, baseCount;
	private int start_num = 0;

	protected static Color numColor = Color.lightGray;

	protected static Font fnt = new Font("Helvetica", Font.PLAIN, 12);
	protected static FontMetrics fntmet = Toolkit.getDefaultToolkit().getFontMetrics(fnt);
	protected static int fntWidth = fntmet.charWidth('C');
	protected static int fntXOffset = fntWidth/2;
	protected static int fntHeight = fntmet.getHeight();

	protected static int tickHeight = 7;
	protected static int tickSpacer = 2;
	protected static int numHeight = fntHeight;
	protected static int numSpacer = 3;

	protected static int tickOffset = 4;
	protected static int numOffset = tickOffset + tickHeight + tickSpacer;
	protected static int numBaseline = numOffset + numHeight;

	private static int baseGlyphHeight = ( tickOffset +
			tickHeight +
			tickSpacer +
			numHeight +
			numSpacer
			);

	protected Point prevPixelPoint = new Point(0,0);
	protected Point currPixelPoint = new Point(0,0);
	protected Point basePixelPoint = new Point(0,0);
	protected Point selPixelPoint = new Point(0,0);

	protected Point2D prevCoordPoint = new Point2D(0,0);
	protected Point2D currCoordPoint = new Point2D(0,0);
	protected Point2D baseCoordPoint = new Point2D(0,0);
	protected Point2D selCoordPoint = new Point2D(0,0);

	protected Rectangle2D labelCoords = new Rectangle2D();
	protected Rectangle labelPixels = new Rectangle();

	/*
	   pixels_per_base is an _average_ over the whole trace
	   this is needed to determine a consistent number of
	   bases to add numbers to on the axis for a given resolution
	   Alternative (which was used at first) is to calculate this from the
	   actual number of bases shown in the view, but since this number can
	   change at a given scale with different translations, it can lead to
	   numbers flickering in and out as the user scrolls along the trace.
	   */
	protected double pixels_per_base, pixels_per_coord, coords_per_base;

	public AsymAxisGlyph() {
		this.setDrawOrder(DRAW_CHILDREN_FIRST);
	}

	public void setBaseCalls( BaseCalls theCalls ) {
		this.base_calls = theCalls;
		this.baseCount = theCalls.getBaseCount();
		this.dataCount = theCalls.getTrace().getTraceLength();
	}

	public final int getHeight() {
		return baseGlyphHeight;
	}

	public void draw(ViewI view) {
		int beg, end, i, j;
		Graphics g = view.getGraphics();
		Rectangle2D viewbox = view.getCoordBox();

		pixels_per_coord = ((LinearTransform)view.getTransform()).getScaleX();
		coords_per_base = (double)dataCount/(double)baseCount;
		pixels_per_base = pixels_per_coord * coords_per_base;

		beg = (int)viewbox.x;
		end = (int)(viewbox.x + viewbox.width);
		if (end < coordbox.x ) { end++; }
		if (end >= dataCount) { end = dataCount - 1; }
		if (beg < 0) { beg = 0; }

		// Drawing base letters along the axis for each visible base
		int stringsdrawn = 0;
		char theBase;
		int baseID;

		double minview = viewbox.x;
		double maxview = viewbox.x + viewbox.width;
		// GAH 12-2-97
		// Need to expand minview and maxview so they'll encompass any piece of
		// a base string or number that falls within the view!
		// Since base and number draws are pixel based, can currently have a base
		// or number whose coordinate falls outside of viewbox but that should be
		// drawn anyway since the backconverted coordinates of its _pixel_ bounds
		// overlaps the view.
		//
		// This wasn't really a problem when doing full redraws with every
		// View.draw() call, but is a big problem when doing scrolling
		// optimizations
		//
		// Base character width will always be a subset of potential number label
		// width.  Therefore Expand by calculating coordinate width of number label
		// from pixel width found through fontmetrics (assumes at the moment
		// that number font and base font are same, and that this font
		// is monospaced

		// just assume maximum number width,
		// assuming traces will be < 10000 bases long
		labelPixels.width = fntWidth*4;
		labelPixels.height = fntHeight;
		view.transformToCoords(labelPixels, labelCoords);
		// Giving plenty of room, could definitely optimize this more
		minview -= labelCoords.width;
		maxview += labelCoords.width;

		boolean bases_within_view = false;
		int firstbase=0, lastbase=0;

		// don't worry about setting default for firstbase/lastbase to nonsense --
		// if they aren't set in loop, they won't be used anyway
		//    firstbase = lastbase = Integer.MIN_VALUE;

		g.setFont(fnt);

		// should use average pixels per base here, rather than viewbox coords,
		// otherwise will throw off optimized scrolling
		int increment;

		if (pixels_per_base > 50) { increment = 2; }
		else if (pixels_per_base > 20) { increment = 5; }
		else if (pixels_per_base > 10) { increment = 10; }
		else if (pixels_per_base > 3) { increment = 20; }
		else { increment = 50; }

		BaseCall calledBase;

		// Drawing base numbers along the axis.
		if ( true || bases_within_view ) {
			g.setColor(numColor);

			Mapping aligner = this.base_calls.getAligner();

			try {
				for (i=0; i < baseCount; i += increment ) {
					calledBase = this.base_calls.getBaseCall(i);
					if ( calledBase != null) {
						baseCoordPoint.x = calledBase.getTracePoint();
						if ( (baseCoordPoint.x >= minview) &&
								(baseCoordPoint.x <= maxview))  {

							baseCoordPoint.y = coordbox.y;
							basePixelPoint =
								view.transformToPixels(baseCoordPoint, basePixelPoint);

							basePixelPoint.x -= fntWidth/2;
							// a little embellishment (need to get rid of hardwiring though)
							g.drawLine( basePixelPoint.x+3,
									basePixelPoint.y + tickOffset,
									basePixelPoint.x+3,
									basePixelPoint.y + tickOffset + tickHeight) ;

							g.drawString( String.valueOf(i+start_num), basePixelPoint.x,
									basePixelPoint.y + numBaseline );
								}
					}
				}
			}
			catch ( ArrayIndexOutOfBoundsException e ) {
				// System.err.println( "AsiymAxisGlyph.draw: baseCount too big: " + baseCount );
			}
		}

	}

	public void setNumColor(Color col) {
		numColor = col;
	}

	public void setStartPos( int start ) {
		this.start_num = start;
	}

	public int getStartPos() {
		return this.start_num;
	}

}
