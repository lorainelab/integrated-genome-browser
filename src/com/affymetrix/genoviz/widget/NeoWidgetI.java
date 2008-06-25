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

import com.affymetrix.genoviz.bioviews.TransformI;
import com.affymetrix.genoviz.event.NeoWidgetListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

/**
 * This interface represents the functionality that is common to all widgets.
 * <p>
 * All widgets include notions of axis, establishing coordinate
 * space, indicating bounds, panning, zooming, selecting and placing
 * items at positions, glyph factories (for creating graphical
 * objects with common attributes), data adapters (for automating the
 * creation of graphical objects from data objects), dealing with
 * color, establishing window resize behavior, and managing event handlers.
 * <p><font size="-1">
 * Note: this interface does <em>not</em> extend Component,
 * However, the current implementations of NeoWidgetI <em>do</em> all extend
 * Component.
 * For more information on implementation-specific characteristics,
 * see the javadocs for the implementation class
 * corresponding to each interface.
 * </font></p>
 */
public interface NeoWidgetI {

  static final int X = TransformI.Dimension.X.ordinal();
  static final int Y = TransformI.Dimension.Y.ordinal();

  /**
   * Type of selection done by the NeoWidget.
   * @see #setSelectionEvent
   */
  public enum SelectionType {
  /**
   * No selection is done by the NeoWidget.
   * A listener or superclass will still get all events and can react to them.
   */

    NO_SELECTION,
    /**
   * Selected on Event.MOUSE_DOWN.
   * A listener or superclass will still get all events and can react to them.
   */
    ON_MOUSE_DOWN,
    /**
   * Selected on Event.MOUSE_UP.
   * A listener or superclass will still get all events and can react to them.
   */
    ON_MOUSE_UP;
  }

  /**
   * Indicates where zooming should be focused
   * in the widget.
   *
   * @see #setZoomBehavior
   */
  public enum ZoomConstraint {

    CONSTRAIN_START,
    CONSTRAIN_MIDDLE,
    CONSTRAIN_END,
    /**
     * Indicates that zooming should be focused
     * at a specified fixed coordinate.
     */
    CONSTRAIN_COORD
  }

  /**
   * indicates that the widget should be resized
   * to fit within a container
   * whenever that container is resized.
   */
  public static final int FITWIDGET = 5;

  /**
   * @see #INTEGRAL_COORDS
   * @see #setScaleConstraint
   * @see #zoom
   * @see NeoWidget#setScaleConstraint
   */
  public enum ScaleConstraint {

    /**
   * constrains high resolution zooming
   * to integral values of pixels per coordinate.
   * If the number of pixels per coordinate
   * (<code>zoom_scale</code>) is greater than one,
   * then <code>zoom_scale</code> is rounded to the nearest integer.
   * <p>
   * In NeoWidget,
   * this is implemented only for zooming triggered by zoomer adjustables.
   * Calling <a href="#zoom"><code>zoom()</code></a> directly has no effect.
   *
   */
    INTEGRAL_PIXELS,
    /**
   * Constrains low resolution zooming
   * to integral values of coordinates per pixel.
   * If the number of pixels per coordinate
   * (<code>zoom_scale</code>) is less than one, then <code>zoom_scale</code>
   * is modified such that <code>1/zoom_scale</code> (coords per pixel)
   * is rounded to the nearest integer.
   * <p>
   * In NeoWidget,
   * this is implemented only for zooming triggered by zoomer adjustables.
   * Calling <a href="#zoom"><code>zoom()</code></a> directly has no effect.
   *
   */
    INTEGRAL_COORDS,
    /**
   * Constrain zooming to both
   * INTEGRAL_PIXELS and INTEGRAL_COORDS.
   */
    INTEGRAL_ALL,
    NONE
  }

  /**
   * Indicates that a widget's bounds should be automatically expanded
   * when an item is added beyond the widget's previous bounds.
   *
   * @see #NO_EXPAND
   * @see #setExpansionBehavior
   */
  public static final int EXPAND = 200;

  /**
   * indicates that a widget's bounds
   * should <em>not</em> be automatically expanded
   * when an item is added beyond the widget's previous bounds.
   *
   * @see #EXPAND
   * @see #setExpansionBehavior
   */
  public static final int NO_EXPAND = 201;

  // *******************************************************

  /**
   * Sets the background Color property.
   * Just like a java.awt.Component.
   *
   * @param theColor <code>Color</code> to set the background.
   */
  public void setBackground(Color theColor);

