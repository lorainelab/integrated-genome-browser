/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.tiers.AffyTieredMap;

import java.awt.Adjustable;
import java.awt.event.*;

final class SeqMapViewActionListener implements ActionListener {

	private final static String ZOOM_OUT_FULLY = "ZOOM_OUT_FULLY";
	private final static String ZOOM_OUT_X = "ZOOM_OUT_X";
	private final static String ZOOM_IN_X = "ZOOM_IN_X";
	private final static String ZOOM_OUT_Y = "ZOOM_OUT_Y";
	private final static String ZOOM_IN_Y = "ZOOM_IN_Y";
	private final static String SCROLL_UP = "SCROLL_UP";
	private final static String SCROLL_DOWN = "SCROLL_DOWN";
	private final static String SCROLL_LEFT = "SCROLL_LEFT";
	private final static String SCROLL_RIGHT = "SCROLL_RIGHT";
	private final static String ZOOM_TO_SELECTED = "Zoom to selected";
	private final static String[] commands = {ZOOM_OUT_FULLY,
		ZOOM_OUT_X, ZOOM_IN_X, ZOOM_OUT_Y, ZOOM_IN_Y,
		SCROLL_UP, SCROLL_DOWN, SCROLL_RIGHT, SCROLL_LEFT};
	private final AffyTieredMap seqmap;
	private final SeqMapView gviewer;
	
	SeqMapViewActionListener(SeqMapView gviewer) {

		this.gviewer = gviewer;
		seqmap = gviewer.seqmap;

		for (String command : commands) {
			MenuUtil.addAccelerator(gviewer, this, command);
		}
		new GenericAction("Zoom out horizontally", null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_OUT_X);
			}
		};
		new GenericAction("Zoom in horizontally", null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_IN_X);
			}
		};
		new GenericAction("Zoom out vertically", null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_OUT_Y);
			}
		};
		new GenericAction("Zoom in vertically", null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_IN_Y);
			}
		};
		new GenericAction("Home Position", "Zoom out fully", null, KeyEvent.VK_UNDEFINED, null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_OUT_FULLY);
			}
		};
		
		new GenericAction(ZOOM_TO_SELECTED, null) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_TO_SELECTED);
			}
		};
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		//System.out.println("SeqMapView received action event "+command);
		doAction(command);
	}

	private void doAction(String command) {
//    if (command.equals(gviewer.zoomtoMI.getText())) {
//      gviewer.zoomToSelections();
//    }
		if (command.equals(gviewer.selectParentMI.getText())) {
			gviewer.selectParents();

		} else if (command.equals(ZOOM_OUT_FULLY)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.X);
			adj.setValue(adj.getMinimum());
			adj = seqmap.getZoomer(NeoMap.Y);
			adj.setValue(adj.getMinimum());
			//map.updateWidget();
		} else if (command.equals(ZOOM_OUT_X)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.X);
			adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(ZOOM_IN_X)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.X);
			adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(ZOOM_OUT_Y)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.Y);
			adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(ZOOM_IN_Y)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.Y);
			adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(SCROLL_LEFT)) {
			int[] visible = seqmap.getVisibleRange();
			seqmap.scroll(NeoAbstractWidget.X, visible[0] - (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		} else if (command.equals(SCROLL_RIGHT)) {
			int[] visible = seqmap.getVisibleRange();
			seqmap.scroll(NeoAbstractWidget.X, visible[0] + (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		} else if (command.equals(SCROLL_UP)) {
			int[] visible = seqmap.getVisibleOffset();
			seqmap.scroll(NeoAbstractWidget.Y, visible[0] - (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		} else if (command.equals(SCROLL_DOWN)) {
			int[] visible = seqmap.getVisibleOffset();
			seqmap.scroll(NeoAbstractWidget.Y, visible[0] + (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		}
	}
}
