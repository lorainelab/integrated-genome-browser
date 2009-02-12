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

package genoviz.demo;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.awt.NeoScrollbar;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.glyph.AxisGlyph;

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.glyph.*;

/**
 *  Demonstrates using the vertical map option
 *  WARNING: currently many glyphs do not display correctly on vertical maps!
 */
public class VerticalMapDemo extends Applet {
	Panel panel1, panel2;

	public void init() {
		panel1 = testMap(NeoMap.HORIZONTAL);
		//panel2 = testMap(NeoMap.VERTICAL);
		this.setLayout(new GridLayout(1,2,10,0));
		this.add(panel1);
		//this.add(panel2);

	}

	public Panel testMap(int orient) {
		final NeoMap map = new NeoMap();
		final VisibleRange selectedRange = new VisibleRange();

		map.setSelectionEvent(map.ON_MOUSE_DOWN);
		map.setMapRange(0, 10000);
		map.setMapOffset(-100, 100);
		AxisGlyph ax = (AxisGlyph)map.addAxis(0);

		map.configure("-glyphtype FillRectGlyph -color green" +
				" -offset 40 -width 5");
		map.addItem(5000, 9000);
		map.addItem(6000, 7000);

		map.configure("-offset -40 -color blue");
		map.addItem(1000, 3000);
		map.addItem(2000, 4000);

		map.configure("-glyphtype PointedGlyph -color magenta" +
				" -offset -40 -width 5");
		map.addItem(4000, 1000);
		map.addItem(6000, 9000);

		NeoScrollbar xzoomer = new NeoScrollbar(NeoScrollbar.VERTICAL);
		map.setZoomer(map.X, xzoomer);
		NeoScrollbar yzoomer = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
		map.setZoomer(map.Y, yzoomer);

		NeoPanel map_pan = new NeoPanel();
		map_pan.setLayout(new BorderLayout());
		map_pan.add("Center", map);
		map_pan.add("West", xzoomer);
		map_pan.add("North", yzoomer);

		Shadow hairline = new Shadow( map, orient );
		selectedRange.addListener( hairline );
		hairline.label.setFont(new Font("Courier", Font.PLAIN, 20));

		if (orient==NeoMap.HORIZONTAL) {
			map.addMouseListener( new MouseAdapter() {
				public void mouseReleased( MouseEvent e ) {
					selectedRange.setSpot( ((NeoMouseEvent)e).getCoordX() );
				}
			});
		}
		else {
			map.addMouseListener( new MouseAdapter() {
				public void mouseReleased( MouseEvent e ) {
					selectedRange.setSpot( ((NeoMouseEvent)e).getCoordY() );
				}
			});
		}


		final int axisID = (orient==NeoMap.HORIZONTAL) ? map.X : map.Y;
		NeoRangeListener zoomMidPointSetter = new NeoRangeListener() {
			public void rangeChanged( NeoRangeEvent e ) {
				double midPoint = ( e.getVisibleEnd() + e.getVisibleStart() ) / 2.0;
				map.setZoomBehavior(axisID, map.CONSTRAIN_COORD, midPoint );
				map.updateWidget();
			}
		};
		selectedRange.addListener( zoomMidPointSetter );

		return map_pan;
	}

}
