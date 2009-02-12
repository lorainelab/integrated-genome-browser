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

package genoviz.tutorial;

import com.affymetrix.genoviz.awt.NeoScrollbar;

import java.awt.*;
import java.awt.event.*;

public class SimpleMap3 extends SimpleMap2 {

	Adjustable zoomer, scroller;

	public SimpleMap3() {
		zoomer = new NeoScrollbar(NeoScrollbar.VERTICAL);
		add("West", (Component)zoomer);
		map.setRangeZoomer(zoomer);
		scroller = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
		add("South", (Component)scroller);
		map.setRangeScroller(scroller);
	}

	public static void main (String argv[]) {
		SimpleMap0 me = new SimpleMap3();
		Frame f = new Frame("GenoViz");
		f.add(me, BorderLayout.CENTER);
		me.addFileMenuItems(f);

		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				Window w = (Window) e.getSource();
				w.dispose();
			}
			public void windowClosed( WindowEvent e ) {
				System.exit( 0 );
			}
		} );

		f.pack();
		f.setBounds(20, 40, 400, 500);
		f.show();
	}

}
