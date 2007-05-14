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

package com.affymetrix.genoviz.awt;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.event.PaintEvent;

import com.affymetrix.genoviz.event.NeoPaintEvent;
import com.affymetrix.genoviz.event.NeoPaintListener;

/**
 * Extends java.awt.Container (via NeoBufferedComponent)
 * to treat canvas painting as an event
 * that can be listened for (by a NeoPaintListener),
 * and to provide double buffering
 */
public class NeoCanvas extends NeoBufferedComponent  {
  public boolean DEBUG_BOUNDS = false;
  public boolean debug = false;
  protected boolean dragging_outside = false;
  protected boolean grab_focus_on_click = true;
  protected boolean focus_traversable = true;
  protected Vector<NeoPaintListener> paint_listeners = new Vector<NeoPaintListener>();


  /**
   * Paints the component,
   * notifying listeners
   * via a NeoCanvasEvent.
   *
   * @param g the specified Graphics object
   * @see #update
   */
  public void directPaint(Graphics g)  {
      if (debug)  {
          System.out.println("----------- in NeoCanvas.directPaint() -----------");
      }
    Dimension d = getSize();
    if (paint_listeners.size() > 0) {
      Rectangle cliprect = g.getClipBounds();
      Rectangle paintrect = null;
      // sometimes after a resize the cliprect starts out as null, so 
      //    checking to avoid NullPointerExceptions
      if (cliprect != null)  {
        paintrect = new Rectangle(cliprect.x, cliprect.y, 
                                  cliprect.width, cliprect.height);
      }
      NeoPaintEvent e = new NeoPaintEvent(this, paintrect, g);
      postPaintEvent(e);
    }
    if (debug)  {
      System.out.println("------------ leaving NeoCanvas.directPaint() ------------");
    }
    if (DEBUG_BOUNDS) {
      g.setColor(Color.red);
      g.drawRect(0, 0, d.width-1, d.height-1);
    }
  }

  /**
   * Lets all the listeners know that this has been painted.
   *
   * @param e an event to pass to them all.
   */
  public void postPaintEvent(NeoPaintEvent e) {
    int id = e.getID();
    NeoPaintListener pl;
    for (int i=0; i<paint_listeners.size(); i++) {
      pl = (NeoPaintListener)paint_listeners.elementAt(i);
      pl.componentPainted(e);  // assume for now event id is always PAINT
    }
  }


  /**
   * Adds the specified listener to those receiving notification
   * of painting this NeoCanvas.
   *
   * @param pl the listener
   */
  public void addNeoPaintListener(com.affymetrix.genoviz.event.NeoPaintListener pl) {
    if (!paint_listeners.contains(pl)) {
      paint_listeners.addElement(pl);
    }
  }

  /**
   * Removes the specified event listener
   * so it no longer receives notification of events
   * from this NeoCanvas.
   *
   * @param pl the listener
   */
  public void removeNeoPaintListener(com.affymetrix.genoviz.event.NeoPaintListener pl) {
    paint_listeners.removeElement(pl);
  }

  /**
   * gets the objects
   * that are listening for this NeoCanvas being painted.
   *
   * @return a vector of all the NeoPaintListeners to this NeoCanvas.
   */
  public Vector getNeoPaintListeners() {
    return paint_listeners;
  }

  /**
   * @deprecated use NeoBufferedComponent.setDoubleBuffered() instead.
   *
   * @see NeoBufferedComponent#setDoubleBuffered
   */
  public void isBuffered(boolean buffered) {
    setDoubleBuffered(buffered);
  }

  /**
   * @deprecated use NeoBufferedComponent.isDoubleBuffered() instead.
   *
   * @see NeoBufferedComponent#isDoubleBuffered
   */
  public boolean getBuffered() {
    return isDoubleBuffered();
  }


  /**
   * Overriding to ensure that NeoCanvas will be included
   * in Tab and Shift-Tab keyboard focus traversal.
   */
  public boolean isFocusTraversable() {
    return focus_traversable;
  }

  /**
   *  Overriding processMouseEvent to give NeoCanvas the focus on 
   *  mouse press over it
   */
  public void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_PRESSED && grab_focus_on_click) {
      this.requestFocus();
    }
    super.processMouseEvent(e);
  }

  /**
   * Sets whether or not this component is to grab the focus
   * when the mouse is clicked on it.
   */
  public void setGrabFocusOnClick(boolean b) {
    grab_focus_on_click = b;
  }

  /**
   * Gets whether or not this component is to grab the focus
   * when the mouse is clicked on it.
   */
  public boolean getGrabFocusOnClick() {
    return grab_focus_on_click;
  }
}