  /**
   * returns the current setting of the background Color property.
   * Just like a java.awt.Component.
   *
   * @return the background <code>Color</code>
   */
  public Color getBackground();

  /**
   * Sets the foreground Color property.
   * Just like a java.awt.Component.
   *
   * @param theColor <code>Color</code> to set the foreground.
   *
   */
  public void setForeground(Color theColor);

  /**
   * Returns the current setting of the foreground Color property.
   * Just like a java.awt.Component.
   *
   * @return the foreground <code>Color</code>
   */
  public Color getForeground();

  /**
   * Adjusts this widget such that the <code>View</code>, scrollbars, etc., fit
   * within the current bounds of the widget.
   *
   * @param xstretch the boolean determines whether stretchToFit
   *   is applied along the X axis.
   * @param ystretch the boolean determines whether stretchToFit
   *   is applied along the Y axis.
   */
  public void stretchToFit(boolean xstretch, boolean ystretch);


  /**
   * Associates a slider
   * to control zooming along the specified axis.
   *
   * @param id identifies the axis of zooming.
   *           Should be ({@link NeoWidget#X} or {@link NeoWidget#Y}).
   * @param slider a slider to be associated with the axis.
   */
  public void setZoomer(TransformI.Dimension dim, JSlider slider);


  /**
   * To be called when the object is no longer needed. Eliminate some references, as is necessary
   * for garbage collection to occur.
   */
  public void destroy();

  /**
   * associates an adjustable component
   * to control scrolling along the specified axis.
   *
   * @param id identifies the axis of scrolling.
   *           Should be {@link #X} or {@link #Y}.
   * @param adj an scrollbar
   *            to be associated with the axis.
   */
  public void setScroller(TransformI.Dimension dim, JScrollBar adj);

  /**
   * indicates that the coordinate scrolling increment should be
   * automatically adjusted upon zooming
   * so that the pixels scrolled remains constant.
   *
   * @see #setScrollIncrementBehavior
   */
  public static final int AUTO_SCROLL_INCREMENT = 0;

  /**
   * indicates that the coordinate scrolling increment should <em>not</em> be
   * automatically adjusted upon zooming.
   *
   * @see #AUTO_SCROLL_INCREMENT
   * @see #setScrollIncrementBehavior
   */
  public static final int NO_AUTO_SCROLL_INCREMENT = 1;
  public static final int AUTO_SCROLL_HALF_PAGE = 2;

  /**
   * Modifies the way that scrolling is performed for an axis.
   *
   * @param id       identifies which axis (X or Y) is being queried.
   * @param behavior AUTO_SCROLL_INCREMENT or NO_AUTO_SCROLL_INCREMENT
   *
   * @see #getScrollIncrementBehavior
   */
  public void setScrollIncrementBehavior(TransformI.Dimension dim, int behavior);

  /**
   * Use this to decide whether or not the scrolling increment
   * is being automatically readjusted.
   *
   * @param id identifies which axis (X or Y) is being queried.
   *
   * @return a constant indicating the scroll behavior.  Valid values
   *  are NeoWidgetI.AUTO_SCROLL_INCREMENT and
   *  NeoWidgetI.NO_AUTO_SCROLL_INCREMENT
   *
   * @see #setScrollIncrementBehavior
   */
  public int getScrollIncrementBehavior(TransformI.Dimension dim);

  /**
   * scrolls this widget along the specified axis.
   *
   * @param id    indentifies which axis to scroll.
   *     valid values are {@link NeoWidget#X} or {@link NeoWidget#Y}.
   * @param value  the double distance in coordinate space
   *               to scroll.
   */
  public void scroll(TransformI.Dimension dim, double value);

  /**
   * zoom this widget to a scale of <code>zoom_scale</code> along the
   * <code>id</code>-th axis.
   *
   * @param dim  indicates which axis dimension to zoom.
   *
   * @param zoom_scale the double indicating the number of pixels
   *   per coordinate
   */
  public void zoom(TransformI.Dimension dim, double zoom_scale);

  /**
   * sets the maximum allowable <code>zoom_scale</code> for this widget.
   * For example, if at
   * the highest resolution, you wish to display individual bases of
   * a sequence, then set <code>max</code> to the width of a character
   * of the desired font.
   *
   * @param axisid indicates which axis to apply this constraint.
   *   valid values are ({@link NeoWidget#X} or {@link NeoWidget#Y}).
   *
   * @param max  the double describing the maximum pixels per coordinate;
   *   should generally be the maximum size (in pixels)
   *   of a visual item.
   *
   * @see #zoom
   * @see #getMaxZoom
   */
  public void setMaxZoom(TransformI.Dimension dim, double max);

