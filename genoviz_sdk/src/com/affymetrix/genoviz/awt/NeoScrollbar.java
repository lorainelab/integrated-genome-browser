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
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.bioviews.NeoTimerEventClock;
import com.affymetrix.genoviz.widget.NeoQualler;

/**
 *  A replacement for java.awt.Scrollbar.  NeoScrollBar provides better 
 *  cross-platform compatibility, which is critical for proper scrolling 
 *  and zooming in the com.affymetrix.genoviz.widget.* components.  Also, 
 *  emulation of JDK 1.1 Adjustable and AdjusmentEvent are 
 *  provided so NeoScrollBars can use delegation-based event handling 
 *  (pseudo-1.1 event handling) to notify listeners of adjusment events, 
 *  even with JDK 1.0 JVMs.
 */
public class NeoScrollbar extends NeoBufferedComponent
implements Adjustable, NeoTimerListener {

  private static final boolean debug = false;
  private static final boolean DEBUG_PAINT = false;

  protected boolean color_thumb_foreground = false;
  protected boolean gutter_scroll = true;
  protected boolean send_events = false; // see #setSendEvents(boolean);

  public static int HORIZONTAL = 0;
  public static int VERTICAL = 1;

  protected static final int NO_SELECTION = 2;
  protected static final int THUMB = 3;
  protected static final int LOWGUTTER = 4;
  protected static final int HIGHGUTTER = 5;
  protected static final int LOWARROW = 6;
  protected static final int HIGHARROW = 7;

  protected Vector listeners;
  protected int thumb_coord_value, thumb_coord_size, thumb_coord_max, thumb_coord_min;
  protected int thumb_pixel_value, thumb_pixel_size, thumb_pixel_max, thumb_pixel_min;
  protected int thumb_pixel_size_min = 15;
  protected int gutter_coord_width, gutter_pixel_width;
  protected int pixels_lengthwise, pixels_crosswise;
  protected float pixels_per_coord;   // scale
  protected int prev_event_id;
  protected int orientation;

  protected int lineIncrement = 1;
  protected int pageIncrement = 5;
  protected int offset = 16;
  protected int arrow_width = 16;

  protected Polygon low_arrow, high_arrow;

  protected Rectangle thumb_rect, low_arrow_rect, high_arrow_rect;

  protected int selected = NO_SELECTION;
  protected int prevx, prevy;

  protected boolean axis_scrollbar, offset_scrollbar;

  protected int repaint_tick, update_tick, paint_tick;

  // boolean for drawing thumb
  // -- if not enough room for thumb, this will be false
  protected boolean draw_thumb = true;
  protected boolean draw_arrows = true;
  protected boolean mouse_is_down = false;

  // fields for mouse-down-and-hold timer   GAH 2-21-98
  protected NeoTimerEventClock timer = null;
  protected int initial_delay = 500;
  protected int timer_interval = 50;
  protected int current_pixel;

  public NeoScrollbar(int orientation, int value,
                     int visible, int minimum, int maximum) {
    super();
    // ignore orientation for now, just assume horizontal
    thumb_rect = new Rectangle(0,0,0,0);
    this.orientation = orientation;
    setBackground(Color.lightGray);
    low_arrow = new Polygon();
    high_arrow = new Polygon();
    axis_scrollbar = true;
    offset_scrollbar = false;
    // We need something like this to restrict minimal size.
    super.setSize( getPreferredSize() );
    setValues(value, visible, minimum, maximum);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
  }

  public NeoScrollbar(int orientation) {
    this(orientation, 0, 10, 0, 100);
  }
  public NeoScrollbar() {
    this(VERTICAL, 0, 10, 0, 100);
  }
    

  /**
   * @deprecated
   * Use <code>setBounds(int, int, int, int)</code> (but override reshape).
   */
  public void reshape(int new_x, int new_y, int new_width, int new_height) {
                       
    super.reshape(new_x, new_y, new_width, new_height);
     Dimension size = getSize();
     low_arrow = new Polygon();
     high_arrow = new Polygon();

     if (orientation == HORIZONTAL) {
       pixels_lengthwise = size.width;
       pixels_crosswise = size.height;
       Point low_center = new Point(arrow_width/2, size.height/2);
       Point high_center = new Point(size.width-arrow_width/2, size.height/2);
       low_arrow.addPoint(low_center.x-2, low_center.y);
       low_arrow.addPoint(low_center.x+2, low_center.y+4);
       low_arrow.addPoint(low_center.x+2, low_center.y-4);
       high_arrow.addPoint(high_center.x+2, high_center.y);
       high_arrow.addPoint(high_center.x-2, high_center.y+4);
       high_arrow.addPoint(high_center.x-2, high_center.y-4);
     }
     else {
       pixels_lengthwise = size.height;
       pixels_crosswise = size.width;
       Point low_center = new Point(size.width/2, arrow_width/2);
       Point high_center = new Point(size.width/2, size.height-arrow_width/2);
       low_arrow.addPoint(low_center.x, low_center.y-2);
       low_arrow.addPoint(low_center.x+5, low_center.y+3);
       low_arrow.addPoint(low_center.x-5, low_center.y+3);
       high_arrow.addPoint(high_center.x, high_center.y+2);
       high_arrow.addPoint(high_center.x+4, high_center.y-2);
       high_arrow.addPoint(high_center.x-4, high_center.y-2);
     }

     thumb_pixel_min = arrow_width;
     thumb_pixel_max = pixels_lengthwise - arrow_width;
     setValues(thumb_coord_value, thumb_coord_size,
               thumb_coord_min, 
               thumb_coord_max + thumb_coord_size);
  }

  public void setValues(int value, int visible,
                        int minimum, int maximum)  {
    /*
      Here we try to enforce resonable values.
      Notice below where I had to deviate from java.awt.Scrollbar's behavior.
      As a consequence, java.awt.Scrollbar cannot be substituted
      for one of these.
      It is quite likely that other commercially available scroll bars
      will have the same checking and, hence, will also be unusable.
      Is this really what we want? -- Eric
      */

    if (visible < 0) {
      visible = 0;
    }
    if (maximum < minimum) {
      maximum = minimum;
    }
    if (value < minimum) { // like java.awt.Scrollbar
      value = minimum;
    }
    else if (maximum < value) {
      value = maximum;
    }

    boolean valueChanged = (value != thumb_coord_value);
    thumb_coord_value = value;
    thumb_coord_min = minimum;

    // making NeoScrollbars behave more like JDK1.1 and Swing 
    // scrollbars, so can mix and match
    thumb_coord_max = maximum - visible;

    thumb_coord_size = visible;
    gutter_pixel_width = pixels_lengthwise - 2*arrow_width;
    gutter_coord_width =
      (thumb_coord_max - thumb_coord_min) + thumb_coord_size;
    pixels_per_coord = (float)gutter_pixel_width / (float)gutter_coord_width;
    thumb_pixel_size =
      (int)(thumb_coord_size * pixels_per_coord);
    if (thumb_pixel_size < thumb_pixel_size_min) {
      thumb_pixel_size = thumb_pixel_size_min;
      pixels_per_coord = (float)(gutter_pixel_width - thumb_pixel_size) /
        (float)(thumb_coord_max - thumb_coord_min);
    }

    calcThumbPixels();
    // calcThumbPixels() will set draw_thumb and draw_arrows...

    repaint();   // just call repaint -- took out threaded repaint...

    // Notifying listeners seems to break the initial display of NeoQualler. -- Eric
    if (valueChanged && send_events) {
      AdjustmentEvent adjevt = new AdjustmentEvent(
        this, 0, AdjustmentEvent.TRACK, getValue());
      processAdjustmentEvent(adjevt);
    }

  }

  /**
   * Toggle the sending of events.
   * @param b If set to true, any time the position of the scrollbar is
   *  changed by a call to {@link #setValues(int, int, int, int)}
   *  a call will be made to {@link #processAdjustmentEvent(AdjustmentEvent)}.
   *  This is often a good thing, but it may break the initial display
   *  of {@link com.affymetrix.genoviz.widget.NeoQualler}.
   */
  public void setSendEvents(boolean b) {
    send_events = b;
  }

  public void setValue(int value) {
    setValues(value, thumb_coord_size, thumb_coord_min, 
              thumb_coord_max + thumb_coord_size);
  }

  public int getValue() {
    return thumb_coord_value;
  }

  public void calcThumbPixels() {
    thumb_pixel_value =
        (int)((thumb_coord_value - thumb_coord_min) * pixels_per_coord)
             + offset;
    if (orientation == HORIZONTAL) {
      thumb_rect.setBounds(thumb_pixel_value, 2,
                                 thumb_pixel_size, pixels_crosswise-4);
    }
    else {
      thumb_rect.setBounds(2, thumb_pixel_value,
                                 pixels_crosswise-4, thumb_pixel_size);
    }
    draw_thumb = (thumb_pixel_size <= gutter_pixel_width);
    draw_arrows = (gutter_pixel_width >= 0);
  }

  public int calcCoordPosition(int x, int y) {
    int coord_value;
    if (orientation == HORIZONTAL) {
      coord_value = 
        (int)((x-offset) / pixels_per_coord) + thumb_coord_min;
    }
    else {
      coord_value =
        (int)((y-offset) / pixels_per_coord) + thumb_coord_min;
    }
    return coord_value;
  }

  public void calcThumbValues() {
    if (orientation == HORIZONTAL) {
      thumb_coord_value =
        (int)((thumb_rect.x-offset) / pixels_per_coord) + thumb_coord_min;
    }
    else {
      thumb_coord_value =
        (int)((thumb_rect.y-offset) / pixels_per_coord) + thumb_coord_min;
    }

    /*
     * needed to add this as compensation for 9/30/97 max value bug fix, or 
     * else thumb_coord_value occasionally exceeds thumb_coord_max
     */
    if (thumb_coord_value > thumb_coord_max) {
      thumb_coord_value = thumb_coord_max;
    }

  }

  public void setUnitIncrement(int increment) {
    setLineIncrement(increment);
  }
  public int getUnitIncrement() {
    return lineIncrement;
  }

  public void setLineIncrement(int increment) {
    lineIncrement = increment;
  }

  public void setBlockIncrement(int increment) {
    setPageIncrement(increment);
  }

  public int getBlockIncrement() {
    return pageIncrement;
  }

  public void setPageIncrement(int increment) {
    pageIncrement = increment;
  }

  public int getOrientation() {
    return orientation;
  }

  public int getMinimum() {
    return thumb_coord_min;
  }
  public int getMaximum() {
    return thumb_coord_max + thumb_coord_size;
  }
  public int getVisibleAmount() {
    return thumb_coord_size;
  }


  /**
   *  Paints directly onto the specified Graphics.
   */
  public void directPaint(Graphics g)  {
    // Don't try to paint if Dimension or Graphic is null -- this happens 
    //   in some JVM's, and will cause NullPointerExceptions if not 
    //   checked for

    // Hmm... for some reason, null Graphics are slipping through 
    //   even though we are doing a null check!
    // However, can check for Dimension width or height being 0 (which 
    //   seems to always be associated with null Graphics) and return before 
    //   trying to draw to the Graphics if either == 0

    if (g == null) { return; }
    Dimension d = getSize();
    if (d == null) { return; }
    if (d.width <= 0 || d.height <= 0) { return; }

    // draw the scroll thumb
    if (draw_thumb) {
      if (color_thumb_foreground) {
        g.setColor(getForeground());
      }
      else {
        g.setColor(getBackground());
      }
      g.fill3DRect(thumb_rect.x, thumb_rect.y,
                   thumb_rect.width, thumb_rect.height, true);
      g.setColor(Color.black);
      if (debug) {
        g.drawString(thumb_coord_value + "   " + 
                     (thumb_coord_value + thumb_coord_size), 
                     thumb_rect.x, thumb_rect.y + 12);

      }
    }
    g.setColor(getForeground());

    if (draw_arrows) {
      if (orientation == HORIZONTAL) {
        // draw rectangles for scroll arrows
        g.drawRect(0,0,arrow_width, pixels_crosswise);
        g.drawRect(pixels_lengthwise-arrow_width, 0,
                   arrow_width, pixels_crosswise);
        // draw outline at edge of whole canvas
        g.drawRect(0,0,pixels_lengthwise-1, pixels_crosswise-1);
      }
      else {
        g.drawRect(0,0, pixels_crosswise, arrow_width);
        g.drawRect(0, pixels_lengthwise-arrow_width,
                   pixels_crosswise, arrow_width);
        g.drawRect(0,0,pixels_crosswise-1, pixels_lengthwise-1);
      }
      g.fillPolygon(low_arrow);
      g.fillPolygon(high_arrow);
    }
    else {
      Dimension dm = this.getSize();
      g.drawRect(0, 0, dm.width-1, dm.height-1);
      g.drawLine(0, 0, dm.width-1, dm.height-1);
      g.drawLine(0, dm.height-1, dm.width-1, 0);
    }
  }

  public void processMouseEvent(MouseEvent e) {
    heardMouseEvent(e);
  }

  public void processMouseMotionEvent(MouseEvent e) {
    heardMouseEvent(e);
  }

  public void heardMouseEvent(MouseEvent e) {
    int id = e.getID();

    // GAH 12-7-98
    // Think I've figured out bug where NeoScrollbar is "jumpy" on 
    //   thumb drags.  This happens sometimes when click on thumb, drag 
    //   outside of scrollbar, then drag back into scrollbar -- thumb 
    //   can "jump" over farther to the left than it should
    //   
    // looks like their is a bug in some JVMs in reporting correct pixel 
    // values in MOUSE_ENTER and MOUSE_EXIT Events -- ends up reporting 
    // pixel position relative to parent container (pixel 0, 0 at upper left 
    // corner of container) rather than relative to NeoScrollbar itself 
    // (pixel 0, 0, at upper left corner of NeoScrollbar).  This in turn is 
    // causing prevx/prevy to be set incorrectly.  Tentative solution is 
    // to completely ignore MOUSE_ENTER/MOUSE_EXIT events

    if (id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_EXITED) {
      return;
    }
    int prev_pixel, diff_pixel;
    int x = e.getX();
    int y = e.getY();
    int diffx = x - prevx;
    int diffy = y - prevy;
    if (orientation == HORIZONTAL) {
      prev_pixel = prevx;
      current_pixel = x;
      diff_pixel = diffx;
    }
    else {   // VERTICAL
      prev_pixel = prevy;
      current_pixel = y;
      diff_pixel = diffy;
    }
    prevx = x;
    prevy = y;

    // if thumb_responds = false, only respond to arrows
    if (id == MouseEvent.MOUSE_PRESSED)  {
      mouse_is_down = true;
      // only pay attention to arrows if they are drawn
      if (draw_arrows && (current_pixel < arrow_width))  {
        selected = LOWARROW;
        thumb_coord_value -= lineIncrement;
      }
      else if (draw_arrows && 
               (current_pixel > pixels_lengthwise - arrow_width))  {
        selected = HIGHARROW;
        thumb_coord_value += lineIncrement;
      }
      // only pay attention to thumb and gutter if thumb is big enough to draw
      else if (draw_thumb && thumb_rect.contains(x,y))   {
        selected = THUMB;
      }
      else if (draw_thumb && (current_pixel < thumb_pixel_value))  {
        selected = LOWGUTTER;
        thumb_coord_value -= pageIncrement;
      }
      else if (draw_thumb && 
               (current_pixel > (thumb_pixel_value + thumb_pixel_size)))  {
        selected = HIGHGUTTER;
        thumb_coord_value += pageIncrement;
      }
      else { 
        return ; 
      }
      if (thumb_coord_value < thumb_coord_min)  {
            thumb_coord_value = thumb_coord_min;
      }
      else if (thumb_coord_value > thumb_coord_max)  {
            thumb_coord_value = thumb_coord_max;
      }
      calcThumbPixels();
      repaint();

      // if mouse down over arrow, then start NeoTimerEventClock thread 
      //     to generate timer tick events  GAH 2-21-98
      if (selected == HIGHARROW || selected == LOWARROW || 
         (gutter_scroll && 
              (selected == HIGHGUTTER || selected == LOWGUTTER))) {
        if (timer != null) { timer.stop(); }
        timer = new NeoTimerEventClock(initial_delay, timer_interval);
        timer.addTimerListener(this);
        timer.start();
      }
      
    }

    else if (id == MouseEvent.MOUSE_DRAGGED) {
      // need to work on avoiding MOUSE_DRAG before MOUSE_UP
      if (prev_event_id == MouseEvent.MOUSE_PRESSED)  {
            prev_event_id = id;
            return;
      }
      if (selected == THUMB) {
        int moveto;
        moveto = thumb_pixel_value + diff_pixel;
        /*
         *  Bug: in some circumstances, scrollbar cannot get to 
         *       max value by dragging the thumb
         *    I believe this is what has been causing many of the zooming
         *    problems, where can't zoom to base visibility on some platforms
         *  Solution: Added +1 to this moveto max edge condition calculation
         *        as temporary fix
         */
        if (moveto > thumb_pixel_max - thumb_pixel_size + 1)  {
          moveto = thumb_pixel_max - thumb_pixel_size + 1;
        }
        else if (moveto < thumb_pixel_min)  {
          moveto = thumb_pixel_min;
        }

        // Fix for setting scrollbar at max or min if current_pixel is 
        //   not within gutter/thumb
        if (current_pixel < arrow_width) {
          moveto = thumb_pixel_min;
        }
        else if (current_pixel > pixels_lengthwise - arrow_width) {
          moveto = thumb_pixel_max - thumb_pixel_size + 1;
        }

        if (orientation == HORIZONTAL) {
          thumb_rect.setLocation(moveto, thumb_rect.y);
        }
        else {
          thumb_rect.setLocation(thumb_rect.x, moveto);
        }
        thumb_pixel_value = moveto;
        calcThumbValues();
      }
      else {
        // should probably just ignore drags in arrows, now that 
        //   there's a threaded timer for mouse holds
        // but leaving the drag stuff in for now...  GAH 2-21-98
        if (selected == LOWARROW) {
          thumb_coord_value -= lineIncrement;
        }
        else if (selected == HIGHARROW)  {
          thumb_coord_value += lineIncrement;
        }
        else if (selected == LOWGUTTER)  {
          // putting in check to make sure current_pixel is still in 
          //    low gutter (at least along the primary axis) -- GAH 12/16/96
          if (current_pixel < thumb_pixel_value)  {
            thumb_coord_value -= pageIncrement;
          }
        }
        else if (selected == HIGHGUTTER)  {
          // putting in check to make sure current_pixel is still in 
          //    high gutter (at least along the primary axis) -- GAH 12/16/96
          if (current_pixel > (thumb_pixel_value + thumb_pixel_size))  {
            thumb_coord_value += pageIncrement;
          }
        }
        if (thumb_coord_value < thumb_coord_min)  {
          thumb_coord_value = thumb_coord_min;
        }
        else if (thumb_coord_value > thumb_coord_max)  {
          thumb_coord_value = thumb_coord_max;
        }
        calcThumbPixels();
      }
      repaint();
    }
    else if (id == MouseEvent.MOUSE_RELEASED) {
      // BUG FIX 6-1-98
      // handling what seems to be an AWT bug: sometimes a MOUSE_ENTER 
      //   event either triggers a spurious MOUSE_UP event, or is itself 
      //   mistakenly given a MOUSE_UP id
      // therefore to filter out false MOUSE_UP, check that there was a 
      //   previous MOUSE_DOWN event
      if (mouse_is_down) {
        mouse_is_down = false;
      }
      else {
        // false report of a MOUSE_UP when there was no previous MOUSE_DOWN
        //     boot out without any further action!
        return;
      }
      
      selected = NO_SELECTION;

      // stop (and mark for gc) mouse-down-and-hold timer
      if (timer != null) { 
        timer.stop(); 
        timer = null;
      }
      repaint();
    }

    prev_event_id = id;
    if (id == MouseEvent.MOUSE_RELEASED || id == MouseEvent.MOUSE_PRESSED ||
        id == MouseEvent.MOUSE_DRAGGED) {
      AdjustmentEvent adjevt = new AdjustmentEvent(this, 0,
                                                      AdjustmentEvent.TRACK,
                                                      getValue());
        processAdjustmentEvent(adjevt);
    }
    return;
  }

  public void repaint() {
    if (debug) {
      repaint_tick++;
      System.out.println(" ");
      System.out.println("repainting myscrollbar, " + repaint_tick);
    }
    super.repaint();
  }

  public void addAdjustmentListener(AdjustmentListener listener) {
    if (listeners == null) {
      listeners = new Vector();
    }
    listeners.addElement(listener);
  }

  public void removeAdjustmentListener(AdjustmentListener listener) {
    if (listeners == null) { return; }
    listeners.removeElement(listener);
  }
  
  public void processAdjustmentEvent(AdjustmentEvent evt) {
    AdjustmentListener lis;
    // what the heck does drawing the thumbnail have to do with
    // whether or not events should be sent? --- Ed
    if (listeners != null /* && draw_thumb */) {
      for (int i=0; i<listeners.size(); i++)  {
        lis = (AdjustmentListener)listeners.elementAt(i);
        lis.adjustmentValueChanged(evt);
      }
    }
  }

  public void setMinimum(int min) {
    setValues(thumb_coord_value, thumb_coord_size,
              min, thumb_coord_max+thumb_coord_size);
  }

  public void setMaximum(int max) {
    setValues(thumb_coord_value, thumb_coord_size,
              thumb_coord_min, max);
  }

  public void setVisibleAmount(int vis) {
    setValues(thumb_coord_value, vis,
              thumb_coord_min, thumb_coord_max+thumb_coord_size);
  }

 /**
  *  Listening for events generated by timer NeoTimerEventClock.
  *  To respond appropriately to user holding mouse down over arrows
  *  to make scrolling appear faster without shortening the timer interval 
  *  too much, doing +/- 2*lineIncrement for holding down on arrows
  *  single clicks on arrows continue to be +/- lineIncrement for fine control
  */
  public void heardTimerEvent(NeoTimerEvent evt) {
    if (selected == LOWARROW || selected == HIGHARROW)  {
      if (selected == LOWARROW) {
        if (thumb_coord_value <= thumb_coord_min) { return; }
        thumb_coord_value -= (2*lineIncrement);
      }
      else if (selected == HIGHARROW) {
        if (thumb_coord_value >= thumb_coord_max) { return; }
        thumb_coord_value += (2*lineIncrement);
      }
      if (thumb_coord_value < thumb_coord_min)  {
        thumb_coord_value = thumb_coord_min;
      }
      else if (thumb_coord_value > thumb_coord_max)  {
        thumb_coord_value = thumb_coord_max;
      }
      calcThumbPixels();
      repaint();
      AdjustmentEvent adjevt = 
              new AdjustmentEvent(this, 0, AdjustmentEvent.TRACK, getValue());
      processAdjustmentEvent(adjevt);
    }
    else if (selected == LOWGUTTER || selected == HIGHGUTTER) {
      if (selected == LOWGUTTER)  {
        if (current_pixel < thumb_pixel_value)  {
          if (thumb_coord_value <= thumb_coord_min) { return; }
          thumb_coord_value -= pageIncrement;          
        }
      }
      else if (selected == HIGHGUTTER)  {
        if (current_pixel > (thumb_pixel_value + thumb_pixel_size))  {
          // should be == rather than >=
          if (thumb_coord_value >= thumb_coord_max) { return; }
          thumb_coord_value += pageIncrement;
        }
      }
      if (thumb_coord_value < thumb_coord_min)  {
        thumb_coord_value = thumb_coord_min;
      }
      else if (thumb_coord_value > thumb_coord_max)  {
        thumb_coord_value = thumb_coord_max;
      }

      calcThumbPixels();
      repaint();
      AdjustmentEvent adjevt = 
        new AdjustmentEvent(this, 0, AdjustmentEvent.TRACK, getValue());
      processAdjustmentEvent(adjevt);
    }
  }

  public void thumbIsForegroundColor(boolean b) {
    color_thumb_foreground = b;
  }

  public void continuousGuttScroll(boolean b) {
    gutter_scroll = b;
  }

  /**
   * Gets the component's preferred size.
   * This method is deprecated in java.awt.Component.
   * None the less it is over ridden here
   * because I suspect other AWT classes still call this procedure.
   * The default implementation of the preferred {@link #getPreferredSize()} calls this method.
   * @deprecated Use getPreferredSize().
   */
  public Dimension preferredSize() {
    if ( HORIZONTAL == this.orientation ) {
      return new Dimension( 2 * arrow_width + thumb_pixel_size_min, arrow_width );
    }
    else {
      return new Dimension( arrow_width, 2 *arrow_width + thumb_pixel_size_min );
    }
  }

  /**
   * @deprecated Use {@link #setDoubleBuffered(boolean)} instead.
   */
  public void setBuffered(boolean b) {
    setDoubleBuffered(b);
  }

  /**
   * @deprecated Use {@link #isDoubleBuffered()} instead.
   */
  public boolean getBuffered() {
    return isDoubleBuffered();
  }

}
