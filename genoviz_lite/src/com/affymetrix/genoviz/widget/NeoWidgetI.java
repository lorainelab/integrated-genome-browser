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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.SceneI;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.WidgetAxis;
import com.affymetrix.genoviz.event.NeoWidgetListener;
import com.affymetrix.genoviz.util.Orientation;
import java.awt.Color;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

/**
 * This interface represents the functionality that is common to all widgets.
 * <p>
 * All widgets include notions of axis, establishing coordinate
 * space, indicating bounds, panning, zooming, selecting and placing
 * items at positions,establishing window resize behavior, 
 * and managing event handlers.
 * <p>
 * Note: this interface does <em>not</em> extend Component,
 * However, the current implementations of NeoWidgetI <em>do</em> all extend
 * Component.
 * </p>
 */
public interface NeoWidgetI {
  
  static final int Xint = XY.X.ordinal();
  static final int Yint = XY.Y.ordinal();

  //The following variables exist only to ease transition from genoviz to genovizLite
  public final static XY X = XY.X;
  public final static XY Y = XY.Y;
  
  
  /**
   * Retrieves the orientation.  If {@link Orientation#Horizontal},
   * then {@link WidgetAxis#Range} corresponds to {@link XY#X}, and
   * {@link WidgetAxis#Offset} corresponds to {@link XY#Y}.
   * @return
   */
  public Orientation getOrientation();
  
  /** @return true if {@link #getOrientation()} is {@link Orientation#Horizontal} */
  public boolean isHorizontal();

  public WidgetAxis toWidgetAxis(XY pixelAxis);
  
  public XY toPixelAxis(WidgetAxis widgetAxis);
  
  /**
   * Indicates that the widget should be resized
   * to fit within a container
   * whenever that container is resized.
   */
  public static final int FITWIDGET = 5; //TODO: make an enum, or use a boolean

  /**
   * Sets the background Color property.
   *
   * @param theColor Color to set the background.
   */
  public void setBackground(Color theColor);

  /**
   * Returns the current setting of the background Color property.
   *
   * @return the background Color
   */
  public Color getBackground();

  /**
   * Sets the foreground Color property.
   *
   * @param theColor <code>Color</code> to set the foreground.
   *
   */
  public void setForeground(Color theColor);

  /**
   * Returns the current setting of the foreground Color property.
   *
   * @return the foreground <code>Color</code>
   */
  public Color getForeground();

  /**
   * Adjusts this widget such that the <code>View</code>, scrollbars, etc., fit
   * within the current bounds of the widget.
   *
   * @param xstretch the boolean determines whether stretchToFit
   *   is applied along the x-axis. ({@link WidgetAxis#Range} if horizontal)
   * @param ystretch the boolean determines whether stretchToFit
   *   is applied along the y-axis. ({@link WidgetAxis#Offset} if horizontal).
   */
  public void stretchToFit(boolean xstretch, boolean ystretch);


  /** Get the view for this widget. */
  public ViewI getView();

  /**
   * Associates a slider
   * to control zooming along the specified axis.
   *
   * @param dim identifies the axis of zooming.
   * @param slider a slider to be associated with the axis.
   */
  public void setZoomer(WidgetAxis dim, JSlider slider);


  /**
   * To be called when the object is no longer needed. Eliminate some references, as is necessary
   * for garbage collection to occur.
   */
  public void destroy();

  /**
   * Associates a JScrollBar
   * to control scrolling along the specified axis.
   *
   * @param dim identifies the axis of scrolling.
   * @param adj a scrollbar
   */
  public void setScroller(WidgetAxis dim, JScrollBar adj);

  /**
   * Modifies the way that scrolling is performed for an axis.
   *
   * @param dim       identifies which axis is being changed.
   * @param behavior  the desired behavior
   *
   * @see #getScrollIncrementBehavior
   */
  public void setScrollIncrementBehavior(WidgetAxis widgetAxis, ScrollIncrementBehavior behavior);

  /**
   * Use this to decide whether or not the scrolling increment
   * is being automatically readjusted.
   *
   * @param dim identifies which axis is being queried.
   *
   * @return a value indicating the scroll behavior.
   *
   * @see #setScrollIncrementBehavior
   */
  public ScrollIncrementBehavior getScrollIncrementBehavior(WidgetAxis widgetAxis);

  /**
   * Scrolls this widget along the specified axis.
   *
   * @param dim    indentifies which axis to scroll.
   * @param value  the distance in coordinate space to scroll.
   */
  public void scroll(WidgetAxis widgetAxis, double value);

