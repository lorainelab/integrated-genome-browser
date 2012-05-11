
package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.event.NeoCanvasDragEvent;
import com.affymetrix.genoviz.event.NeoCanvasDragListener;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 * @author hiralv
 */
public class DragScrollMonitor 
		implements NeoConstants, MouseListener, MouseMotionListener {

	protected boolean started;
	protected int xorigin, yorigin;
	protected int xdirection, ydirection, width, height;
	protected int startEventID, stretchEventID, endEventID;
	protected CopyOnWriteArraySet<NeoCanvasDragListener> listeners;
	protected int startClickCount;

	public DragScrollMonitor(){
		listeners = new CopyOnWriteArraySet<NeoCanvasDragListener>();
		started = false;
		startEventID = MouseEvent.MOUSE_PRESSED;
		stretchEventID = MouseEvent.MOUSE_DRAGGED;
		endEventID = MouseEvent.MOUSE_RELEASED;
		startClickCount = 1;
	}

	protected void start(int x, int y) {
		xorigin = x;
		yorigin = y;
		started = true;

	}

	protected void drag(int x, int y) {
		// direction constants are from com.affymetrix.genoviz.util.NeoConstants
		if (xorigin < x) {
			xdirection = WEST;
		} else if (xorigin > x) {
			xdirection = EAST;
		} else {
			xdirection = NONE;
		}
		width = Math.abs(xorigin-x);

		if (yorigin < y) {
			ydirection = NORTH;
		} else if (yorigin > y) {
			ydirection = SOUTH;
		} else {
			ydirection = NONE;
		}
		height = Math.abs(yorigin-y);
		start(x,y);
	}
	
	public void end() {
		started = false;
	}
	
	protected void heardEvent(MouseEvent evt){
		int id = evt.getID();
		int clickCount = evt.getClickCount();
		int x = evt.getX();
		int y = evt.getY();
		if (id == startEventID && clickCount == startClickCount && !started) {
			// && modifiers = startEventMask && evt.key = startEventKey
			start(x, y);
		}
		else if (id == stretchEventID && started) {
			// && modifiers = stretchEventMask && evt.key = stretchEventKey
			drag(x, y);
			if (listeners.size() > 0) {
				NeoCanvasDragEvent rbevent =
					new NeoCanvasDragEvent(this, xdirection, ydirection, width, height);
				processEvent(rbevent);
			}
		}
		else if (id == endEventID && started) {
			// && modifiers = endEventMask && evt.key = endEventKey
			end();	
		}
	}

	protected void processEvent(NeoCanvasDragEvent evt) {
		for (NeoCanvasDragListener listener :listeners)  {
			listener.canvasDragEvent(evt);
		}
	}

	public void addDragListener(NeoCanvasDragListener listener)  {
		listeners.add(listener);
	}

	public void removeDragListener(NeoCanvasDragListener listener)  {
		listeners.remove(listener);
	}

	public void mousePressed(MouseEvent e)  { heardEvent(e); }
	public void mouseReleased(MouseEvent e) { heardEvent(e); }
	public void mouseDragged(MouseEvent e)  { heardEvent(e); }

	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mouseMoved(MouseEvent e) { }

}
