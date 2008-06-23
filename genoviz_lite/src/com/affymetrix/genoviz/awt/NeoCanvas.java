/**
*   Copyright (c) 1998-2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.awt;

import com.affymetrix.genoviz.event.NeoPaintEvent;
import com.affymetrix.genoviz.event.NeoPaintListener;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Extends {@link Container}
 * to treat canvas painting as an event
 * that can be listened for (by a {@link NeoPaintListener}).
 */
public class NeoCanvas extends Container  {
  protected boolean grabFocusOnClick = true;
  protected boolean focusable = true;
  private final List<NeoPaintListener> paintListeners = new CopyOnWriteArrayList<NeoPaintListener>();

  /**
   * Paints the component,
   * notifying listeners
   * via a {@link NeoPaintEvent}.
   *
   * @param g the specified Graphics object
   * @see #update
   */
  @Override
  public void paint(Graphics g) {
    if (paintListeners.size() > 0) {
      final Rectangle cliprect = g.getClipBounds();
      final Rectangle paintrect;
      // sometimes after a resize the cliprect starts out as null, so
      //    checking to avoid NullPointerExceptions
      if (cliprect != null)  {
        paintrect = new Rectangle(cliprect.x, cliprect.y,
                                  cliprect.width, cliprect.height);
      } else {
        paintrect = new Rectangle(getSize());
      }
      final NeoPaintEvent e = new NeoPaintEvent(this, paintrect, (Graphics2D) g);
      postPaintEvent(e);
    }
  }

  /**
   * Lets all the listeners know that this has been painted.
   *
   * @param e an event to pass to them all.
   */
  public void postPaintEvent(NeoPaintEvent e) {
    for (NeoPaintListener npl : paintListeners) {
      npl.componentPainted(e);
    }
  }

  /**
   * Adds the specified listener to those receiving notification
   * of painting this NeoCanvas.
   *
   * @param pl the listener
   */
  public void addNeoPaintListener(NeoPaintListener pl) {
    if (!paintListeners.contains(pl)) {
      paintListeners.add(pl);
    }
  }

  /**
   * Removes the specified event listener
   * so it no longer receives notification of events
   * from this NeoCanvas.
   *
   * @param pl the listener
   */
  public void removeNeoPaintListener(NeoPaintListener pl) {
    paintListeners.remove(pl);
  }

  /**
   * Gets the objects
   * that are listening for this NeoCanvas being painted.
   *
   * @return a List of all the NeoPaintListeners to this NeoCanvas.
   */
  public List<NeoPaintListener> getNeoPaintListeners() {
    return paintListeners;
  }

  /**
   * Overriding to ensure that NeoCanvas will be included
   * in Tab and Shift-Tab keyboard focus traversal.
   */
  @Override
  public boolean isFocusable() {
    return focusable;
  }

  /**
   *  Overriding processMouseEvent to give NeoCanvas the focus on
   *  mouse press over it
   */
  @Override
  public void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_PRESSED && grabFocusOnClick) {
      this.requestFocus();
    }
    super.processMouseEvent(e);
  }

  /**
   * Sets whether or not this component is to grab the focus
   * when the mouse is clicked on it.
   */
  public void setGrabFocusOnClick(boolean b) {
    grabFocusOnClick = b;
  }

  /**
   * Gets whether or not this component is to grab the focus
   * when the mouse is clicked on it.
   */
  public boolean getGrabFocusOnClick() {
    return grabFocusOnClick;
  }
}