  /*
   * sets the minimum allowable <code>zoom_scale</code> for this widget.
   * For example, if at lowest resolution, you wish to ensure that at
   * least one pixel is displayed per base and the coordinate system
   * is set such that each base corresponds to a unit, then set
   * <code>min</code> to 1.
   *
   * @param id  indicates which axis to apply this constraint.
   *   valid values are NeoWidgetI.X or NeoWidgetI.Y.
   *
   * @param min  the double describing the minimum pixels per coordinate;
   *   should generally be the minimum size (in pixels)
   *   of a visual item.
   *
   * @see #zoom
   * @see #getMinZoom
   */
  public void setMinZoom(TransformI.Dimension dim, double min);

  /**
   * returns the currently set maximum <code>zoom_scale</code>.
   *
   * @return the maximum number of pixels per coordinate.
   * @see #setMaxZoom
   */
  public double getMaxZoom(TransformI.Dimension dim);

  /**
   * returns the currently set minimum <code>zoom_scale</code>.
   *
   * @return the minimum number of pixels per coordinate.
   * @see #setMinZoom
   */
  public double getMinZoom(TransformI.Dimension dim);

  /**
   * Returns a list of all <code>Glyph</code>s at
   *  <code>x,y</code> in this widget.
   *
   * @param x the double describing the X position
   * @param y the double describing the Y position
   *
   * @return a <code>List</code> of <code>Glyph</code>s
   * at <code>x,y</code>
   */
  public List<GlyphI> getItems(double x, double y, int location);

  /**
   * adds <code>glyph</code> to the list of selected glyphs for this
   * widget.  Selected glyphs will be displayed differently than
   * unselected glyphs, based on selection style
   *
   * @param glyph a <code>GlyphI</code> to select
   * @see #deselect
   * @see #getSelected
   */
  public void select(GlyphI glyph);

  /**
   * adds all glyphs in List <code>glyphs</code> to the list of
   * selected glyphs for this widget.  Selected glyphs will be displayed
   * differently than unselected glyphs, based on selection style
   *
   * @param glyphs a List of <code>GlyphIs</code> to select
   * @see #deselect
   * @see #getSelected
   */
  public void select(List<GlyphI> glyphs);

  /**
   * Removes <code>glyph</code> from the list of selected glyphs for this widget.
   * Visually unselects glyph.
   *
   * @see #select
   * @see #getSelected
   */
  public void deselect(GlyphI glyph);

  /**
   * Removes all glyphs in List <code>glyphs</code> from the list of selected
   * glyphs for this widget.  Visually unselects glyph.
   *
   * @see #select
   * @see #getSelected
   */
  public void deselect(List<GlyphI> glyphs);

  /**
   * retrieves all currently selected glyphs.
   *
   * @return a List of all selected GlyphIs
   * @see #deselect
   * @see #select
   */
  public List<GlyphI> getSelected();

  /**
   * If this widget contains other widgets, returns the internal widget
   *    at the given location.
   *
   * @param location where to find the component widget.
   * @return the component widget.
   */
  public NeoWidgetI getWidget(int location);

  /**
   * If this widget contains other widgets, returns the internal widget
   *    that contains the given GlyphI.
   *
   * @param gl the glyph to search for
   * @return the component widget.
   */
  public NeoWidgetI getWidget(GlyphI gl);

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
   * sets the visibility of <code>item</code> for this widget.
   *
   * @param glyph the GlyphI to modify visibility of.
   * @param visible a boolean indicator of visibility.  if false,
   *   then the GlyphI is not displayed.
   */
  public void setVisibility(GlyphI glyph, boolean visible);

  /**
   * sets the visibility of all glyph's in list for this widget.
   *
   * @param glyphs List of GlyphI's to modify visibility;
   * @param visible a boolean indicator of visibility.  if false,
   *   then the GlyphI is not displayed.
   */
  public void setVisibility(List<GlyphI> glyphs, boolean visible);

  /**
   * gets the visibility of an item in this widget.
   *
   * @param glyph the GlyphI whose visibility is queried
   */
  public boolean getVisibility(GlyphI glyph);

