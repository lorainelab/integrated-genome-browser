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

package com.affymetrix.genoviz.pseudoswing;

import java.awt.*;
import java.util.*;

/**
 * Applets don't have direct access to the AWT SystemEvent queue.
 * To work around this we call EventQueueCanvas.repaint()
 * on a per applet instance of this class.
 * The AWT deals with this by queuing a java.awt.PaintEvent
 * for the event dispatching thread
 * which is dispatched (Component.dispatchEvent()) the usual way.
 * Component.dispatchEvent() handles PaintEvents
 * by calling our update() method
 * (on the event dispatching thread)
 * which processes the {@link WrappedRunnableEvent}s stashed
 * in the runnableEvents vector.
 */
public class EventQueueCanvas extends Canvas  {

  /**
   *  Vector of all EventQueueCanvases
   *  the EventQueueCanvas constructor adds each EventQueueCanvas to this Vector
   */
  protected static Vector<EventQueueCanvas> allEventQueueCanvases = new Vector<EventQueueCanvas>();

  /**
   *  maps Containers to their associated EventQueueCanvas
   */
  protected static Hashtable<Container,EventQueueCanvas> containerToCanvas = new Hashtable<Container,EventQueueCanvas>();

  /**
   * Vector of all the WrappedRunnableEvents associated with this
   *   EventQueueCanvas (each added to it via addWrappedRunnableEvent());
   */
  protected Vector<WrappedRunnableEvent> runnableEvents = new Vector<WrappedRunnableEvent>();

  EventQueueCanvas(Container cont) {
    super();
    allEventQueueCanvases.addElement(this);
    containerToCanvas.put(cont, this);
    setBounds(0, 0, 1, 1);
  }

  /**
   * Remove the EventQueueCanvas associated with this container
   * and clear all of the *ToCanvas hash entries that point at it.
   * This still needs testing!!!
   */
  static void remove(Container cont) {
    EventQueueCanvas rc = (EventQueueCanvas)(containerToCanvas.get(cont));
    if (rc == null) { return; }
    allEventQueueCanvases.removeElement(rc);
    containerToCanvas.remove(cont);
    EventQueueCanvas anotherCanvas = getFirstVisible();

    // If there are still events, try to move them to another canvas?
    WrappedRunnableEvent[] events = rc.getWrappedRunnableEvents();
    if (events != null && events.length > 0 && anotherCanvas != null) {
      for (int counter = 0; counter < events.length; counter++) {
        WrappedRunnableEvent e = events[counter];
      }
      anotherCanvas.repaint();
    }
  }

  static EventQueueCanvas getFirstVisible() {
    EventQueueCanvas curcanvas;
    for (int i=0; i<allEventQueueCanvases.size(); i++) {
      curcanvas = (EventQueueCanvas)allEventQueueCanvases.elementAt(i);
      if (curcanvas.isVisible()) {
        return curcanvas;
      }
    }
    // if can't find a visible canvas, will return first one anyway...
    if (allEventQueueCanvases.size() > 0) {
      return (EventQueueCanvas)allEventQueueCanvases.elementAt(0);
    }
    // if no canvases available, return null
    return null;
  }

  /**
   * Return an array built from the runnableEvents vector or
   *    null if the vector is empty.
   * Why is this needed?  Because the update method needs
   *    a _copy_ of the vector so it doesn't have to hold the lock
   *    when calling WrappedRunnableEvent.run()
   */
  private synchronized WrappedRunnableEvent[] getWrappedRunnableEvents() {
    int n = runnableEvents.size();
    if (n == 0) { return null; }
    else {
      WrappedRunnableEvent[] rv = new WrappedRunnableEvent[n];
      for(int i = 0; i < n; i++) {
        rv[i] = (WrappedRunnableEvent)(runnableEvents.elementAt(i));
      }
      runnableEvents.removeAllElements();
      return rv;
    }
  }

  protected synchronized void addWrappedRunnableEvent(WrappedRunnableEvent e) {
    runnableEvents.addElement(e);
  }


  /***********   OVERRIDING STANDARD CANVAS EVENTS   ***********/

  /**
   * Process all of the WrappedRunnableEvents that have accumulated
   * since EventQueueCanvas.repaint() was called.
   */
  public void update(Graphics g) {
    WrappedRunnableEvent[] events = getWrappedRunnableEvents();
    WrappedRunnableEvent evt;
    if (events != null) {
      for(int i = 0; i < events.length; i++) {
        evt = events[i];
        evt.run();
      }
    }
  }

  /**
   * return true if there are events to be processed, regardless of
   *    whether the canvas is actually "showing" or not.
   * (need this because the AWT code that dispatches paint events
   *    will _not_ go to completsion if isShowing() returns false
   */
  public boolean isShowing() {
    return runnableEvents.size() > 0;
  }

  public Dimension getPreferredSize() {
    return new Dimension(1, 1);
  }

  /** null-op: for now do nothing if paint is called */
  public void paint(Graphics g) {
  }


}
