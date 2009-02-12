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

package com.affymetrix.genoviz.widget.neoqualler;

import java.awt.*;
import java.util.Vector;
import java.util.Enumeration;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.widget.NeoQualler;
import com.affymetrix.genoviz.glyph.*;

public class QualityBases extends Glyph  {
	protected ReadConfidence read_conf;

	protected static String baseString[] = { "A", "C", "G", "T", "-" };
	protected static Color baseColor[] = { Color.green, Color.cyan,
		Color.yellow, Color.red, Color.white };
	protected static Color numColor = Color.lightGray;
	protected static Font fnt = new Font("Helvetica", Font.BOLD, 12);
	protected static FontMetrics fntmet=Toolkit.getDefaultToolkit().getFontMetrics(fnt);
	protected static int fntWidth = fntmet.charWidth('C');
	protected static int fntXOffset = fntWidth/2;
	protected static int fntHeight = fntmet.getHeight();

	protected static int topSpacer = 1;
	protected static int letterHeight = fntHeight;
	protected static int letterSpacer = 3;
	protected static int tickHeight = 7;
	protected static int tickSpacer = 2;
	protected static int numHeight = fntHeight;
	protected static int numSpacer = 4;

	protected static int letterOffset = topSpacer;
	protected static int letterBaseline = topSpacer + letterHeight;
	protected static int tickOffset = letterBaseline + letterSpacer;
	protected static int numOffset = tickOffset + tickHeight + tickSpacer;
	protected static int numBaseline = numOffset + numHeight;

	public static int baseGlyphHeight = (
			topSpacer +
			letterHeight +
			letterSpacer +
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

	protected int read_length;
	protected String bString = baseString[4]; // default to -
	protected Color currBaseColor = baseColor[4]; // default to white
	protected String numString;
	protected boolean showBases = true;
	protected BaseConfidence calledBase;
	protected BaseConfidence nextBase;

	protected GlyphI sel_glyph;
	protected static Color sel_color = Color.white;

	public QualityBases(ReadConfidence read_conf) {
		this.setDrawOrder(DRAW_CHILDREN_FIRST);
		setReadConfidence(read_conf);
	}

	public void setReadConfidence(ReadConfidence read_conf) {
		this.read_conf = read_conf;
		read_length = read_conf.getReadLength();
		clearSelection();
	}

	public void draw(ViewI view) {
		int beg, end, i, j;
		Graphics g = view.getGraphics();
		Rectangle2D viewbox = view.getCoordBox();

		beg = (int)viewbox.x;
		end = (int)(viewbox.x + viewbox.width);
		if (end < coordbox.x ) { end++; }
		if (end >= read_length) { end = read_length - 1; }
		if (beg < 0) { beg = 0; }

		char theBase;
		double minview = viewbox.x;
		double maxview = viewbox.x + viewbox.width;
		boolean firstdisplayed = true;
		int firstbase, lastbase;
		firstbase = lastbase = Integer.MIN_VALUE;
		g.setFont(fnt);

		double avgBasesDrawn = viewbox.width;
		int increment;
		if  (avgBasesDrawn < 20) { increment = 2; }
		else if  (avgBasesDrawn < 50) { increment = 5; }
		else if  (avgBasesDrawn < 100) { increment = 10; }
		else if  (avgBasesDrawn < 300) { increment = 20; }
		else { increment = 50; }

		if ( increment > 10 ) {
			showBases = false;
		}
		else {
			showBases = true;
		}

		Rectangle2D visible_box = ((View)view).calcCoordBox();
		for (i=(int)visible_box.x; i<(int)(visible_box.x+visible_box.width); i++) {
			if (i>= read_length) {
				break;
			}
			if ((calledBase = read_conf.getBaseConfidenceAt(i)) != null) {
				baseCoordPoint.x = i;
				if ((baseCoordPoint.x >= minview) && (baseCoordPoint.x <= maxview))  {
					if (firstdisplayed) {
						firstbase = i;
						firstdisplayed = false;
					}
					lastbase = i;
					baseCoordPoint.y = coordbox.y;
					basePixelPoint =
						view.transformToPixels(baseCoordPoint, basePixelPoint);
					theBase = calledBase.getBase();

					if (theBase == 'A' || theBase == 'a') {
						bString = baseString[0];
						currBaseColor = baseColor[0];
					}
					else if (theBase == 'C' || theBase == 'c') {
						bString = baseString[1];
						currBaseColor = baseColor[1];
					}
					else if (theBase == 'G' || theBase == 'g') {
						bString = baseString[2];
						currBaseColor = baseColor[2];
					}
					else if (theBase == 'T' || theBase == 't') {
						bString = baseString[3];
						currBaseColor = baseColor[3];
					}
					else {
						bString = baseString[4];
						currBaseColor = baseColor[4];
					}

					if (showBases) {
						g.setColor(currBaseColor);
						g.drawString( bString, basePixelPoint.x,
								basePixelPoint.y + letterBaseline );
					}
				}
			}
		}

		// Drawing base numbers along the axis

		if (firstbase%increment != 0) {
			firstbase = firstbase + increment - firstbase%increment;
		}
		g.setColor(numColor);
		for (i=firstbase; i<=lastbase; i+=increment) {
			baseCoordPoint.x = i;
			baseCoordPoint.y = coordbox.y;
			basePixelPoint =
				view.transformToPixels(baseCoordPoint, basePixelPoint);

			// a little embellishment (need to get rid of hardwiring though)
			g.drawLine( basePixelPoint.x+3,
					basePixelPoint.y + tickOffset,
					basePixelPoint.x+3,
					basePixelPoint.y + tickOffset + tickHeight) ;

			g.drawString( String.valueOf(i), basePixelPoint.x,
					basePixelPoint.y + numBaseline );
		}
	}

	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return  isVisible?pixel_hitbox.intersects(pixelbox):false;
	}

	public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
		return isVisible?coord_hitbox.intersects(coordbox):false;
	}

	public void clearSelection() {
		if (sel_glyph != null) {
			removeChild(sel_glyph);
			sel_glyph = null;
		}
	}

	public void select(int base) {
		this.select(base, base);
	}

	public void deselect(int base) {
		this.deselect(base, base);
	}

	//  selection is inclusive of start and end
	public void select(int start, int end) {
		if (sel_glyph == null) {
			sel_glyph = new OutlineRectGlyph();
			sel_glyph.setColor(sel_color);
			addChild(sel_glyph, 0);
		}
		Rectangle2D cb = getCoordBox();
		sel_glyph.setCoords(start,cb.y,end-start + 1,cb.height);
	}

	//  selection is inclusive of begbase and endbase
	public void deselect(int begbase, int endbase) {
	}

}