  /**
   * creates a named color
   * and adds it to the widget's collection
   * of named colors.
   *
   * @param name a unique identifier for the color.
   * @param col  the <code>Color</code> to be associated with
   *   <code>name</code>.
   */
  public void addColor(String name, Color col);

  /**
   * retrieves a named color.
   *
   * @param name the <code>String</code> label for a <code>Color</code>.
   * @return the <code>Color</code> corresponding to <code>name</code>.
   * @see #addColor
   */
  public Color getColor(String name);

  /**
   * retrieves a color's name.
   *
   * @param theColor a <code>Color</code> to look for.
   * @return a <code>String</code> label associated with a color.
   * @see #addColor
   */
  public String getColorName(Color theColor);

  /**
   * enumerates all the color names.
   *
   * @return an <code>Enumeration</code> of all color name <code>String</code>s
   *   set by <code>addColor</code>
   * @see #addColor
   */
  public Enumeration getColorNames();

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
   * update the position of <code>glyph</code> by <code>diffx</code>
   * and <code>diffy</code> in the X and Y axes respectively,
   * relative to the current position of <code>glyph</code>, where
   * the current position of <code>glyph</code> is the coordinate of the
   * top left coordinate of <code>glyph</code>'s bounding box.
   * Offsets are specified in coordinate space (not pixels).
   *
   * @param glyph the GlyphI to move
   * @param diffx the double relative offset along the X axis
   * @param diffy the double relative offset along the Y axis
   * @see #moveAbsolute
   * @see NeoMapI#addItem
   */
  public void moveRelative(GlyphI glyph, double diffx, double diffy);

  /**
   * update the position of all <code>glyphs</code> in List by
   * <code>diffx</code> and <code>diffy</code> in the X and Y axes respectively,
   * relative to the current position of <code>glyphs</code>, where
   * the current position of a <code>glyph</code> is the coordinate of the
   * top left corner of the <code>glyph</code>'s bounding box.
   * Offsets are specified in coordinate space (not pixels).
   *
   * @param glyphs the List of GlyphIs to move
   * @param diffx the double relative offset along the X axis
   * @param diffy the double relative offset along the Y axis
   * @see #moveAbsolute
   * @see NeoMapI#addItem
   */
  public void moveRelative(List<GlyphI> glyphs, double diffx, double diffy);

  /**
   * modifies the position of <code>glyph</code> to be the
   * new absolute position (<code>x,y</code>) specified in
   * coordinate space (not pixels).
   *
   * @param glyph the GlyphI to move
   * @param x the absolute double position along the X axis.
   * @param y the absolute double position along the Y axis.
   * @see #moveRelative
   * @see NeoMapI#addItem
   */
  public void moveAbsolute(GlyphI glyph, double x, double y);

  /**
   * Modifies the position of all <code>glyphs</code>  in List to be the
   * new absolute position (<code>x,y</code>) specified in
   * coordinate space (not pixels).
   * @param glyphs the List of GlyphIs to move
   * @param x the absolute double position along the X axis.
   * @param y the absolute double position along the Y axis.
   * @see #moveRelative
   * @see NeoMapI#addItem
   */
  public void moveAbsolute(List<GlyphI> glyphs, double x, double y);


  /**
   * Determines widget behavior along each axis if items are added beyond
   * current bounds of the widget.  Valid values are EXPAND, in which case the
   * widget's bounds are extended to encompass the new item's location, or
   * NO_EXPAND, in which case the widget refuses to expand to encompass the
   * new item
   *
   * @param axisid the axis ({@link #X} or {@link #Y}) to apply
   *   the constraint.
   * @param behavior the type of constraint to apply.  Valid
   *  values are EXPAND and NO_EXPAND
   *
   * @see #EXPAND
   * @see #NO_EXPAND
   */
  public void setExpansionBehavior(int axisid, int behavior);

  /**
   * Gets the behvior set by setExpansionBehavior.
   *
   * @param axisid the axis (NeoWidgetI.X or NeoWidgetI.Y) whose expansion
   *                   behvior is to be retrieved
   *
   * @see #setExpansionBehavior
   */
  public int getExpansionBehavior(int axisid);

  /**
   * constrains zooming along the given axis to the given contraint.
   * You can focus horizontal zooming at the left edge, center or right edge.
   * You can focus vertical zooming at the top, center, or bottom.
   *
   * @param axisid the axis ({@link NeoWidget#X} or {@link NeoWidget#Y}) to constrain.
   * @param constraint the type desired.
   *
   * @see NeoWidget#X
   * @see NeoWidget#Y
   */
  public void setZoomBehavior(int axisid, ZoomConstraint constraint);