  /**
   * Zoom this widget.
   * @param dim  indicates which axis dimension to zoom.
   * @param zoom_scale the double indicating the number of pixels
   *   per coordinate
   */
  public void zoom(WidgetAxis widgetAxis, double zoom_scale);

  /**
   * Sets the maximum allowable <code>zoom_scale</code> for this widget.
   * For example, if at
   * the highest resolution, you wish to display individual bases of
   * a sequence, then set <code>max</code> to the width of a character
   * of the desired font.
   *
   * @param dim indicates which axis to apply this constraint.
   * @param max  the double describing the maximum pixels per coordinate;
   *   should generally be the maximum size (in pixels)
   *   of a visual item.
   *
   * @see #zoom
   * @see #getMaxZoom
   */
  public void setMaxZoom(WidgetAxis widgetAxis, double max);

  /*
   * Sets the minimum allowable <code>zoom_scale</code> for this widget.
   * For example, if at lowest resolution, you wish to ensure that at
   * least one pixel is displayed per base and the coordinate system
   * is set such that each base corresponds to a unit, then set
   * <code>min</code> to 1.
   *
   * @param id  indicates which axis to apply this constraint.   *
   * @param min  the double describing the minimum pixels per coordinate;
   *   should generally be the minimum size (in pixels)
   *   of a visual item.
   *
   * @see #zoom
   * @see #getMinZoom
   */
  public void setMinZoom(WidgetAxis widgetAxis, double min);

  /**
   * Returns the currently set maximum <code>zoom_scale</code>.
   *
   * @return the maximum number of pixels per coordinate.
   * @see #setMaxZoom
   */
  public double getMaxZoom(WidgetAxis widgetAxis);

  /**
   * Returns the currently set minimum <code>zoom_scale</code>.
   *
   * @return the minimum number of pixels per coordinate.
   * @see #setMinZoom
   */
  public double getMinZoom(WidgetAxis widgetAxis);

  /**
   * Returns a list of all <code>Glyph</code>s at
   * coordinate point <code>x,y</code> in this widget.
   *
   * @param x the double describing the X coordinate
   * @param y the double describing the Y coordinate
   *
   * @return a List of {@link GlyphI}s
   * at the point
   */
  public List<GlyphI> getItems(double x, double y);

  /**
   * Adds <code>glyph</code> to the list of selected glyphs for this
   * widget.  Selected glyphs will be displayed differently than
   * unselected glyphs, based on selection style
   *
   * @param glyph a <code>GlyphI</code> to select
   * @see #deselect
   * @see #getSelected
   */
  //TODO: delete this method
  public void select(GlyphI glyph);

  /**
   * Adds all glyphs in List <code>glyphs</code> to the list of
   * selected glyphs for this widget.  Selected glyphs will be displayed
   * differently than unselected glyphs, based on selection style
   *
   * @param glyphs a List of <code>GlyphIs</code> to select
   * @see #deselect
   * @see #getSelected
   */
  //TODO: delete this method
  public void select(List<GlyphI> glyphs);

  /**
   * Removes <code>glyph</code> from the list of selected glyphs for this widget.
   * Visually unselects glyph.
   *
   * @see #select
   * @see #getSelected
   */
  //TODO: delete this method
  public void deselect(GlyphI glyph);

  /**
   * Removes all glyphs in List <code>glyphs</code> from the list of selected
   * glyphs for this widget.  Visually unselects glyph.
   *
   * @see #select
   * @see #getSelected
   */
  //TODO: delete this method
  public void deselect(List<GlyphI> glyphs);

  /**
   * Retrieves all currently selected glyphs.
   *
   * @return an Iterable of all selected glyphs
   */
  public Iterable<GlyphI> getSelected();

//  /**
//   * If this widget contains other widgets, returns the internal widget
//   *    at the given location.
//   *
//   * @param location where to find the component widget.
//   * @return the component widget.
//   */
//  public NeoWidgetI getWidget(int location);

//  /**
//   * If this widget contains other widgets, returns the internal widget
//   *    that contains the given GlyphI.
//   *
//   * @param gl the glyph to search for
//   * @return the component widget.
//   */
//  public NeoWidgetI getWidget(GlyphI gl);

  /**
   * Updates the visual appearance of the widget.
   * It is important to call this method
   * to view any externally introduced changes
   * in widget appearance
   * since the last call to updateWidget().
   */
  public void updateWidget();

  /**
   * Updates the visual appearance of the widget.
   * This form allows you to force complete redrawing of the entire widget.
   * <p>
   * An implementation can use this as an interim measure
   * or in place of smooth internal optimizations.
   * Generally,
   * updateWidget() with no arguments should have the same effect
   * as updateWidget(true),
   * but may be more efficient.
   * updateWidget(false) should be equivalent to updateWidget().
   *
   * @param full_update indicates wether or not the entire widget
   *                    should be redrawn.
   */
  public void updateWidget(boolean full_update);

