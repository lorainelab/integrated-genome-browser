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
import java.util.*;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.BasicGraphGlyph;
import com.affymetrix.genoviz.glyph.BasicImageGlyph;
import java.awt.BorderLayout;
import java.awt.Image;
import javax.swing.JScrollBar;

public class GraphDemo extends Applet {
	NeoMap map;
	JScrollBar xzoomer;
	Vector selected = new Vector();

	public void init() {
		map = new NeoMap(true, false);  // no internal vertical scroller
		map.setMapOffset(-200, 200);
		map.setMapRange(0, 1000);
		map.addAxis(0);

		xzoomer = new JScrollBar(JScrollBar.VERTICAL);

		NeoPanel widg_pan = new NeoPanel();
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", map);
		widg_pan.add("West", xzoomer);
		setLayout(new BorderLayout());
		add("Center", widg_pan);

		map.setZoomer(map.X, xzoomer);

		double xcoords[] = { 100, 200, 300, 400, 500, 600, 700, 800, 900 };
		double ycoords[] = { -50, -25,  25, 100,  50, 175, -10,  50,  74 };

		map.configure("-glyphtype BasicGraphGlyph -color red -offset 0 " +
				"-width 200 -packer null");
		BasicGraphGlyph sg = (BasicGraphGlyph)map.addItem(0, 1000);
		sg.setPointCoords(xcoords, ycoords);

		Image img1 = this.getImage(getCodeBase(), "./images/red-ball.gif");
		this.prepareImage(img1, this);

		Image img2 = this.getImage(getCodeBase(), "./images/alphahelix.gif");
		this.prepareImage(img2, this);

		map.configure("-glyphtype com.affymetrix.genoviz.glyph.BasicImageGlyph "
				+ " -offset 50 -width 10");

		// Put a an alpha helix image (or images, if tiled) from 600 to 700.
		BasicImageGlyph ig;
		ig = (BasicImageGlyph)map.addItem(600, 700);
		ig.setImage(img2, this);
		ig.setPrimaryFill(ig.TILED);
		ig.setSecondaryFill(ig.NONE);

		// Put a red ball at 600.
		map.configure("-offset 0");
		ig = (BasicImageGlyph)map.addItem(600, 601);
		ig.setImage(img1, this);

		// Put a logo near the bottom center of the map.
		/*Image img3 = this.getImage(getCodeBase(), "./images/affymetrix_logo.gif");
		  map.configure("-offset 140");
		  ig = (BasicImageGlyph)map.addItem(0, 999);
		  ig.setImage(img3, this);*/
	}


}