  /**
   * Constrains zooming along the given axis to the given point.
   * This form of the setZoomBehavior method is used to constrain
   * (or focus) zooming to a particular coordinate
   * rather than {@link NeoWidgetI.ZoomConstraint#CONSTRAIN_START}, {@link NeoWidgetI.ZoomConstraint#CONSTRAIN_MIDDLE}, or {@link NeoWidgetI.ZoomConstraint#CONSTRAIN_END}.
   *
   * @param axisid the axis ({@link NeoWidget#X} or {@link NeoWidget#Y}) to constrain.
   * @param constraint the type desired.
   *        The only valid value is {@link NeoWidgetI.ZoomConstraint#CONSTRAIN_COORD}.
   * @param coord the coordinate at which to focus zooming.
   *
   */
  public void setZoomBehavior(int axisid, ZoomConstraint constraint, double coord);

  /**
   * Controls the scale values allowed during zooming.
   *
   * Scale constraints are currently only considered during
   *    zooming with zoomer[] adjustables
   *
   * @param axisid     {@link #X} or {@link #Y}
   * @param constraint {@link ScaleConstraint#INTEGRAL_PIXELS}, {@link ScaleConstraint#INTEGRAL_COORDS}, or {@link ScaleConstraint#INTEGRAL_ALL}.
   *
   */
  public void setScaleConstraint(int axisid, ScaleConstraint constraint);


  /**
   * turns rubber banding on and off.
   * Configuration for rubberbanding
   *  currently options are only to turn rubber banding on or off,
   *  but anticipate having a longer signature for color, event mapping, etc.
   *
   * @param activate the boolean indicator.  if true, then rubber
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
   * specifies the color in which selected items are visually displayed.
   *
   * @param color the color specification to use for selection.
   */
  public void setSelectionColor(Color color);

  /**
   * Returns the appearance set by setSelectionAppearance.
   */
  public SceneI.SelectType getSelectionAppearance();

  /**
   * returns the Color set by setSelectionColor.
   *
   * @see #setSelectionColor
   */
  public Color getSelectionColor();

  /**
   * determines whether or not subselection of glyphs is allowed.
   *
   * @param allowed <code>true</code> indicates that subselections
   *   of glyphs are allowed.
   */
  public void setSubSelectionAllowed(boolean allowed);

  /**
   * returns the current setting for subselection.
   *
   * @return <code>true</code> if subselection is currently allowed.
   * @see #setSubSelectionAllowed
   */
  public boolean isSubSelectionAllowed();

  /**
   * returns the bounding rectangle of the glyph in coordinates
   */
  public java.awt.geom.Rectangle2D.Double getCoordBounds(GlyphI glyph);

  /**
   * sets the pointing precision of the mouse.
   *
   * @param blur the number of pixels from the edge of glyphs.
   * When the mouse is clicked this close to the glyph
   * the glyph is selected.
   */
  public void setPixelFuzziness(int blur);

  /**
   * gets the pointing precision of the mouse.
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
   *   receives notification of key events on this widget
   * @param l the key listener
   * @see #addKeyListener
   */
  public void removeKeyListener(KeyListener l);

  /**
   * removes the <code>glyph</code> from this widget
   *
   * @param glyph the GlyphI to remove
   * @see NeoMapI#addItem
   */
  public void removeItem(GlyphI glyph);

  /**
   * Removes all GlyphI's in List from this widget
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
   * sets the background color for a component widget
   * within this widget.
   *
   * @param id identifies the component widget to color.
   * @param col is the color to assign to the background.
   */
  public void setBackground(int id, Color col);

  /**
   * gets the background color for a component widget
   * within this widget.
   *
   * @param id identifies which component widget.
   * @return the color assigned to the background.
   */
  public Color getBackground(int id);

  /**
   *  returns true if the glyph supports selection of a subregion
   *  in addition to selection of the whole item
   */
  public boolean supportsSubSelection(GlyphI glyph);


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
  public void setSelectionEvent(NeoWidgetI.SelectionType theEvent);


  /**
   * Gets the selection method for automatic selection in the NeoWidget.
   */
  public NeoWidgetI.SelectionType getSelectionEvent();

  /**
   * Adds a widget listener.
   */
  public void addWidgetListener(NeoWidgetListener l);

  /** Removes a listener added by {@link #addWidgetListener}. */
  public void removeWidgetListener(NeoWidgetListener l);

}
