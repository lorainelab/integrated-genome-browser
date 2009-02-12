/**
 *   Copyright (c) 1998-2007 Affymetrix, Inc.
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

package com.affymetrix.genoviz.util;

import java.util.*;
import java.awt.*;
import java.awt.print.*;
import javax.swing.*;

import java.util.List;

import com.affymetrix.genoviz.awt.NeoBufferedComponent;

/**
 * Prints a component.
 * Currently attempts to scale the Component to fit on a single page
 * Also tries to improve printing quality for Swing and GenoViz components,
 *   or components that contain Swing or GenoViz components.
 *
 * <p> Turns off double-buffering in Swing components.
 * Turns off double-buffering in GenoViz components.
 */
public class ComponentPagePrinter implements Printable {
	static boolean DEBUG = false;
	boolean DISABLE_SWING_BUFFERING = true;
	boolean DISABLE_NEO_BUFFERING = true;

	// The component to be printed.
	Component comp;

	public ComponentPagePrinter(Component c) {
		comp = c;
	}

	public void print(int orientation) throws PrinterException {
		if (orientation != PageFormat.LANDSCAPE &&
				orientation != PageFormat.PORTRAIT &&
				orientation != PageFormat.REVERSE_LANDSCAPE) {
			throw new PrinterException("Orientation not recognized");
				}

		PrinterJob pjob = PrinterJob.getPrinterJob();
		PageFormat preformat = pjob.defaultPage();
		preformat.setOrientation(orientation);

		// show the page format dialog, allow user changes to format
		PageFormat postformat = pjob.pageDialog(preformat);
		// if preformat is identical to (the same object as) postformat,
		// that means that the user pressed the "Cancel" button.
		if (preformat != postformat) {
			pjob.setPrintable(this, postformat);

			// show the print dialog, allow user to change options or cancel
			if (pjob.printDialog()) {
				pjob.print();
			}
		}
	}

	public void print() throws PrinterException {
		print(PageFormat.LANDSCAPE);
	}

	/**
	 * callback method called by PrinterJob
	 * in response to PrinterJob.print() call.
	 */
	public int print(Graphics g, PageFormat format, int page_index) {
		// must tell the PrintJob to stop printing after first page
		if (page_index > 0) {
			return Printable.NO_SUCH_PAGE;
		}

		//int orientation = format.getOrientation();

		// get the bounds of the component
		Dimension dim = comp.getSize();
		double cHeight = dim.getHeight();
		double cWidth = dim.getWidth();

		// get the bounds of the printable area
		double pHeight = format.getImageableHeight();
		double pWidth = format.getImageableWidth();

		double pXStart = format.getImageableX();
		double pYStart = format.getImageableY();

		double xRatio = pWidth / cWidth;
		double yRatio = pHeight / cHeight;
		if (DEBUG) {
			System.out.println("Page: width = " + pWidth + ", height = " + pHeight);
		}

		Graphics2D g2 = (Graphics2D) g;
		g2.translate(pXStart, pYStart);
		g2.scale(xRatio, yRatio);

		List<NeoBufferedComponent> neo_comps = new ArrayList<NeoBufferedComponent>();
		List<Boolean> buf_states = new ArrayList<Boolean>();
		if (DISABLE_NEO_BUFFERING) {
			// turning double buffering off in NeoBufferedComponents
			turnNeoBufferingOff(comp, neo_comps, buf_states);
		}

		RepaintManager currentManager = RepaintManager.currentManager(comp);
		if (DISABLE_SWING_BUFFERING) {
			// turning double buffering off in Swing components
			currentManager.setDoubleBufferingEnabled(false);
		}

		comp.paint(g2);

		if (DISABLE_SWING_BUFFERING) {
			// turning double buffering back on in Swing components
			currentManager.setDoubleBufferingEnabled(true);
		}

		if (DISABLE_NEO_BUFFERING) {
			restoreNeoBuffering(neo_comps, buf_states);
		}

		return Printable.PAGE_EXISTS;
	}


	// recursively descend into children, searching for NeoBufferedComponents,
	//    recording their double-buffering status (in neo_comps and buf_states vectors),
	//    then turning double buffering off
	public static void turnNeoBufferingOff(Component com, List<NeoBufferedComponent> neo_comps, List<Boolean> buf_states) {
		if (com instanceof NeoBufferedComponent) {
			NeoBufferedComponent nbc = (NeoBufferedComponent)com;
			boolean buffered = nbc.isDoubleBuffered();

			if (DEBUG)  {
				System.out.println("turning off buffering in " + nbc +
						", previous buffered: " + buffered);
			}

			neo_comps.add(nbc);
			buf_states.add(new Boolean(buffered));
			nbc.setDoubleBuffered(false);
		}
		if (com instanceof Container) {
			Container parent = (Container)com;
			for (int i=0; i<parent.getComponentCount(); i++) {
				Component child = parent.getComponent(i);
				turnNeoBufferingOff(child, neo_comps, buf_states);
			}
		}
		return;
	}

	// restore buffer state of NeoBufferedComponents,
	//   based on neo_comps and buf_states Vectors
	public static void restoreNeoBuffering(List<NeoBufferedComponent> neo_comps, List<Boolean> buf_states) {
		for (int i=0; i<neo_comps.size(); i++) {
			NeoBufferedComponent nbc = neo_comps.get(i);
			boolean buffered = buf_states.get(i).booleanValue();
			if (DEBUG)  {
				System.out.println("restoring buffering in " + nbc +
						", buffer state: " + buffered);
			}
			nbc.setDoubleBuffered(buffered);
		}
	}

}
