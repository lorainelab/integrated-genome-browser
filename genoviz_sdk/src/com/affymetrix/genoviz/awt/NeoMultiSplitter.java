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
import java.util.*;
import com.affymetrix.genoviz.util.NeoConstants;  // to use VERTICAL and HORIZONTAL

/**
 * puts multiple components in a panel
 * where their layout can be dynamically changed
 * by adjusting borders between them.
 *
 * <p> This is a bit like Swing's JSplitPane.
 * This one, however, can layout more than two components.
 * It can also be used with Java 1.0.
 *
 * <p> It is also still a bit experimental.
 * Dynamic resizing, in particular, is problematic in some environments.
 */
public class NeoMultiSplitter extends Panel 
implements NeoConstants, LayoutManager  {

  private boolean DEBUG_LAYOUT = false;

  static Color middleTransient = Color.black;
  static Color lightTransient = Color.white;
  static Color darkTransient = Color.black;

  /*
   * color intensity at which to transition from using darkTransient 
   *    against a light background to lightTransient against a dark 
   *    background
   * full intensity is white, r=255 + g=255 + b=255 = 765, 
   *    so threshold light/dark transition at half intensity = 382
   * unfortunately, this makes the bar hard to see when using 
   *    lightTransient/darkTransient colors that aren't extreme, when 
   *    moving bar over other mellow colors...
   * therfore try three transient colors -- when background is mellow, use 
   *    middleTransient, which should be extreme
   */
  static int low_threshold = 200;
  static int high_threshold = 565;

  /**
   * possible constraint for components in this Panel.
   * Relative means a child's size is
   * determined by proportion relative to other components.
   */
  public static int CONSTRAIN_RELATIVE = 2;

  /**
   * possible constraint for components in this Panel.
   * Absolute means a child's size is constrained
   * to an absolute size in pixels.
   * <em>Not yet used.</em>
   */
  public static int CONSTRAIN_ABSOLUTE = 3;

  /**
   * possible value for layout timing.
   * Components are resized dynamically as the mouse is dragged.
   * <em>Do not use. Still buggy.</em>
   */
  public static int DYNAMIC_LAYOUT = 5;
  /**
   * possible value for layout timing.
   * Components are resized when the mouse button is released.
   */
  public static int DELAYED_LAYOUT = 6;
  /**
   * possible value for layout timing.
   * Potential fix for slow machines. 
   * Just cursor change?
   */
  public static int REALLY_DELAYED_LAYOUT = 7;  

  protected int comp_count = 0;
  protected int orientation = HORIZONTAL;
  protected int layoutConstraint = CONSTRAIN_RELATIVE;
  protected int layoutTiming = DELAYED_LAYOUT;

  protected Dimension prev_size = new Dimension();
  protected int barsize = 8;
  protected int tbarsize = barsize;
  protected int min_comp_size = 0;
  protected boolean togglingControl = false;
  protected Component toggledComponent = null;
  Vector<NeoSplitterBar> splitters = new Vector<NeoSplitterBar>();

  // the splitter currently being moved
  NeoSplitterBar current_splitter;

  Vector<Component> comps = new Vector<Component>();
  
  // vector of components' previous size, relative to others (as percentage?), 
  //  for toggling expansion/compression
  Vector prev_relative_size = new Vector();

  // Assumes that you can only drag one splitter bar at a time
  int split_loc  = -1;
  Point last_drag_loc = null;

  /**
   * constructs a NeoMultiSplitter in a HORIZONTAL orientation.
   */
  public NeoMultiSplitter() {
    setLayout(this);
  }

  /**
   * constructs a NeoMultiSplitter in the given orientation.
   *
   * @param theOrientation must be VERTICAL or HORIZONTAL.
   *        VERTICAL splitters split the panels horizontally
   *        so that the dividers between panels are vertical.
   *        Similarly for HORIZONTAL.
   *
   *        <p><em>Note: these orientations are the opposite
   *        of those used by JSplitPane.</em>
   */
  public NeoMultiSplitter(int theOrientation) {
    this();
    switch (theOrientation) {
    case HORIZONTAL:
    case VERTICAL:
      break;
    default:
      throw new IllegalArgumentException(
        "Orientation must be either HORIZONTAL or VERTICAL.");
    }
    this.orientation = theOrientation;
  }

  public boolean mouseDown(Event evt, int x, int y) {
    if (evt.target instanceof NeoSplitterBar) {
      NeoSplitterBar bar = (NeoSplitterBar)evt.target;
      Rectangle barbox = bar.getBounds();
      // transform x and y back to bar coordinates
      int bx = x - barbox.x;
      int by = y - barbox.y;
      boolean hitControls = bar.hitControls(bx, by);
      if (hitControls)  { 
        togglingControl = true;
        if (bar.hitHighExpand(bx, by)) {
          toggledComponent = getHighComponent(bar);
        }
        else {
          toggledComponent = getLowComponent(bar);
        }
      }
    }
    return super.mouseDown(evt, x, y);
  }

  public boolean mouseUp(Event evt, int x, int y) {
    if (evt.target instanceof NeoSplitterBar) {
      if (togglingControl) {
        toggleExpansion((NeoSplitterBar)evt.target, toggledComponent);
        togglingControl = false;
        return super.mouseUp(evt, x, y);
      }
      if (layoutTiming == DELAYED_LAYOUT) {
        if (last_drag_loc != null) {
          drawTransientSplit(last_drag_loc.x, last_drag_loc.y, this);
          if (orientation == VERTICAL)  {
            split_loc = last_drag_loc.x;
          }
          else {
            split_loc = last_drag_loc.y;
          }
          doLayout();
          last_drag_loc = null;
        }
      }
      else {
        // do nothing for dynamic layout -- already done in mouseDrag
        last_drag_loc = null;
      }
    }
    return super.mouseUp(evt, x, y);
  }

  public boolean mouseDrag(Event evt, int x, int y) {
    if (evt.target instanceof NeoSplitterBar) {
      if (togglingControl) {
        return super.mouseDrag(evt, x, y);
      }
      current_splitter = (NeoSplitterBar)evt.target;
      if ((orientation == VERTICAL) &&
        (last_drag_loc != null) && (x == last_drag_loc.x)) {
        return true;
      }
      else if ((orientation == HORIZONTAL) &&
               (last_drag_loc != null) && (y == last_drag_loc.y)) {
        return true;
      }

      if (layoutTiming == DYNAMIC_LAYOUT) {
        if (orientation == VERTICAL) {
          split_loc = x;
        }
        else {
          split_loc = y;
        }
        doLayout();
      }
      else { // defaults to DELAYED_LAYOUT
        moveTransientSplit(x, y);
      }
      if (last_drag_loc == null) {
        last_drag_loc = new Point(0, 0);
      }
      last_drag_loc.x = x;
      last_drag_loc.y = y;
      return true;
    }
    else {
      return super.mouseDrag(evt, x, y);
    }
  }

  public void moveTransientSplit(int x, int y) {
    if (last_drag_loc != null) {
      drawTransientSplit(last_drag_loc.x, last_drag_loc.y, this);
    }
    drawTransientSplit(x, y, this);
  }

  public void drawTransientSplit(int x, int y, Container cont) {
    Component[] children = cont.getComponents();
    Component child;
    Graphics pgraphics, cgraphics;
    Rectangle child_bounds;
    int xtrans, ytrans;
    Color transColor;
    Color back;

    try {
      pgraphics = cont.getGraphics();
      back = cont.getBackground();
      int intensity = back.getRed() + back.getGreen() + back.getBlue();
      if (intensity < low_threshold) { 
        transColor = lightTransient; 
      } 
      else if (intensity < high_threshold) {
        transColor = middleTransient; 
      }
      else { transColor = darkTransient; } 
      pgraphics.setColor(transColor);
      pgraphics.setXORMode(back);

      if (orientation == VERTICAL) {
        pgraphics.fillRect(x-tbarsize/2, 0, tbarsize, getSize().height);
      }
      else {
        pgraphics.fillRect(0, y-tbarsize/2, getSize().width, tbarsize);
      }
      pgraphics.dispose();
    }
    catch (Exception ex) {
      // something happened when trying to use cont's Graphics
    }

    for (int i=0; i < children.length; i++) {
      child = children[i];
      child_bounds = child.getBounds();

      // if transient split not within component, skip this component
      if (orientation == VERTICAL && 
          (x < child_bounds.x || 
           x > (child_bounds.x + child_bounds.width))) {
        continue;
      }
      else if (orientation == HORIZONTAL && 
               (y < child_bounds.y || 
                y > (child_bounds.y + child_bounds.height))) {
        continue;
      }

     // translating event location from parent coordinates to child coordinates
     // (not using evt.translate() because don't want to modify evt.x or evt.y
      xtrans = x-child_bounds.x;
      ytrans = y-child_bounds.y;
      if (child instanceof Container) {
        drawTransientSplit(xtrans, ytrans, (Container)child);
      }
      else {
        try {
          cgraphics = child.getGraphics();
          back = child.getBackground();
          int intensity = back.getRed() + back.getGreen() + back.getBlue();

          if (intensity < low_threshold) { 
            transColor = lightTransient; 
          } 
          else if (intensity < high_threshold) {
            transColor = middleTransient; 
          }
          else { transColor = darkTransient; } 
          cgraphics.setColor(transColor);
          cgraphics.setXORMode(back);


          if (orientation == VERTICAL) {
            cgraphics.fillRect(xtrans-tbarsize/2, 0, 
                               tbarsize, child.getSize().height);
          }
          else {
            cgraphics.fillRect(0, ytrans-tbarsize/2, 
                               child.getSize().width, tbarsize);
          }
          cgraphics.dispose();
        }
              catch (Exception ex) {
          // something happened while trying to draw to child's Graphics
          // I think this may happen for some Components
          //   (Button, Label, Scrollbar, etc.)
              }
      }

    }
  }

  /**
   * hides or reveals the given component.
   * This simulates the user moving the bar by dragging.
   *
   * @param bar the bar to move.
   * @param comp the component to hide or reveal.
   */
  public void toggleExpansion(NeoSplitterBar bar, Component comp) {
    current_splitter = bar;
    if (comp.getSize().height > 0) {
      split_loc = 0;
    }
    else {
      split_loc = 100;
    }
    doLayout();
    
  }

  /**
   * gets the component below (or to the right of) the given bar.
   *
   * @param bar the bar between two components.
   */
  public Component getLowComponent(NeoSplitterBar bar) {
    for (int i=0; i<splitters.size(); i++) {
      if (splitters.elementAt(i) == bar) {
        return (Component)comps.elementAt(i);
      }
    }
    return null;
  }

  /**
   * gets the component above (or to the left of) the given bar.
   *
   * @param bar the bar between two components.
   */
  public Component getHighComponent(NeoSplitterBar bar) {
    for (int i=0; i<splitters.size(); i++) {
      if (splitters.elementAt(i) == bar) {
        return (Component)comps.elementAt(i+1);
      }
    }
    return null;
  }

  /* ------  LayoutManager implementation -------- */

  public void addLayoutComponent(String name, Component comp) {
    // keeping track of "real" (non-splitter) components in a separate vector
    if (comp instanceof NeoSplitterBar)  {
      // do nothing, splitter should already have been added...
    }
    else {
      if (comps.size() > 0) {
        NeoSplitterBar bar = new NeoSplitterBar(orientation);
        this.add("comp" + comp_count, bar);
        //        this.add("whatever", bar);
        //        current_splitter = bar;
        // therefore calling via inheritance Container.add(name,bar), which
        // in turn will call addLayoutComponent(name,bar), which will end up
        // in the first branch of the conditional -- a little twisted, but
        // it gets the job done...
        splitters.addElement(bar);
        comp_count++;
      }
      comps.addElement(comp);
      comp_count++;
    }
  }

  public void removeLayoutComponent(Component comp) {
    if (comp instanceof NeoSplitterBar) {
      splitters.removeElement(comp);
    }
    else {
      NeoSplitterBar bar = null;
      Component comparray[] = this.getComponents();
      for (int i=0; i<comparray.length; i++) {
        if (comparray[i] == comp) {
          if ((i != (comparray.length-1)) &&
              (comparray[i] instanceof NeoSplitterBar)) {
            bar = (NeoSplitterBar)comparray[i+1];
          }
          break;
        }
      }
      comps.removeElement(comp);
      if (bar != null) {
        this.remove(bar);
        // therefore will call via inheritance Container.remove(bar), which
        // will in turn eventually call removeLayoutComponent(bar), which
        // will end up in the first branch of the conditional
      }
    }
  }

  public Dimension preferredLayoutSize(Container target) {
    if (target != this) {
      throw new IllegalArgumentException("NeoMultiSplitter can only serve as its own LayoutManager!");
    }
    return getSize();
  }

  public Dimension minimumLayoutSize(Container target) {
    if (target != this) {
      throw new IllegalArgumentException("NeoMultiSplitter can only serve as its own LayoutManager!");
    }
    return getSize();
  }

  /**
   * Where most of the real work of the layout management happens
   */
  public void layoutContainer(Container target) {
    if (DEBUG_LAYOUT) {
      System.err.print("NeoMultiSplitter.layoutContainer() called, " + 
                       "current splitter = " + current_splitter);
    }
    if (target != this) {
      throw new IllegalArgumentException("NeoMultiSplitter can only serve as its own LayoutManager!");
    }

    // if no components contained, then there's nothing to lay out...
    // remember, MUST use add("name", component) method for components to 
    // actually be added
    if (comps.size() == 0) {
      return; 
    }
    Dimension dm = getSize();
    Insets ins = getInsets();

    int top = ins.top;
    int bottom = dm.height - ins.bottom;
    int left = ins.left;
    int right = dm.width - ins.right;
    int usable_height, usable_width;
    usable_height = bottom - top;
    usable_width = right - left;

    int total_splitter_pixels = barsize * splitters.size();

    // assume for the moment that comps and splitters are visilbe && != null
    int splitleft, splitright, splittop, splitbottom;

    // just a temporary route around VERTICAL option
    if (orientation == VERTICAL) { 
      int spare_pixels = usable_width - total_splitter_pixels;
      if (current_splitter == null) {
        int total_comp_pixels = 0;
        for (int i=0; i<comps.size(); i++) {
          total_comp_pixels += ((Component)comps.elementAt(i)).getSize().width;
        }
        if (DEBUG_LAYOUT) {
          System.out.println("comps: " + total_comp_pixels + 
                             ", splitters: " + total_splitter_pixels + 
                             ", spare_pixels: = " + spare_pixels);
        }
        //  previous dimensions were zeroed out, so divide up equally
        if (total_comp_pixels == 0) {
          int comp_size = spare_pixels / comps.size();
          int offset = left;
          for (int i=0; i<comps.size(); i++) {
            Component comp = (Component)comps.elementAt(i);
            if (DEBUG_LAYOUT) {
              System.out.println("Reshaping comp " + comp + 
                                 ", offset = " + offset + 
                                 ", size = " + comp_size);
            }
            comp.setBounds(offset, top, comp_size, usable_height);
            offset += comp_size;
            if (i < (comps.size()-1)) {
              Component splitbar = (Component)splitters.elementAt(i);
              if (DEBUG_LAYOUT)  {
                System.out.println("Reshaping splitbar " + splitbar + 
                                   ", offset = " + offset + 
                                   ", size = " + barsize);
              }
              splitbar.setBounds(offset, top, barsize, usable_height);
              offset += barsize;
            }
          }
        }
        else { 
          // no splitter moved, and components already have been sized
          // therefore scale non-splitbar components proportionally
          if (DEBUG_LAYOUT) { 
            System.err.print("no splitter moved, components already sized, " + 
                             "therefore scaling components proportionally");
          } 
          double scaling_factor = 
            (double)spare_pixels / (double)total_comp_pixels;
          double proportion;
          int comp_size;
          int offset = left;
          for (int i=0; i<comps.size(); i++) {
            Component comp = (Component)comps.elementAt(i);
            comp_size = (int)(scaling_factor * comp.getSize().width);
            comp.setBounds(offset, top, comp_size, usable_height);
            offset += comp_size;
            if (i < (comps.size()-1)) {
              Component splitbar = (Component)splitters.elementAt(i);
              splitbar.setBounds(offset, top, barsize, usable_height);
              offset += barsize;
            }
          }

        }
      }
      else {   // current_splitter != null
        // if layoutContainer call is due to dragging a splitter bar, then 
        // calculate the total pixels of the two components that this bar is
        //    actually splitting, and re-shape them accordingly
        // for this type of behavior, don't have to worry about other components

        Component greater = null;
        Component lesser = null;
        int splitter_num = 0;
        for (int i=0; i<splitters.size(); i++) {
          if (splitters.elementAt(i) == current_splitter) {
            splitter_num = i;
            lesser = (Component)comps.elementAt(i);
            greater = (Component)comps.elementAt(i+1);
            break;
          }
        }
        if (splitter_num > 0) { 
          Rectangle prev_split_bounds = 
            ((Component)splitters.elementAt(splitter_num-1)).getBounds();
          left = prev_split_bounds.x + prev_split_bounds.width;
        }
        if (splitter_num < (splitters.size()-1)) {
          Rectangle next_split_bounds = 
            ((Component)splitters.elementAt(splitter_num+1)).getBounds();
          right = next_split_bounds.x;
        }
        
        splitleft = split_loc-barsize/2;
        splitright = split_loc+barsize/2;
        if (DEBUG_LAYOUT)  {
          System.err.print(" splitleft = " + splitleft + 
                           " splitright = " + splitright + 
                           " left = " + left + ", right = " + right);
        } 
        if (splitleft < left) { 
          if (DEBUG_LAYOUT)  { System.err.print(" splitleft < left!"); }
          splitleft = left + min_comp_size;
          splitright = splitleft + barsize;
        }
        if (splitright > right) {
          if (DEBUG_LAYOUT)  { System.err.print(" splitright > right!"); }
          splitright = right - min_comp_size;
          splitleft = splitright - barsize;
        }

        if (current_splitter != null)  {
          current_splitter.setBounds(splitleft, top, barsize, usable_height);
          if (DEBUG_LAYOUT)  {
            System.err.print("Splitter: " + current_splitter.getBounds() + ", " + 
                             current_splitter.getBackground());
          }
          current_splitter.validate();
        }
        if (lesser != null)  {
          // validating because sometimes panels don't redraw after re-shape 
          //   unless specifically validated...
          lesser.setBounds(left, top, splitleft-left, usable_height);
          if (DEBUG_LAYOUT)  {
            System.err.print("Lesser: " + lesser + ", " + 
                             lesser.getBounds() + ", " + 
                             lesser.getBackground());
          }
          lesser.validate();
        }
        if (greater != null)  {
          // validating because sometimes panels don't redraw after re-shape 
          //   unless specifically validated...
          greater.setBounds(splitright, top, right-splitright, usable_height);
          if (DEBUG_LAYOUT)  {          
            System.err.print("Greater: " + greater + ", " + 
                             greater.getBounds() + ", " + 
                             greater.getBackground());
          }
          greater.validate();
        }
        int total_size = 0;
        for (int i=0; i<comps.size(); i++) {
          total_size += ((Component)comps.elementAt(i)).getSize().width;
        }
        if (DEBUG_LAYOUT)  { 
          System.err.println("Total Component Size: " + total_size);
        }
      }  // END splitter used conditional
    }  // END orientation == VERTICAL branch
    else { // orientation == HORIZONTAL
      int spare_pixels = usable_height - total_splitter_pixels;

      // if there's no current_splitter, assume this is a resizing issue, 
      // and divide up the space based on previous proportions
      // but, if previous dimensions were zeroed out, then divide up 
      // equally
      if (current_splitter == null) {
        int total_comp_pixels = 0;
        for (int i=0; i<comps.size(); i++) {
          total_comp_pixels += ((Component)comps.elementAt(i)).getSize().height;
        }
        if (DEBUG_LAYOUT) {
          System.out.println("comps: " + total_comp_pixels + 
                             ", splitters: " + total_splitter_pixels + 
                             ", spare_pixels: = " + spare_pixels);
        }
        //  previous dimensions were zeroed out, so divide up equally
        if (total_comp_pixels == 0) {
          int comp_size = spare_pixels / comps.size();
          int offset = top;
          for (int i=0; i<comps.size(); i++) {
            Component comp = (Component)comps.elementAt(i);
            if (DEBUG_LAYOUT) {
              System.out.println("Reshaping comp " + comp + 
                                 ", offset = " + offset + 
                                 ", size = " + comp_size);
            }
            comp.setBounds(left, offset, usable_width, comp_size);
            offset += comp_size;
            if (i < (comps.size()-1)) {
              Component splitbar = (Component)splitters.elementAt(i);
              if (DEBUG_LAYOUT)  {
                System.out.println("Reshaping splitbar " + splitbar + 
                                   ", offset = " + offset + 
                                   ", size = " + barsize);
              }
              splitbar.setBounds(left, offset, usable_width, barsize);
              offset += barsize;
            }
          }
        }
        else { 
          // no splitter moved, and components already have been sized
          // therefore scale non-splitbar components proportionally
          if (DEBUG_LAYOUT) { 
            System.err.print("no splitter moved, components already sized, " + 
                             "therefore scaling components proportionally");
          } 
          double scaling_factor = 
            (double)spare_pixels / (double)total_comp_pixels;
          double proportion;
          int comp_size;
          int offset = top;
          for (int i=0; i<comps.size(); i++) {
            Component comp = (Component)comps.elementAt(i);
            comp_size = (int)(scaling_factor * comp.getSize().height);
            comp.setBounds(left, offset, usable_width, comp_size);
            offset += comp_size;
            if (i < (comps.size()-1)) {
              Component splitbar = (Component)splitters.elementAt(i);
              splitbar.setBounds(left, offset, usable_width, barsize);
              offset += barsize;
            }
          }

        }
      }

      // if layoutContainer call is due to dragging a splitter bar, then 
      // calculate the total pixels of the two components that this bar is
      //    actually splitting, and re-shape them accordingly
      // for this type of behavior, don't have to worry about other components
      else {
        Component greater = null;
        Component lesser = null;
        int splitter_num = 0;
        for (int i=0; i<splitters.size(); i++) {
          if (splitters.elementAt(i) == current_splitter) {
            splitter_num = i;
            lesser = (Component)comps.elementAt(i);
            greater = (Component)comps.elementAt(i+1);
            break;
          }
        }
        if (splitter_num > 0) { 
          Rectangle prev_split_bounds = 
            ((Component)splitters.elementAt(splitter_num-1)).getBounds();
          top = prev_split_bounds.y + prev_split_bounds.height;
        }
        if (splitter_num < (splitters.size()-1)) {
          Rectangle next_split_bounds = 
            ((Component)splitters.elementAt(splitter_num+1)).getBounds();
          bottom = next_split_bounds.y;
        }
        
        splittop = split_loc-barsize/2;
        splitbottom = split_loc+barsize/2;
        if (DEBUG_LAYOUT)  {
          System.err.print(" splittop = " + splittop + 
                           " splitbottom = " + splitbottom + 
                           " top = " + top + ", bottom = " + bottom);
        } 
        if (splittop < top) { 
          if (DEBUG_LAYOUT)  { System.err.print(" splittop < top!"); }
          splittop = top + min_comp_size;
          splitbottom = splittop + barsize;
        }
        if (splitbottom > bottom) {
          if (DEBUG_LAYOUT)  { System.err.print(" splitbottom > bottom!"); }
          splitbottom = bottom - min_comp_size;
          splittop = splitbottom - barsize;
        }

        if (current_splitter != null)  {
          current_splitter.setBounds(left, splittop, usable_width, barsize);
          if (DEBUG_LAYOUT)  {
            System.err.print("Splitter: " + current_splitter.getBounds() + ", " + 
                             current_splitter.getBackground());
          }
          current_splitter.validate();
        }
        if (lesser != null)  {
          // validating because sometimes panels don't redraw after re-shape 
          //   unless specifically validated...
          lesser.setBounds(left, top, usable_width, splittop-top);
          if (DEBUG_LAYOUT)  {
            System.err.print("Lesser: " + lesser + ", " + 
                             lesser.getBounds() + ", " + 
                             lesser.getBackground());
          }
          lesser.validate();
        }
        if (greater != null)  {
          // validating because sometimes panels don't redraw after re-shape 
          //   unless specifically validated...
          greater.setBounds(left, splitbottom, usable_width, bottom-splitbottom);
          if (DEBUG_LAYOUT)  {          
            System.err.print("Greater: " + greater + ", " + 
                             greater.getBounds() + ", " + 
                             greater.getBackground());
          }
          greater.validate();
        }
        int total_size = 0;
        for (int i=0; i<comps.size(); i++) {
          total_size += ((Component)comps.elementAt(i)).getSize().height;
        }
        if (DEBUG_LAYOUT)  { 
          System.err.println("Total Component Size: " + total_size);
        }
      }  // END splitter used conditional
    }  // END orientation conditional
    prev_size.width = dm.width;
    prev_size.height = dm.height;
    if (layoutTiming == DELAYED_LAYOUT) { 
      current_splitter = null;
    }
  }


  public void setLayoutTiming(int timing) {
    if (timing != DYNAMIC_LAYOUT && timing != DELAYED_LAYOUT) {
      throw new IllegalArgumentException("argument to " +
                  "NeoMultiSplitter.setLayoutTiming() must be either " +
                  "NeoMultiSplitter.DYNAMIC_LAYOUT or NeoMultiSplitter.DELAYED_LAYOUT");
    }
    layoutTiming = timing;
  }

  public int getLayoutTiming() {
    return layoutTiming;
  }

  public void setLayoutConstraint(int constraint) {
    if (constraint != CONSTRAIN_ABSOLUTE && constraint != CONSTRAIN_RELATIVE) {
      throw new IllegalArgumentException("argument to " +
                  "NeoMultiSplitter.setLayoutConstraint() must be either " +
                  "NeoMultiSplitter.CONSTRAIN_ABSOLUTE " +
                  "or NeoMultiSplitter.CONSTRAIN_RELATIVE");
    }
    layoutConstraint = constraint;
  }

  public int getLayoutConstraint() {
    return layoutConstraint;
  }

  /**
   * sets the "width" of the divider(s) between panes.
   *
   * @param size in pixels.
   */
  public void setDividerSize(int size) {
    if (barsize != size) {
      barsize = size;
      tbarsize = barsize;
      doLayout();
    }
  }

  /**
   * gets the "width" of the divider(s) between panes.
   *
   * @return size in pixels.
   */
  public int getDividerSize() {
    return barsize;
  }

}