  /**
   * Associates an arbitrary datamodel object with a glyph.  Can be retrieved using
   * <a href="#getDataModel">getDataModel</a>.  More than one <code>glyph</code>
   * may be associated with one <code>datamodel</code>, but only one
   * <code>datamodel</code> can be associated with a <code>glyph</code>.
   *
   * @param glyph       a GlyphI on the NeoWidgetI
   * @param datamodel an arbitrary  Object
   * @see #getDataModel
   */
  public void setDataModel(GlyphI glyph, Object datamodel);

  /**
   * Retrieve the datamodel associated with the glyph.  This facilitates
   * efficient event handling by associating application-specific data to
   * the visual glyphs.
   *
   * @param glyph a GlyphI on the widget
   * @return the datamodel associated with <code>GlyphI</code>.
   *
   */
  public Object getDataModel(GlyphI glyph);

  /**
   *  Returns true if any datamodels are represented by multiple glyphs.
   *  WARNING: once one model is represented by multiple glyphs, this flag might only 
   *     be reset to false when clearWidget() is called
  */
  public boolean hasMultiGlyphsPerModel();

  /**
   * Returns the first GlyphI found in the NeoWidgetI that is associated with
   * the <code>datamodel</code>.  Typically, <code>datamodel</code> is
   * an arbitrary datamodel that has been associated with one or more glyphs.
   * If you know there is only one GlyphI associated with each datamodel, this
   * method is more efficient than calling getItems(datamodel), which returns
   * a List.
   *
   * @param datamodel an arbitrary object associated with one or
   *   more glyphs.
   * @return the first GlyphI found to be associated with the datamodel
   */
  public GlyphI getItem(Object datamodel);

  /**
   * Retrieves the List of glyphs associated with the
   * <code>datamodel</code>.  Typically, <code>datamodel</code> is
   * an arbitrary datamodel that has been associated with one or more glyphs.
   *
   * @param datamodel an arbitrary object associated with one or
   *   more glyphs.
   * @return the List of glyphs associated with <code>
   *  datamodel</code>.
   */
  public List<GlyphI> getItems(Object datamodel);

  /**
   * constrains zooming along the given axis to the given contraint.
   * You can focus horizontal zooming at the left edge, center or right edge.
   * You can focus vertical zooming at the top, center, or bottom.
   *
   * @param axisid the axis to constrain.
   * @param constraint the type desired.
   */
  public void setZoomBehavior(WidgetAxis axis, ZoomConstraint constraint);

  /**
   * Constrains zooming along the given axis to the given point.
   * This form of the setZoomBehavior method is used to constrain
   * (or focus) zooming to a particular coordinate
   * rather than {@link NeoWidgetI.ZoomConstraint#CONSTRAIN_START}, {@link NeoWidgetI.ZoomConstraint#CONSTRAIN_MIDDLE}, or {@link NeoWidgetI.ZoomConstraint#CONSTRAIN_END}.
   *
   * @param axisid the axis to constrain.
   * @param constraint the type desired.
   *        The only valid value is {@link ZoomConstraint#CONSTRAIN_COORD}.
   * @param coord the coordinate at which to focus zooming.
   *
   */
  //TODO: should the other zoom constraints be supported also, or should we get rid of this setting
  public void setZoomBehavior(WidgetAxis axis, ZoomConstraint constraint, double coord);

  /**
   * Controls the scale values allowed during zooming.
   *
   * Scale constraints are currently only considered during
   *    zooming with zoomer[] adjustables
   *
   * @param axis the axis
   * @param constraint the constraint
   */
  public void setScaleConstraint(WidgetAxis axis, ScaleConstraint constraint);


  /**
   * Turns rubber banding on and off.
   *
   * @param activate the boolean indicator.  If true, then rubber
   *   banding is activated.
   */
  public void setRubberBandBehavior(boolean activate);

  /**
   * Specifies the manner in which selected items are visually displayed.
   *
   * @param behavior how selected Glyphs are visually differentiated
   * from unselected Glyphs.
   */
  public void setSelectionAppearance(SceneI.SelectType behavior);

  /**
   * Specifies the color in which selected items are visually displayed.
   *
   * @param color the color specification to use for selection.
   */
  public void setSelectionColor(Color color);

  /**
   * Returns the appearance set by setSelectionAppearance.
   */
  public SceneI.SelectType getSelectionAppearance();

