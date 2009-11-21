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

package com.affymetrix.genoviz.bioviews;

//import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import com.affymetrix.genoviz.awt.NeoCanvas;
import com.affymetrix.genoviz.event.NeoDragEvent;
import com.affymetrix.genoviz.event.NeoDragListener;
import com.affymetrix.genoviz.event.NeoTimerEvent;
import com.affymetrix.genoviz.event.NeoTimerListener;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Dimension;

@SuppressWarnings(value="deprecation")
public class DragMonitor
	implements NeoConstants, MouseListener, MouseMotionListener, NeoTimerListener {

	NeoCanvas can;
	List<NeoDragListener> listeners = new CopyOnWriteArrayList<NeoDragListener>();
	boolean already_dragging_outside = false;

	protected NeoTimerEventClock timer = null;
	protected int initial_delay = 250;
	protected int timer_interval = 100;

	protected int time_count = 0;

  public DragMonitor(NeoCanvas can) {
		this.can = can;
		can.addMouseListener(this);
		can.addMouseMotionListener(this);
	}

	/** implementing MouseListener interface */
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) {
		if (timer != null) {
			timer.stop();
			timer = null;
		}
		already_dragging_outside = false;
	}

	/** implementing MouseMotionListener interface */
	public void mouseMoved(MouseEvent e) { }
	public void mouseDragged(MouseEvent evt) {
		Dimension dim = can.getSize();
		int x = evt.getX();
		int y = evt.getY();
		if ((!already_dragging_outside) &&
				(x < 0 || x > dim.width ||
				 y < 0 || y > dim.height)) {
			if (timer != null) { timer.stop(); }
			already_dragging_outside = true;

			int direction;
			// direction constants are from com.affymetrix.genoviz.util.NeoConstants
			if (x < 0) { direction = WEST; }
			else if (y < 0) { direction = NORTH; }
			else if (x > dim.width) { direction = EAST; }
			else if (y > dim.height) { direction = SOUTH; }
			else { direction = NONE; }
			Integer dirobj = new Integer(direction);

			timer = new NeoTimerEventClock(initial_delay, timer_interval, dirobj);
			timer.addTimerListener(this);
			timer.start();
				 }
		else if (already_dragging_outside &&
				(x > 0 && x < dim.width &&
				 y > 0 && y < dim.height)) {
			already_dragging_outside = false;
			if (timer != null) {
				timer.stop();
				timer = null;
			}
				 }
	}

	/**
	 *  If NeoTimerEventClock's lazy_event_posting is turned on, calls to
	 *  heardTimerEvent() should be running on the event dispatch thread,
	 *  and therefore be running synchronously with AWT event and paint calls
	 */
	public void heardTimerEvent(NeoTimerEvent evt) {
		Object arg = evt.getArg();
		if (!(arg instanceof Integer)) { return; }
		int direction = ((Integer)arg).intValue();
		NeoDragEvent new_event = new NeoDragEvent(this, direction);
		for (int i=0; i<listeners.size(); i++) {
			listeners.get(i).heardDragEvent(new_event);
		}
		time_count++;
	}

	public void addDragListener(NeoDragListener listener) {
		listeners.add(listener);
	}

	public void removeDragListener(NeoDragListener listener) {
		listeners.remove(listener);
	}

	public List<NeoDragListener> getDragListeners() {
		return listeners;
	}

}
