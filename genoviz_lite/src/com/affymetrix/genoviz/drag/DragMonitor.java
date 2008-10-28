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

package com.affymetrix.genoviz.drag;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.util.NeoConstants;
import java.util.concurrent.CopyOnWriteArrayList;

//TODO: consider deleting.  Currently unused.
public class DragMonitor
implements MouseListener, MouseMotionListener, NeoTimerListener {

  final NeoCanvas can;
  final List<NeoDragListener> listeners = new CopyOnWriteArrayList<NeoDragListener>();
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
  @Override
  public void mouseClicked(MouseEvent e) { }
  @Override
  public void mouseEntered(MouseEvent e) { }
  @Override
  public void mouseExited(MouseEvent e) { }
  @Override
  public void mousePressed(MouseEvent e) { }

  @SuppressWarnings(value="deprecation")
  @Override
  public void mouseReleased(MouseEvent e) {
    if (timer != null) {
      timer.stop();
      timer = null;
    }
    already_dragging_outside = false;
  }

  /** implementing MouseMotionListener interface */
  @Override
  public void mouseMoved(MouseEvent e) { }

  @SuppressWarnings(value="deprecation")
  @Override
  public void mouseDragged(MouseEvent evt) {
    Dimension dim = can.getSize();
    int x = evt.getX();
    int y = evt.getY();
    if ((!already_dragging_outside) &&
        (x < 0 || x > dim.width ||
         y < 0 || y > dim.height)) {
      if (timer != null) { timer.stop(); }
      already_dragging_outside = true;

      NeoConstants.Direction direction;
      // direction constants are from com.affymetrix.genoviz.util.NeoConstants
      if (x < 0) { direction = NeoConstants.Direction.LEFT; }
      else if (y < 0) { direction = NeoConstants.Direction.UP; }
      else if (x > dim.width) { direction = NeoConstants.Direction.RIGHT; }
      else if (y > dim.height) { direction = NeoConstants.Direction.DOWN; }
      else { direction = NeoConstants.Direction.NONE; }

      timer = new NeoTimerEventClock(initial_delay, timer_interval, direction);
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

  /*
   *  If NeoTimerEventClock's lazy_event_posting is turned on, calls to
   *  heardTimerEvent() should be running on the event dispatch thread,
   *  and therefore be running synchronously with AWT event and paint calls.
   */
  @Override
  public void heardTimerEvent(NeoTimerEvent evt) {
    NeoDragEvent new_event = new NeoDragEvent(this, (NeoConstants.Direction) evt.getArg());
    for (NeoDragListener listener : listeners) {
      listener.heardDragEvent(new_event);
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