  /**
   * Returns the Color set by setSelectionColor.
   *
   * @see #setSelectionColor
   */
  public Color getSelectionColor();

//  /**
//   * Determines whether or not subselection of glyphs is allowed.
//   *
//   * @param allowed <code>true</code> indicates that subselections
//   *   of glyphs are allowed.
//   */
//  public void setSubSelectionAllowed(boolean allowed);
//
//  /**
//   * Returns the current setting for subselection.
//   *
//   * @return <code>true</code> if subselection is currently allowed.
//   * @see #setSubSelectionAllowed
//   */
//  public boolean isSubSelectionAllowed();

  /**
   * Returns the bounding rectangle of the glyph in coordinates.
   */
  public Rectangle2D.Double getCoordBounds(GlyphI glyph);

  /**
   * Sets the pointing precision of the mouse.
   *
   * @param blur the number of pixels from the edge of glyphs.
   * When the mouse is clicked this close to the glyph
   * the glyph is selected.
   */
  public void setPixelFuzziness(int blur);

  /**
   * Gets the pointing precision of the mouse.
   *
   * @return the number of pixels around glyph bounds
   * considered to be "within" the glyph.
   * @see #setPixelFuzziness
   */
  public int getPixelFuzziness();

  /**
   * Adds the specified mouse listener to receive notification of
   *   events on this NeoWidget.
   * @param l the mouse listener
   * @see #removeMouseListener
   */
  //TODO: remove? replace with NeoMouseListener?
  public void addMouseListener(MouseListener l);

  /**
   * Adds the specified mouse motion listener to receive notification of
   *   events on this NeoWidget.
   * @param l the mouse motion listener
   * @see #removeMouseMotionListener
   */
  public void addMouseMotionListener(MouseMotionListener l);

  /**
   * Adds the specified key listener to receive notification of
   *   events on this NeoWidget.
   * @param l the key listener
   * @see #removeKeyListener
   */
  public void addKeyListener(KeyListener l);

  /**
   * Removes the specified mouse listener so it no longer
   *   receives notification of mouse events on this widget
   * @param l the mouse listener
   * @see #addMouseListener
   */
  //TODO: remove? replace with NeoMouseListener?
  public void removeMouseListener(MouseListener l);

  /**
   * Removes the specified mouse motion listener so it no longer
   *   receives notification of mouse motion events on this widget
   * @param l the mouse motion listener
   * @see #addMouseMotionListener
   */
  public void removeMouseMotionListener(MouseMotionListener l);

  /**
   * Removes the specified key listener so it no longer
   *   receives notification of key events on this widget.
   * @param l the key listener
   * @see #addKeyListener
   */
  //TODO: remove?
  public void removeKeyListener(KeyListener l);

  /**
   * Removes the glyph from this widget.
   *
   * @param glyph the GlyphI to remove
   * @see NeoMapI#addItem
   */
  public void removeItem(GlyphI glyph);

  /**
   * Removes all GlyphI's in List from this widget.
   *
   * @param glyphs the List of GlyphIs to remove
   * @see NeoMapI#addItem
   */
  public void removeItem(List<GlyphI> glyphs);

  /**
   *  Clears all glyphs from the widget
   */
  public void clearWidget();

  /**
   * Make this glyph be drawn before all its siblings
   * (a more drastic method is toFront(),
   * which is only implemented for NeoMap)
   */
  public void toFrontOfSiblings(GlyphI glyph);

  /**
   *  Make this glyph be drawn behind all its siblings
   *  (a more drastic method is toBack(), which is only implemented for NeoMap)
   */
  public void toBackOfSiblings(GlyphI glyph);

  /**
   * Determines automatic seelction behavior.
   * If <code>theEvent == ON_MOUSE_UP</code>
   * then automatic selection occurs on the mouse up event.  Similarly,
   * if <code>theEvent == ON_MOUSE_DOWN</code> then a automatic selection
   * occurs on the mouse down event.  Automatic selection is disabled for the
   * NeoWidget if <code>theEvent == NO_SELECTION</code>.
   *
   * @param theEvent
   *        all NeoWidgets support NO_SELECTION, ON_MOUSE_DOWN, or ON_MOUSE_UP
   *        some widgets support additional options
   */
  public void setSelectionEvent(SelectionType theEvent);


  /**
   * Gets the selection method for automatic selection in the NeoWidget.
   */
  public SelectionType getSelectionEvent();

  /**
   * Adds a widget listener.
   */
  public void addWidgetListener(NeoWidgetListener l);

  /** Removes a listener added by {@link #addWidgetListener}. */
  public void removeWidgetListener(NeoWidgetListener l);
}
