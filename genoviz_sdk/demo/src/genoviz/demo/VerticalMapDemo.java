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

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.widget.VisibleRange;
import javax.swing.JScrollBar;

/**
 *  Demonstrates using the vertical map option
 *  WARNING: currently many glyphs do not display correctly on vertical maps!
 *
 * @version $Id$
 */
public class VerticalMapDemo extends Applet {
	Panel panel1, panel2;

	public void init() {
		panel1 = testMap(NeoConstants.HORIZONTAL);
		//panel2 = testMap(NeoMap.VERTICAL);
		this.setLayout(new GridLayout(1,2,10,0));
		this.add(panel1);
		//this.add(panel2);

	}

	public Panel testMap(int orient) {
		final NeoMap map = new NeoMap();
		final VisibleRange selectedRange = new VisibleRange();

		map.setSelectionEvent(NeoMap.ON_MOUSE_DOWN);
		map.setMapRange(0, 10000);
		map.setMapOffset(-100, 100);
		AxisGlyph ax = map.addAxis(0);

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

		JScrollBar xzoomer = new JScrollBar(JScrollBar.VERTICAL);
		map.setZoomer(NeoMap.X, xzoomer);
		JScrollBar yzoomer = new JScrollBar(JScrollBar.HORIZONTAL);
		map.setZoomer(NeoMap.Y, yzoomer);

		NeoPanel map_pan = new NeoPanel();
		map_pan.setLayout(new BorderLayout());
		map_pan.add("Center", map);
		map_pan.add("West", xzoomer);
		map_pan.add("North", yzoomer);

		Shadow hairline = new Shadow( map, orient );
		selectedRange.addListener( hairline );
		hairline.label.setFont(new Font("Courier", Font.PLAIN, 20));

		if (orient==NeoConstants.HORIZONTAL) {
			map.addMouseListener( new MouseAdapter() {
				@Override
				public void mouseReleased( MouseEvent e ) {
					selectedRange.setSpot( ((NeoMouseEvent)e).getCoordX() );
				}
			});
		}
		else {
			map.addMouseListener( new MouseAdapter() {
				@Override
				public void mouseReleased( MouseEvent e ) {
					selectedRange.setSpot( ((NeoMouseEvent)e).getCoordY() );
				}
			});
		}


		final int axisID = (orient==NeoConstants.HORIZONTAL) ? NeoMap.X : NeoMap.Y;
		NeoRangeListener zoomMidPointSetter = new NeoRangeListener() {
			public void rangeChanged( NeoRangeEvent e ) {
				double midPoint = ( e.getVisibleEnd() + e.getVisibleStart() ) / 2.0;
				map.setZoomBehavior(axisID, NeoMap.CONSTRAIN_COORD, midPoint );
				map.updateWidget();
			}
		};
		selectedRange.addListener( zoomMidPointSetter );

		return map_pan;
	}

}
