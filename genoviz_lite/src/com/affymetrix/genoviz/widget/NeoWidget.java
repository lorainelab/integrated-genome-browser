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

package com.affymetrix.genoviz.widget;

import com.affymetrix.genoviz.awt.NeoCanvas;

import com.affymetrix.genoviz.transform.ExponentialOneDimTransform;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.transform.LinearTwoDimTransform;
import com.affymetrix.genoviz.transform.OneDimLinearTransform;
import com.affymetrix.genoviz.transform.OneDimTransform;
import com.affymetrix.genoviz.bioviews.RubberBand;
import com.affymetrix.genoviz.bioviews.SceneII;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;

import com.affymetrix.genoviz.bioviews.WidgetAxis;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoViewMouseEvent;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.event.NeoWidgetEvent;
import com.affymetrix.genoviz.event.NeoWidgetListener;

import com.affymetrix.genoviz.util.NeoConstants.Orientation;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import javax.swing.JScrollBar;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Abstract incomplete implementation of NeoWidgetI.
 */
public abstract class NeoWidget extends NeoAbstractWidget
  implements NeoWidgetI, AdjustmentListener {

  protected NeoCanvas canvas;

  protected SceneII scene;

  protected View view;
  protected LinearTwoDimTransform trans;

  private int pixel_beg[] = new int[2];
  private int pixel_end[] = new int[2];
  protected int pixel_size[] = new int[2];

  // pixelblur is the amount of pixel space leeway given when finding overlaps
  protected int pixelblur = 2;
  protected int min_rubberband_size = 2;

  protected int zoomer_value[] = new int[2];
  protected int scroller_value[] = new int[2];
  protected int prev_zoomer_value[] = new int[2];
  protected int prev_scroller_value[] = new int[2];
  protected OneDimTransform zoomtrans[] = new OneDimTransform[2];
  protected OneDimTransform scrolltrans[] = new OneDimTransform[2];

  protected double zoomer_scale[] = new double[2];
  protected ZoomConstraint zoom_behavior[] = new ZoomConstraint[2];
  protected double zoom_coord[] = new double[2];

  protected ScaleConstraint scale_constraint[] = {
    ScaleConstraint.NONE, ScaleConstraint.NONE};
  protected int reshape_constraint[] = { FITWIDGET, FITWIDGET };

  protected ExpandBehavior expansion_behavior[] = {
    ExpandBehavior.NO_EXPAND, ExpandBehavior.NO_EXPAND };

  // constraints on transform scale
  protected boolean set_min_pix_per_coord[] = { false, false };
  protected boolean set_max_pix_per_coord[] = { false, false };
  protected double min_pixels_per_coord[] = new double[2];
  protected double max_pixels_per_coord[] = new double[2];
  // the equivalent values for max/min scrolling offset are obtained form
  //    the scene's coord box as local variables

  // caching transform values
  //    LinearTwoDimTransform is:
  //      pixel_loc = (pixels_per_coord * coord_loc) + pixel_offset
  protected double pixels_per_coord[] = new double[2];
  protected double pixel_offset[] = new double[2];

  protected double coords_per_pixel[] = new double[2];

  protected JScrollBar scroller[] = new JScrollBar[2]; // 0 = Primary, 1 = Secondary
  protected JSlider zoomer[] = new JSlider[2]; // 0 = Primary, 1 = Secondary

  protected RubberBand rband;
  protected Rectangle2D.Double bandbox;
  protected boolean rbActivated = false;

  protected boolean hscroll_show, vscroll_show;

  protected final static String hscroll_default_loc = "South";
  protected final static String vscroll_default_loc = "East";
  protected String hscroll_loc = hscroll_default_loc;
  protected String vscroll_loc = vscroll_default_loc;

  protected List<NeoRubberBandListener> rubberband_listeners = new CopyOnWriteArrayList<NeoRubberBandListener>();


  public NeoWidget() {
    this.setScrollIncrementBehavior(WidgetAxis.Primary, ScrollIncrementBehavior.NO_AUTO_SCROLL_INCREMENT);
    this.setScrollIncrementBehavior(WidgetAxis.Secondary, ScrollIncrementBehavior.NO_AUTO_SCROLL_INCREMENT);

    // start with default identity linear transform
    trans = new LinearTwoDimTransform();

//    // adding name for common default color -- should probably be in a static
//    //  class initialization method
//    addColor("nicePaleBlue", new Color(180, 250, 250)); //TODO: delete
  }

  /**
   * Destructor that unlocks graphic resources, cuts links.
   * Call only when the map is no longer being displayed.
   */
  @Override
  public void destroy() {

    super.destroy();
    if (canvas != null) {
      remove(canvas);
    }
    if (scroller != null) {
      if ( scroller[0] != null ) {
        scroller[0].removeAdjustmentListener(this);
      }
      if ( scroller[1] != null ) {
        scroller[1].removeAdjustmentListener(this);
      }
    }
    if (zoomer != null) {
      if ( zoomer[0] != null ) {
        zoomer[0].removeChangeListener(changeListener);
      }
      if ( zoomer[1] != null ) {
        zoomer[1].removeChangeListener(changeListener);
      }
    }
    scroller = null;
    zoomer = null;
    if (canvas != null) {
      if ( rband != null ) {
        canvas.removeMouseListener(rband);
        canvas.removeMouseMotionListener(rband);
      }
    canvas = null;
    }
    rband = null;
    view.destroy();
  }

  protected Orientation orient;

  @Override
  public Orientation getOrientation() {
    return orient;
  }

  @Override
  public boolean isHorizontal() {
    return orient == Orientation.Horizontal;
  }

  public void setReshapeBehavior(int id, int behavior) {
    reshape_constraint[id] = behavior;
  }

  public int getReshapeBehavior(WidgetAxis axis) {
    return reshape_constraint[axis.ordinal()];
  }

  public SceneII getScene() { return scene; }

  public NeoCanvas getNeoCanvas() { return canvas; }

  @Override
  public ViewI getView() { return view; }

  /**
   *  Sets the bounds.
   *  Unlike {@link #setBounds(int, int, int)}, this does NOT add
   *  1 to the width.
   *
   */
  public void setFloatBounds(WidgetAxis axis, double start, double end) {
    final double size = end - start;
    if (size < 0) {
      return;
    }
    final Rectangle2D.Double sbox = scene.getCoordBox();
    if (axis == WidgetAxis.Primary) {
      if (isHorizontal()) {
        scene.setCoords(start, sbox.y, size, sbox.height);
      } else {
        scene.setCoords(sbox.x, start, sbox.width, size);
      }
    }
    else { // Secondary axis
      if (isHorizontal()) {
        scene.setCoords(sbox.x, start, sbox.width, size);
      } else {
        scene.setCoords(start, sbox.y, size, sbox.height);
      }
    }
    adjustScroller(axis);
  }

 /**
  *  Adds 1 to width when calculating coord box.
  *  For example setting Primary bounds to 0, 74, actually sets coord box to
  *  cbox.x = 0, cbox.width = 75
  */
  //TODO: stop adding one?
  public void setBounds(WidgetAxis axis, int start, int end) {
    final double size = end-start+1;
    final Rectangle2D.Double sbox = scene.getCoordBox();
    if (axis == WidgetAxis.Primary) {
      if (isHorizontal()) {
        scene.setCoords(start, sbox.y, size, sbox.height);
      } else {
        scene.setCoords(sbox.x, start, sbox.width, size);
      }
    }
    else {
      if (isHorizontal()) {
        scene.setCoords(sbox.x, start, sbox.width, size);
      } else {
        scene.setCoords(sbox.x, start, sbox.width, size);
      }
    }
    adjustScroller(axis);
  }


  /**
   * gets all the items in the widget that are visible.
   * <p><em><strong>We will change this name soon.</strong></em>
   * We should make this <code>getDisplayedItems()</code> instead,
   * and reserve "visible" for <code>getVisibleItem(x, y)</code>
   * which makes sure the item is not only drawn/displayed,
   * but also visible (unobscured by other items).
   *
   * @return a List of the items.
   */
  public List<GlyphI> getVisibleItems() {
    // SHOULD THIS BE getViewBounds() INSTEAD!!??!!!
    //    WHICH GLYPHS DO WE REALLY WANT TO RETURN????
    java.awt.geom.Rectangle2D.Double coordrect = getCoordBounds();
    List<GlyphI> pickvect = new ArrayList<GlyphI>();
    scene.pickTraversal(coordrect, pickvect, view);
    return pickvect;
  }


  /**
   *  Retrieve a List of all drawn glyphs that overlap
   *  the coordinate rectangle coordrect.
   */
  public List<GlyphI> getItemsByCoord(java.awt.geom.Rectangle2D.Double coordrect) {
    List<GlyphI> pickvect = new ArrayList<GlyphI>();
    scene.pickTraversal(coordrect, pickvect, view);
    return pickvect;
  }

  /**
   *  retrieve a List of all drawn glyphs that overlap
   *  the pixel point x, y.
   */
  public List<GlyphI> getItemsByPixel(int x, int y) {
    Rectangle pixrect = new Rectangle(x-this.pixelblur, y-this.pixelblur,
        2*this.pixelblur, 2*this.pixelblur);
    java.awt.geom.Rectangle2D.Double coordrect = new java.awt.geom.Rectangle2D.Double();
    coordrect = view.transformToCoords(pixrect, coordrect);
    return this.getItemsByCoord(coordrect);
  }

  /**
   * gets the items that overlap a given rectangle of pixels.
   *
   * @param pixrect a rectangle in pixel space
   * @return the overlapping glyphs
   */
  public List<GlyphI> getItems(Rectangle pixrect) {
    // no pixelblur for region selection
    java.awt.geom.Rectangle2D.Double coordrect = new java.awt.geom.Rectangle2D.Double();
    coordrect = view.transformToCoords(pixrect, coordrect);
    return getItemsByCoord(coordrect);
  }

  /**
   * Gets the items overlapping a given point in coordinate space.
   * A fuzz factor, in pixels, is added around the point.
   *
   * @return the overlapping glyphs
   * @see com.affymetrix.genoviz.widget.NeoWidgetI#setPixelFuzziness
   */
  @Override
  public List<GlyphI> getItems(double x, double y) {
    Rectangle2D.Double coordrect = new Rectangle2D.Double(x, y, 1, 1);
    if (0 < pixelblur) {
      Rectangle pixrect = new Rectangle();
      pixrect = view.transformToPixels(coordrect, pixrect);
      pixrect.setBounds(pixrect.x-pixelblur, pixrect.y-pixelblur,
          pixrect.width+2*pixelblur, pixrect.height+2*pixelblur);
      coordrect = view.transformToCoords(pixrect, coordrect);
    }
    return getItemsByCoord(coordrect);
  }

  /**
   * Updates the visual appearance of the widget.  It is important to call
   * this method to view any externally introduced changes in widget
   * appearance since last call to updateWidget()
   */
  @Override
  public void updateWidget() {
    updateWidget(false);
  }

  /**
   * Updates the visual appearance of the widget
   *@param full_update  if true, then force redrawing of entire widget.
   */
  @Override
  public void updateWidget(boolean full_update) {
    if (canvas == null) {
      return; // in case destroy() was called.
    } // in case destroy() was called.
    if (full_update) {
      scene.maxDamage();
    }
    canvas.repaint();
  }

  @Override
  public void setRubberBandBehavior(boolean activate) {
    if (canvas == null) {
      return; // in case destroy() was called.
    } // in case destroy() was called.
    if (activate && !rbActivated) {
      // rubberband listens to canvas for MOUSE_DOWN, MOUSE_DRAG, MOUSE_UP events
      canvas.addMouseListener(rband);
      canvas.addMouseMotionListener(rband);
      rbActivated = true;
      rband.clearRubberBand();
    }
    else if (!activate && rbActivated)  {
      canvas.removeMouseListener(rband);
      canvas.removeMouseMotionListener(rband);
      rbActivated = false;
      rband.clearRubberBand();
    }
  }

  public boolean getRubberBandBehavior() {
    return rbActivated;
  }

  /** This is the only place <code>pixel_*</code> should change. */
  protected void setPixelBounds() {
    if (canvas == null) {
      return; // in case destroy() was called.
    } // in case destroy() was called.
    pixel_beg[Xint] = 0;
    pixel_beg[Yint] = 0;
    pixel_size[Xint] = canvas.getSize().width;
    pixel_size[Yint] = canvas.getSize().height;

    pixel_end[Xint] = pixel_size[Xint] - pixel_beg[Xint];
    pixel_end[Yint] = pixel_size[Yint] - pixel_beg[Yint];

    Rectangle bbox = new Rectangle(pixel_beg[Xint], pixel_beg[Yint],
        pixel_size[Xint], pixel_size[Yint]);
    view.setPixelBox(bbox);
  }

  public Rectangle getPixelBounds(GlyphI gl) {
    return gl.getPixelBox(view);
  }

  public Rectangle2D.Double getCoordBounds() {
    return scene.getCoordBox();
  }

  public Rectangle2D.Double getViewBounds() {
    return view.getCoordBox();
  }

  @Override
  public Rectangle2D.Double getCoordBounds(GlyphI gl) {
    return gl.getCoordBox();
  }

  @Override
  public void setPixelFuzziness(int blur) {
    pixelblur = blur;
  }

  @Override
  public int getPixelFuzziness() {
    return pixelblur;
  }

  @Override
  public void setScroller(WidgetAxis dim, JScrollBar adj) {
    final int id = dim.ordinal();
    if (adj == null) {
      throw new IllegalArgumentException("NeoWidget.setScroller() requires " +
          "an Adjustable argument, was passed a null instead");
    }
    // can only be one scroller for each range -- remove this from previous
    // scroller's listeners
    if (scroller[id] != null) {
      scroller[id].removeAdjustmentListener(this);
    }
    scroller[id] = adj;
    scrolltrans[id] = new OneDimLinearTransform();
    scroller[id].addAdjustmentListener(this);
  }

  @Override
  public void setZoomer(WidgetAxis dim, JSlider slider) {
    final int id = dim.ordinal();
    if (slider == null) {
      throw new IllegalArgumentException("NeoWidget.setZoomer() requires " +
          "an Adjustable argument, was passed a null instead");
    }
    // can only be one zoomer for each range -- remove this from previous
    // zoomer's listeners
    if (zoomer[id] != null) {
      zoomer[id].removeChangeListener(changeListener);
    }
    zoomer[id] = slider;
    slider.setMinimum(0);
    slider.setExtent(10);
    slider.setMaximum(200 + slider.getExtent()); //TODO: what is this for?
    slider.setValue(0);

    // GAH 6-29-99
    // setting max of exponential tranform to (max - visible amount) to
    // compensate for the fact that the maximum for the value
    // is really the scrollbar maximum minus
    // the visible amount (the thumb)
    zoomtrans[id] = new ExponentialOneDimTransform(
      min_pixels_per_coord[id],
      max_pixels_per_coord[id],
      slider.getMinimum(),
      slider.getMaximum() - slider.getExtent());
    slider.addChangeListener(changeListener);
  }

  //TODO: maybe should return coord it _actually_ scrolled to
  @Override
  public void scroll(WidgetAxis dim, double coord_value) {
    // double new_coord_value = 0;
    // double prev_pixel_value;
    // a boolean to enforce scroller adjustment if bumping up against
    //   map edges, otherwise sometimes the scrollers don't get adjusted
    //   in these cases
    boolean force_scroller_adjust = false;

    // The following block is trying to constrain scrolling
    // to stay inside coordinate bounds.
    //TODO: can it be simplified?
    {
      Rectangle2D.Double scene_coords = getCoordBounds();
      Rectangle2D.Double view_coords = view.getCoordBox();
      double min_coord, max_coord;

      //TODO: check this association of Primary with x.  Is that right?
      if (dim == WidgetAxis.Primary) {
        min_coord = scene_coords.x;
        max_coord = scene_coords.x + scene_coords.width - view_coords.width;
      }
      else {
        min_coord = scene_coords.y;
        max_coord = scene_coords.y + scene_coords.height - view_coords.height;
      }
      if (min_coord >= max_coord) {
        // this will be true iff view_coords >= scene_coords in id dimension, in
        // which case we are attempting to scroll something that is <=
        // the view -- therefore center it?

        double scene_center;
        if (dim == WidgetAxis.Primary) {
          scene_center = scene_coords.x + (scene_coords.width/2);
          coord_value = scene_center - (view_coords.width/2);
          force_scroller_adjust = true;
        }
        else {
          scene_center = scene_coords.y + (scene_coords.height/2);
          coord_value = scene_center - (view_coords.height/2);
          force_scroller_adjust = true;
        }
      }

      else if (coord_value < min_coord) {
        coord_value = min_coord;
        force_scroller_adjust = true;
      }
      else if (coord_value > max_coord) {
        coord_value = max_coord;
        force_scroller_adjust = true;
      }
    }

    final int ordinal = dim.ordinal();
    double pixel_value;
    if (dim == WidgetAxis.Primary) {
      pixels_per_coord[ordinal] = trans.getScaleX();
      pixel_value = coord_value * pixels_per_coord[ordinal];
      trans.setOffsetX(-pixel_value);
    }
    else {
      pixels_per_coord[ordinal] = trans.getScaleY();
      pixel_value = coord_value * pixels_per_coord[ordinal];
      trans.setOffsetY(-pixel_value);
    }

    if (force_scroller_adjust || coord_value != scroller_value[ordinal]) {
      adjustScroller(dim);
    }

    view.calcCoordBox();
  }


  public void adjustScroller(WidgetAxis dim) {
    final int ordinal = dim.ordinal();
    if (scroller[ordinal] == null) { return; }
    // GAH 1-15-2004
    // trying same trick as in adjustZoomer(), supressing event kickback changing
    //   original scroll value
    // not sure how necessary this suppression is with adjustScroller(), could just
    //   be a problem with adjustZoomer(), but trying for now -- need to watch
    //   for any side effects...
    scroller[ordinal].removeAdjustmentListener(this);

    double coord_beg, coord_end, coord_size;
    java.awt.geom.Rectangle2D.Double scenebox = scene.getCoordBox();
    if (ordinal == Xint)  {
      pixel_offset[ordinal] = -1 * trans.getOffsetX();
      coord_beg = scenebox.x;
      coord_size = scenebox.width;
    }
    else {
      coord_beg = scenebox.y;
      coord_size = scenebox.height;
      pixel_offset[ordinal] = -1 * trans.getOffsetY();

    }
    coord_end = coord_beg + coord_size;

    double coord_offset = pixel_offset[ordinal] * coords_per_pixel[ordinal];
    double visible_coords = coords_per_pixel[ordinal] * pixel_size[ordinal];

    // If there is more visible than the maximum coordinate size,
    //  max-out the scrollbar.
    BoundedRangeModel scrollModel = scroller[ordinal].getModel();
    boolean valueIsAdjusting = false;
    if (zoomer[ordinal] != null) {
      if (zoomer[ordinal].getValueIsAdjusting()) {
        valueIsAdjusting = true;
      }
    }

    
    // Since this code can be the result of the user sliding
    // the zoomer, use the zoomer's value-is-adjusting flag.
    if (scrollModel != null) {
      valueIsAdjusting |= scrollModel.getValueIsAdjusting();
      if (coord_size < visible_coords) {
        scrollModel.setRangeProperties(
          scrollModel.getMinimum(), scrollModel.getMaximum() - scrollModel.getMinimum(),
          scrollModel.getMinimum(), scrollModel.getMaximum(),
          valueIsAdjusting);
      } else {
        // value, extent, min, max
        scrollModel.setRangeProperties(
          (int) coord_offset, (int) visible_coords,
          (int) coord_beg, (int) coord_end,
          valueIsAdjusting);
      }
    }

    if (scroll_behavior[ordinal] == ScrollIncrementBehavior.AUTO_SCROLL_INCREMENT) {
      if (coords_per_pixel[ordinal] > 1) {
        scroller[ordinal].setUnitIncrement((int)coords_per_pixel[ordinal]);
        scroller[ordinal].setBlockIncrement((int)(5*coords_per_pixel[ordinal]));
      }
      else {
        scroller[ordinal].setUnitIncrement(1);
        scroller[ordinal].setBlockIncrement(5);
      }
    }
    else if (scroll_behavior[ordinal] == ScrollIncrementBehavior.AUTO_SCROLL_HALF_PAGE)  {
      if (coords_per_pixel[ordinal] > 1) {
        scroller[ordinal].setUnitIncrement((int)(5*coords_per_pixel[ordinal]));
        if (ordinal == Xint) { scroller[ordinal].setBlockIncrement((int)(getViewBounds().width/2)); }
        else if (ordinal == Yint)  { scroller[ordinal].setBlockIncrement((int)(getViewBounds().height/2)); }
      }
      else {
        scroller[ordinal].setUnitIncrement(1);
        scroller[ordinal].setBlockIncrement(5);
      }
    }
    scroller[ordinal].addAdjustmentListener(this);
  }

  public void adjustZoomer(WidgetAxis dim) {
    int id = dim.ordinal();
    if (zoomer[id] == null) { return; }
    if (pixels_per_coord[id] == zoomer_value[id]) { return; }
    // GAH 1-15-2004  trying to deal with problem where precisely setting zoom to Z
    //   via NeoWidget.zoom() call ends up _not_ precisely setting zoom, because
    //   NeoWidget.zoom() triggers adjustZoomer(), and the zoomer in turn sends an
    //   adjustment change event back to NeoWidget(), but because zoomer can only
    //   change in pixel increments, the change event forces _another_ zoom to
    //   some scale factor near Z but not quite Z...
    // Therefore try bracketing most of adjustZoomer() method with removing and then
    //   re-adding NeoWidget as listener to zoomer
    zoomer[id].removeChangeListener(changeListener);

    // otherwise need to do a zoomtrans[id].inverseTransform() to
    //  set correct zoomer_scroll_value[id]
    zoomer_scale[id] = pixels_per_coord[id];
    zoomer_value[id] = (int)zoomtrans[id].inverseTransform(zoomer_scale[id]);
    // catching bug where zoomer_value gets negative values for weird
    //   zoomtrans (like when zoom_max and zoom_min are equal)
    // zoomer_value is NOT scale --
    //   goes through zoomtrans transform to get scale
    if (zoomer_value[id] < 0)   { zoomer_value[id] = 0; }
    zoomer[id].setValue(zoomer_value[id]);

    zoomer[id].addChangeListener(changeListener);
  }

  public double getZoom(WidgetAxis dim) {
    return pixels_per_coord[dim.ordinal()];
  }

  @Override
  public double getMinZoom(WidgetAxis dim) {
    return min_pixels_per_coord[dim.ordinal()];
  }

  @Override
  public double getMaxZoom(WidgetAxis dim) {
    return max_pixels_per_coord[dim.ordinal()];
  }

  @Override
  public void setMinZoom(WidgetAxis dim, double min) {
    final int ordinal = dim.ordinal();
    double prev_scale;
    boolean scale_at_min = false;
    if (dim == WidgetAxis.Primary) {
      prev_scale = trans.getScaleX();
    } else {
      prev_scale = trans.getScaleY();
    }
    if (prev_scale == min_pixels_per_coord[ordinal]) {
      scale_at_min = true;
    }

    set_min_pix_per_coord[ordinal] = true;
    min_pixels_per_coord[ordinal] = min;
    // calling stretchToFit only for side effects...
    //   (like setting Adjustable appearance)
    stretchToFit(false, false);

    // testing scale adjustment if outside allowable range
    //   might want to push this down into stretchToFit() -- GAH 12/14/97
    double pix_per_coord;
    if (dim == WidgetAxis.Primary) {
      pix_per_coord = trans.getScaleX();
    } else {
      pix_per_coord = trans.getScaleY();
    }
    if (pix_per_coord < min || scale_at_min) {
      zoom(dim, min);
    }

  }

  @Override
  public void setMaxZoom(WidgetAxis dim, double max) {

    int ordinal = dim.ordinal();
    // assuming that if scale is already at max zoom, want to
    //   change scale to keep it at max zoom
    double prev_scale;
    if (ordinal == Xint) { prev_scale = trans.getScaleX(); }
    else         { prev_scale = trans.getScaleY(); }
    boolean scale_at_max = (prev_scale == max_pixels_per_coord[ordinal]);

    set_max_pix_per_coord[ordinal] = true;
    max_pixels_per_coord[ordinal] = max;
    // calling stretchToFit() only for side effects...
    stretchToFit(false, false);

    // testing scale adjustment if outside allowable range
    //   might want to push this down into stretchToFit() -- GAH 12/14/97
    double pix_per_coord;
    if (ordinal == Xint) { pix_per_coord = trans.getScaleX(); }
    else         { pix_per_coord = trans.getScaleY(); }
    if (pix_per_coord > max || scale_at_max) {
      zoom(dim, max);
    }
  }


//  @Override
//  public void setVisibility(GlyphI gl, boolean isVisible) {
//    scene.setVisibility(gl, isVisible);
//  }
//
//  @Override
//  public void setVisibility(List<GlyphI> glyphs, boolean isVisible) {
//    for (GlyphI g : glyphs) {
//      setVisibility(g, isVisible);
//    }
//  }

  /****************************************/
  /** Methods for dealing with selecion **/
  /****************************************/

  /**
   * adds an object to the selection.
   * @see com.affymetrix.genoviz.widget.NeoWidgetI#select
   */
  @Override
  public void select(GlyphI g) {
    scene.select(g);
    if ( g.isSelected() && !selected.contains(g)) {
      selected.add(g);
    }
  }

  @Override
  public void deselect(GlyphI g) {
    scene.deselect(g);
    selected.remove(g);
  }

  @Override
  public void clearWidget() {
    super.clearWidget();
  }

  /**
   * Scale constraints are currently only considered during
   *    zooming with zoomer[] adjustables
   */
  @Override
  public void setScaleConstraint(WidgetAxis axis, ScaleConstraint constraint) {
    scale_constraint[axis.ordinal()] = constraint;
  }

  @Override
  public void setZoomBehavior(WidgetAxis axis, ZoomConstraint constraint) {
    switch (constraint) {
      case CONSTRAIN_START:
      case CONSTRAIN_MIDDLE:
      case CONSTRAIN_END:
        break;
      default:
        throw new IllegalArgumentException("Invalid constraint.");
    }
    zoom_behavior[axis.ordinal()] = constraint;
  }
  public ZoomConstraint getZoomBehavior(int axisid) {
    return zoom_behavior[axisid];
  }

  public double getZoomCoord(int axisid) {
    return zoom_coord[axisid];
  }

  @Override
  public void setZoomBehavior(WidgetAxis axis, ZoomConstraint constraint, double coord) {
    //TODO: support other constraints, or get rid of parameter
    if (ZoomConstraint.CONSTRAIN_COORD != constraint) {
      throw new IllegalArgumentException("Invalid constraint.");
    }
    zoom_behavior[axis.ordinal()] = constraint;
    zoom_coord[axis.ordinal()] = coord;
  }


  ChangeListener changeListener = new ChangeListener() {

    @Override
    public void stateChanged(ChangeEvent e) {
      Object source = e.getSource();
      if (source == zoomer[Xint] || source == zoomer[Yint]) {
        WidgetAxis dim;
        if (source == zoomer[Xint]) {
          dim = WidgetAxis.Primary;
        } else {
          dim = WidgetAxis.Secondary;
        }
        int id = dim.ordinal();
        zoomer_value[id] = zoomer[id].getValue();
        if (zoomer_value[id] == prev_zoomer_value[id]) {
          return;
        }
        zoomer_scale[id] = zoomtrans[id].transform(zoomer_value[id]);
        if (scale_constraint[id] == ScaleConstraint.INTEGRAL_PIXELS ||
          scale_constraint[id] == ScaleConstraint.INTEGRAL_ALL) {
          if (zoomer_scale[id] >= 1) {
            zoomer_scale[id] = (int) (zoomer_scale[id] + .0001);
          }
        }
        zoom(dim, zoomer_scale[id]);
        updateWidget();
        prev_zoomer_value[id] = zoomer_value[id];
      }
    }
  };

  @Override
  public void adjustmentValueChanged(AdjustmentEvent evt) {
     Adjustable source = evt.getAdjustable();

    WidgetAxis dim;
    if (source == scroller[WidgetAxis.Primary.ordinal()]) {
      dim = WidgetAxis.Primary;
    } else if (source == scroller[WidgetAxis.Secondary.ordinal()]) {
      dim = WidgetAxis.Secondary;
    } else {
      return;
    }
    int id = dim.ordinal();

    scroller_value[id] = (int) scrolltrans[id].transform(source.getValue());

    if (scroller_value[id] != prev_scroller_value[id]) {
      scroll(dim, scroller_value[id]);
      updateWidget();
      prev_scroller_value[id] = scroller_value[id];
    }
  }

  public void setScrollTransform(WidgetAxis dim, OneDimTransform trans) {
    scrolltrans[dim.ordinal()] = trans;
  }


  /**
   * determine whether to call <code>getItem()</code> or <code>getItems()</code>.
   * If <code>item count == 1</code>
   * there will be an additional object creation time overhead
   * when calling <code>getItems()</code>.
   *
   * @param datamodel associated with one or more Glyphs.
   * @return number of Glyphs associated with the given data model.
   * @see #getItem
   * @see #getItems
   */
  public int getItemCount(Object datamodel) {
    /*
     * Could just store everything (even single items) as Lists,
     * but we want to avoid the memory overhead.
     * A better way to deal with this might be to maintain separate hashes,
     * one for Lists and one for GlyphI?  Hmm, maybe not...
     */
    Object result = model_hash.get(datamodel);
    if (result instanceof GlyphI) {
      return 1;
    }
    else if (result instanceof List) {
      return ((List)result).size();
    }
    else {
      return 0;
    }
  }

  /**
   * Get the items associated with a particular data model.
   *
   * @param datamodel being visualized.
   * @return a List of all the glyphs tied to the given data model.
   */
  @Override
  @SuppressWarnings("unchecked")
   public List<GlyphI> getItems(Object datamodel) {
    Object result = model_hash.get(datamodel);
    if (result instanceof List) {
      return (List<GlyphI>)result;
    } else if (result == null) {
      return Collections.<GlyphI>emptyList();
    }
    else {
      return Arrays.<GlyphI>asList((GlyphI)result);
    }
  }


  /**
   * Returns the GlyphI which is associated with the given object.
   *  If there is more than one glyph associated with the datamodel,
   *  then return glyph that was most recently associated
   */
  @Override
  public GlyphI getItem(Object datamodel) {
    Object result = model_hash.get(datamodel);
    if (result instanceof GlyphI) {
      return (GlyphI)result;
    }
    else if (result instanceof List && ! ((List)result).isEmpty()) {
      List vec = (List)result;
      return (GlyphI) vec.get(vec.size()-1);
    }
    else {
      return null;
    }
  }

  @Override
  public void setSelectionAppearance(SceneII.SelectType select_behavior) {
    scene.setSelectionAppearance( select_behavior );
  }

  @Override
  public SceneII.SelectType getSelectionAppearance() {
    return scene.getSelectionAppearance();
  }

  @Override
  public void setSelectionColor(Color col) {
    scene.setSelectionColor(col);
  }

  @Override
  public Color getSelectionColor() {
    return scene.getSelectionColor();
  }

  @Override
  public void zoom(WidgetAxis dim, double zoom_scale) {
    final int ordinal = dim.ordinal();

    // Make sure zoom_scale is within range.
    zoom_scale = Math.min(zoom_scale, max_pixels_per_coord[ordinal]);
    zoom_scale = Math.max(zoom_scale, min_pixels_per_coord[ordinal]);

    double prev_pixels_per_coord = pixels_per_coord[ordinal];
    pixels_per_coord[ordinal] = zoom_scale;
    coords_per_pixel[ordinal] = 1/zoom_scale;
    if (pixels_per_coord[ordinal] == prev_pixels_per_coord) {
      return;
    }

    Rectangle2D.Double scenebox = scene.getCoordBox();
    double prev_coords_per_pixel = 1/prev_pixels_per_coord;
    double prev_pixel_offset;
    double coord_beg, coord_end, coord_size;
    double fixed_coord, fixed_pixel;

    if (ordinal == Xint) {
      prev_pixel_offset = -1 * trans.getOffsetX();
      coord_beg = scenebox.x;
      coord_size = scenebox.width;
    }
    else {
      prev_pixel_offset = -1 * trans.getOffsetY();
      coord_beg = scenebox.y;
      coord_size = scenebox.height;
    }
    coord_end = coord_beg + coord_size;

    double prev_coord_offset = prev_pixel_offset * prev_coords_per_pixel;
    double prev_visible_coords = prev_coords_per_pixel * pixel_size[ordinal];

    switch (zoom_behavior[ordinal]) {
      case CONSTRAIN_MIDDLE:
        fixed_coord = prev_coord_offset + (prev_visible_coords / 2.0f);
        break;
      case CONSTRAIN_START:
        fixed_coord = prev_coord_offset;
        break;
      case CONSTRAIN_END:
        fixed_coord = prev_coord_offset + prev_visible_coords;
        break;
      case CONSTRAIN_COORD:
        fixed_coord = zoom_coord[ordinal];
        break;
      default:
        throw new IllegalArgumentException("zoom behavior value invalid");
    }

    double visible_coords = coords_per_pixel[ordinal] * pixel_size[ordinal];
    fixed_pixel = prev_pixels_per_coord * fixed_coord - prev_pixel_offset;
    double real_pix_offset = fixed_pixel - pixels_per_coord[ordinal] * fixed_coord;
    double coord_offset = -real_pix_offset * coords_per_pixel[ordinal];

    if ((scale_constraint[ordinal] == ScaleConstraint.INTEGRAL_PIXELS ||
          scale_constraint[ordinal] == ScaleConstraint.INTEGRAL_ALL) && zoom_scale >= 1) {
      coord_offset = (int)coord_offset;
    }


    double last_coord_displayed = coord_offset + visible_coords;

    if (!((coord_offset < coord_beg)  && (last_coord_displayed > coord_end))) {
      if (coord_offset < coord_beg) {
        coord_offset = coord_beg;
        last_coord_displayed = coord_offset + visible_coords;
      }
      if (last_coord_displayed > coord_end) {
        coord_offset = coord_end - visible_coords;
      }
    }

    if ((scale_constraint[ordinal] == ScaleConstraint.INTEGRAL_PIXELS ||
          scale_constraint[ordinal] == ScaleConstraint.INTEGRAL_ALL) && zoom_scale >= 1) {
      pixel_offset[ordinal] =
        (int)(Math.ceil(coord_offset / coords_per_pixel[ordinal]));
    }
    else {
      pixel_offset[ordinal] = coord_offset / coords_per_pixel[ordinal];
    }

    if (ordinal == Xint) {
      trans.setOffsetX(-pixel_offset[ordinal]);
      trans.setScaleX(pixels_per_coord[ordinal]);
    }
    else {
      trans.setOffsetY(-pixel_offset[ordinal]);
      trans.setScaleY(pixels_per_coord[ordinal]);
    }
    if (zoom_scale != zoomer_scale[ordinal]) {
      adjustZoomer(WidgetAxis.values()[ordinal]);
    }
    adjustScroller(dim);

    view.calcCoordBox();
  }

  @Override
  public void toFrontOfSiblings(GlyphI glyph) {
    scene.toFrontOfSiblings(glyph);
  }

  @Override
  public void toBackOfSiblings(GlyphI glyph) {
    scene.toBackOfSiblings(glyph);
  }

  public boolean isOnTop(GlyphI gl) {
    if (!gl.isVisible()) { return false; }
    java.awt.geom.Rectangle2D.Double cbox = gl.getCoordBox();
    List<GlyphI> pickvect = new ArrayList<GlyphI>();
    getScene().pickTraversal(cbox, pickvect, getView());
    if (pickvect.size() == 0) {
      // something very strange is going on if pickvect doesn't at
      //    least pick up the glyph itself...
      return false;
    }
    if (gl == pickvect.get(pickvect.size()-1)) {
      // if gl is last element in pickvect then it was drawn last,
      //   and therefore is on top and unobscured
      return true;
    }
    // otherwise there are other glyphs that hit its coordbox, and are
    //   being drawn after it, and therefore it is at least partially obscured
    return false;
  }

  public boolean isFullyWithinView(GlyphI gl) {

    // a) glyph.isVisible() == true
    if (!gl.isVisible())  { return false; }

    // b) the entire glyph is within the bounds of the region of
    //    the map drawn on screen iff union of view coord box and
    //    glyph coord box is same size as view coord box
    Rectangle2D.Double viewbox = getView().getCoordBox();
    Rectangle2D.Double glyphbox = gl.getCoordBox();
    if (!glyphbox.intersects(viewbox)) { return false; }
    Rectangle2D.Double unionbox = (Rectangle2D.Double) viewbox.createUnion(glyphbox);

    if (unionbox.equals(viewbox)) { return true; }
    return false;
  }

  public boolean isPartiallyWithinView(GlyphI gl) {
    // a) glyph.isVisible() == true
    if (!gl.isVisible())  { return false; }

    // b) part of the glyph is within the bounds of the region of
    //    the map drawn on screen iff view coord box hits glyph
    Rectangle2D.Double viewbox = getView().getCoordBox();
    return gl.hit(viewbox, this.getView());
  }

  @Override
  public void heardMouseEvent(MouseEvent evt) {
    if (! (evt instanceof NeoViewMouseEvent)) { return; }
    NeoViewMouseEvent e = (NeoViewMouseEvent)evt;

    // NeoViewMouseEvents return the _View_ they occured on as the source,
    // rather than the Component like most AWTEvents
    Object source = e.getSource();

    if (source != view) { return; }
    int id = e.getID();

    NeoMouseEvent nevt =
      new NeoMouseEvent(e, this, -1, e.getCoordX(), e.getCoordY());
    // translating from NeoCanvas pixel location to
    //     widget pixel location
    Rectangle bnds = view.getComponent().getBounds();
    nevt.translatePoint(bnds.x, bnds.y);

    if (mouse_listeners.size() > 0) {
      for (MouseListener ml : mouse_listeners) {
        if (id == MouseEvent.MOUSE_CLICKED) { ml.mouseClicked(nevt); }
        else if (id == MouseEvent.MOUSE_ENTERED) { ml.mouseEntered(nevt); }
        else if (id == MouseEvent.MOUSE_EXITED) { ml.mouseExited(nevt); }
        else if (id == MouseEvent.MOUSE_PRESSED) { ml.mousePressed(nevt); }
        else if (id == MouseEvent.MOUSE_RELEASED) { ml.mouseReleased(nevt);}
      }
    }
    if (mouse_motion_listeners.size() > 0) {
      for (MouseMotionListener mml : mouse_motion_listeners) {
        if (id == MouseEvent.MOUSE_DRAGGED) { mml.mouseDragged(nevt); }
        else if (id == MouseEvent.MOUSE_MOVED) { mml.mouseMoved(nevt); }
      }
    }
  }

  public void addRubberBandListener(NeoRubberBandListener l) {
    if (!rubberband_listeners.contains(l)) {
      rubberband_listeners.add(l);
    }
  }

  public void removeRubberBandListener(NeoRubberBandListener l) {
    rubberband_listeners.remove(l);
  }

  private NeoWidgetListener listeners = null;

  @Override
  public void addWidgetListener( NeoWidgetListener l ) {
    listeners = NeoEventMulticaster.add( listeners, l );
  }

  @Override
  public void removeWidgetListener( NeoWidgetListener l ) {
    listeners = NeoEventMulticaster.remove( listeners, l );
  }

  /**
   * Notify all the listeners that an event has occurred.
   */
  protected void fireNeoWidgetEvent( NeoWidgetEvent e ) {
    if ( null != listeners ) {
      listeners.widgetCleared( e );
    }
  }
}
