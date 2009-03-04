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

import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.event.NeoMouseEvent;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class SequenceMap1 extends SequenceMap0 implements NeoRangeListener {

	protected MouseListener mouser = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			if (e instanceof NeoMouseEvent) {
				neoMouseClicked((NeoMouseEvent)e);
			}
		}
	};

	public void neoMouseClicked(NeoMouseEvent theEvent) {
		// Let's see if we can scroll that NeoSeq.
		this.seq.makeResidueVisible((int)theEvent.getCoordX());
		this.seq.updateWidget();
	}

	public SequenceMap1() {
		super();
		this.seq.addRangeListener(this);
		this.map.addMouseListener(this.mouser);
	}

	public String getAppletInfo() {
		return "Simple Sequence Map - genoviz Software, Inc.";
	}

	public void rangeChanged(NeoRangeEvent theEvent) {
		int sbeg = (int)theEvent.getVisibleStart();
		// Center zooming at the beginning of the range.
		this.map.setZoomBehavior(
				this.map.X,
				this.map.CONSTRAIN_COORD,
				sbeg
				);
	}

	public static void main (String argv[]) {
		SequenceMap1 me = new SequenceMap1();
		Frame f = new Frame("GenoViz");
		f.add("Center", me);
		// me.addFileMenuItems(f);

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
		f.setBounds( 20, 40, 300, 250 );
		f.show();
	}

}
