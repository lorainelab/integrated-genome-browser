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

package com.affymetrix.genoviz.bioviews;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoGlyphDragEvent;
import com.affymetrix.genoviz.event.NeoGlyphDragListener;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoWidgetI;
import com.affymetrix.genoviz.widget.NeoWidget;
import java.awt.geom.Point2D;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A class to handle generic dragging of Glyphs (and glyph hierarchies)
 * on a NeoWidget.
 */
public class GlyphDragger
  implements MouseListener, MouseMotionListener, NeoConstants {

  // internal mapping of HORIZONTAL to HORIZ
  int HORIZ = 0;
  // internal mapping of VERTICAL to VERT
  int VERT = 1;

  // flags for whether to contrain dragging in the horizontal
  // or vertical directions -- if a constraint is true, that means
  // the glyph cannot be dragged in that direction
  boolean constrained[] = new boolean[2];

  // flag for whether to drag "actual" glyph, or a copy of it
  // Doesn't really work right now -- assuming that the glyph
  //   being passed in is already a copy of the actual glyph
  // But, moving towards situation where GlyphDragger handles more
  //   of the details, including copying glyph and manipulation of
  //   transient container
  boolean drag_a_copy = true;

  double prevx, prevy;
  double currentx, currenty;
  Point2D.Double prev_point = new Point2D.Double(0, 0);
  Point2D.Double cur_point = new Point2D.Double(0, 0);

  GlyphI dragged_glyph;
  NeoWidgetI widget;
  List<NeoGlyphDragListener> drag_listeners = new CopyOnWriteArrayList<NeoGlyphDragListener>();
  boolean force_within_parent = false;

  // a transform to use when mapping mouse drags to glyph coords
  //    if no trans is passed in via startDrag, will use standard
  //    View transform
  LinearTransform trans;

  //  public GlyphDragger(NeoWidgetI widg, GlyphI gl, NeoMouseEvent nevt) {
  public GlyphDragger(NeoWidgetI widg) {
    this.widget = widg;
    constrained[HORIZ] = false;
    constrained[VERT] = false;
  }

  public LinearTransform getTransform(NeoMouseEvent nevt) {
    if (nevt != null) {
      Object src = nevt.getSource();
      if (src instanceof NeoWidget) {
        trans = (LinearTransform)((NeoWidget)src).getView().getTransform();
        return trans;
      }
    }
    return null;
  }

  public void startDrag(GlyphI gl, NeoMouseEvent nevt) {
    startDrag(gl, nevt, null);
  }

  public void startDrag(GlyphI gl, NeoMouseEvent nevt, LinearTransform t) {
    startDrag(gl, nevt, t, false);
  }

  /**
   *  Start a drag.
   *  Allowing option of specifying a LinearTransform, rather than
   *    getting it from the NeoMouseEvent's view, so that can use with
   *    glyph's that have modified transforms (not identical to
   *    view's global transform)
   */
  public void startDrag(GlyphI gl, NeoMouseEvent nevt, LinearTransform t, boolean restrict_to_parent) {
    force_within_parent = restrict_to_parent;
    trans = t;
    if (trans == null) {
      trans = getTransform(nevt);
    }
    this.dragged_glyph = gl;
    // if have no event to start with, then set start coords based on
    //   glyph coords
    if (nevt == null) {
      prevx = gl.getCoordBox().x;
      prevy = gl.getCoordBox().y;
    }
    // otherwise base start coords on event coords
    else {
      prev_point.x = (double)nevt.getX();
      prev_point.y = (double)nevt.getY();
      // inverse transform to map pixel position to coords
      trans.inverseTransform(prev_point, prev_point);
    }
    // flushing, just in case...
    widget.removeMouseListener(this);
    widget.removeMouseMotionListener(this);
    widget.addMouseListener(this);
    widget.addMouseMotionListener(this);
    if (drag_listeners.size() > 0) {
      NeoGlyphDragEvent dragevt =
        new NeoGlyphDragEvent(widget, NeoGlyphDragEvent.DRAG_STARTED, dragged_glyph);
      notifyListeners(dragevt);
    }
  }

  public void mouseMoved(MouseEvent evt) { }

  public void mouseDragged(MouseEvent evt) {
    if (!(evt instanceof NeoMouseEvent)) { return; }
    NeoMouseEvent nevt = (NeoMouseEvent)evt;
    if (trans == null) { trans = getTransform(nevt); }
    cur_point.x = (double)nevt.getX();
    cur_point.y = (double)nevt.getY();
    trans.inverseTransform(cur_point, cur_point);

    if (constrained[HORIZ]) {
      cur_point.x = prev_point.x;
    }
    if (constrained[VERT]) {
      cur_point.y = prev_point.y;
    }
    if (force_within_parent) {
      java.awt.geom.Rectangle2D.Double pbox = dragged_glyph.getParent().getCoordBox();
      java.awt.geom.Rectangle2D.Double cbox = dragged_glyph.getCoordBox();
      if (cur_point.y < pbox.y) {
        dragged_glyph.moveAbsolute(cbox.x, pbox.y);
      }
      else {
        dragged_glyph.moveRelative(cur_point.x - prev_point.x, cur_point.y - prev_point.y);
      }
    }
    else {
      dragged_glyph.moveRelative(cur_point.x - prev_point.x, cur_point.y - prev_point.y);
    }

    prev_point.x = cur_point.x;
    prev_point.y = cur_point.y;
    if (drag_listeners.size() > 0) {
      NeoGlyphDragEvent dragevt =
        new NeoGlyphDragEvent(widget, NeoGlyphDragEvent.DRAG_IN_PROGRESS, dragged_glyph);
      notifyListeners(dragevt);
    }
    widget.updateWidget();
  }


  public void mousePressed(MouseEvent evt) { }
  public void mouseClicked(MouseEvent evt) { }
  public void mouseEntered(MouseEvent evt) { }
  public void mouseExited(MouseEvent evt) { }

  public void mouseReleased(MouseEvent evt) {
    mouseDragged(evt);
    widget.removeMouseListener(this);
    widget.removeMouseMotionListener(this);
    if (drag_listeners.size() > 0) {
      NeoGlyphDragEvent dragevt =
        new NeoGlyphDragEvent(widget, NeoGlyphDragEvent.DRAG_ENDED, dragged_glyph);
      notifyListeners(dragevt);
    }
    if (drag_a_copy) {
      widget.removeItem(dragged_glyph);
      widget.updateWidget();
    }
    dragged_glyph = null; // helps with garbage collection
    trans = null;
  }

  /**
   *  Duplicate a glyph hierarchy.
   *
   *  TODO:
   *  WARNING:  currently may not deal well with glyph's that have a
   *    transform different from the View's standard transform.
   *   (for example, glyph's whose ancestor is a PixelFloaterGlyph or
   *      a TranformTierGlyph)
   *
   *  duplicate a glyph hierarchy
   *  @param col if null, duplicate colors of glyph hierarchy;
   *             if not null, set all colors in glyph hierarchy to col
   */
  public static GlyphI duplicateGlyph(GlyphI gl, Color col) {
    // really should just be able to do a copy/clone method call on
    // the glyph, but that API doesn't exist yet

    GlyphI newgl = null;
    try  {
      newgl = (GlyphI)gl.getClass().newInstance();
    }
    catch (Exception ex) {
      System.err.println("Exception in GlyphDragger.duplicateGlyph()!");
      ex.printStackTrace();
      return null;
    }
    if (col == null)  {
      newgl.setColor(gl.getColor());
    }
    else {
      newgl.setColor(col);
    }
    java.awt.geom.Rectangle2D.Double cbox = gl.getCoordBox();
    newgl.setCoords(cbox.x, cbox.y-5, cbox.width, cbox.height);
    java.util.List<GlyphI> children = gl.getChildren();
    if (children != null) {
      for (GlyphI child  : children) {
        newgl.addChild(GlyphDragger.duplicateGlyph(child, col));
      }
    }
    return newgl;
  }

  public void setConstraint(NeoConstants.Orientation axis, boolean is_constrained) {
    if (axis == NeoConstants.Orientation.Vertical) {
      constrained[VERT] = is_constrained;
    }
    else if (axis == NeoConstants.Orientation.Horizontal) {
      constrained[HORIZ] = is_constrained;
    }
  }

  public boolean getConstraint(NeoConstants.Orientation axis) {
    if (axis == NeoConstants.Orientation.Vertical) {
      return constrained[VERT];
    }
    else if (axis == NeoConstants.Orientation.Horizontal) {
      return constrained[HORIZ];
    }
    else return false;
  }

  public void setUseCopy(boolean b) {
    this.drag_a_copy = b;
  }

  /**
   *  Add a glyph drag listener.
   *  Note: may want to change this to distinguish between listeners that
   *  want to know about DRAG_IN_PROGRESS events,
   *  versus listeners that just want to know about DRAG_STARTED and
   *  DRAG_ENDED events
   *  (since posting lots of DRAG_IN_PROGRESS events may be inefficient
   *     on the other hand, another way to deal with this is to recycle the same
   *     NeoGlyphDragEvent...).
   */
  public void addGlyphDragListener(NeoGlyphDragListener listener) {
    drag_listeners.add(listener);
  }

  public void removeGlyphDragListener(NeoGlyphDragListener listener) {
    drag_listeners.remove(listener);
  }

  public void removeAllListeners() {
    drag_listeners.clear();
  }

  public void notifyListeners(NeoGlyphDragEvent evt) {
    for (NeoGlyphDragListener listener : drag_listeners) {
      listener.heardGlyphDrag(evt);
    }
  }
}
