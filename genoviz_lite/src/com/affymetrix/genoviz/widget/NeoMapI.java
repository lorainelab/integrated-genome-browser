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

import java.awt.*;
import java.util.*;
import java.util.List;
import com.affymetrix.genoviz.bioviews.TransformI;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.bioviews.RubberBand;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import javax.swing.JSlider;

/**
 * This interface provides general purpose controls for a map.
 * Included are
 * configuring and adding glyphs,
 * creating zoom controls,
 * associating a data model,
 * and selecting glyphs in a map.
 *
 * <p> NeoMapI represents the abstract application programmer interface
 * for a map.  An implementation of a map, such as NeoMap, is used
 * to instantiate a map, and these implementations can include additional
 * functionality.
 *
 * <p> Example:
 *
 * <pre>
 * NeoMapI map = new NeoMap();
 *
 * // add zoom adjustable (in this case NeoScrollbar)
 * map.setRangeZoomer(new NeoScrollbar(NeoScrollbar.HORIZONTAL));
 *
 * map.setMapRange(-500,500); // X bounds
 * map.setMapOffset(-20,20);  // Y bounds
 *
 * // ensure that initial display shows the complete X and Y bounds
 * map.stretchToFit(true,true);
 *
 * // add an axis on the center of this map
 * map.addAxis(0);
 *
 * // map configuration options. (implementation specific parameters)
 * map.configure("-name1 val1 -name2 val2 ...etc..");
 *
 * // instantiate new factory.  (implementation specific parameters)
 * Object fac = map.addFactory("-name1 val1 -name2 -val2");
 *
 * // add items using a factory
 * item1 = map.addItem(fac,-20,-10);
 * item2 = map.addItem(fac,150,400);
 * ...etc..
 *
 * // associate a private datum to an item
 * setDataModel(item1,myPrivData);
 * </pre>
 *
 * @author Gregg Helt
 *
 */
public interface NeoMapI extends NeoWidgetI {

  /**
   * For methods inherited from NeoWidgetI that require a sub-component id.
   * For NeoMapI the component <em>is</em> the only sub-component,
   * and its id is MAP.
   */
  public static final int MAP = 400;

  /**
   * sets the coordinates describing the range of the map.
   * The map's starting coordinate is set to <code>start</code> and
   * its final coordinate is set to <code>end-1</code>.
   * Affects the primary axis.
   *
   * @param start  the integer indicating the starting
   *   coordinate of the map
   * @param end  the integer indicating one coordinate
   *   beyond the final coordinate of the map.
   *
   * @see #setMapOffset
   */
  public void setMapRange(int start, int end);

  /**
   * sets the coordinates describing the offset of the map.
   * The map's starting coordinate is set to <code>start</code> and
   * its final coordinate is set to <code>end-1</code>.
   * Affects the secondary axis.
   *
   * @param start  the integer indicating the starting
   *   coordinate of the map
   * @param end  the integer indicating one coordinate
   *   beyond the final coordinate of the map.
   */
  public void setMapOffset(int start, int end);

  /**
   * Selects a range along a GlyphI.
   * This will default to normal selection
   * if the glyph does not support selection of internal areas and ranges.
   * Note that there is no deselect(glyph, start, end),
   * since only one selection per glyph is allowed,
   * either whole glyph or a contiguous range.  To
   * deselect a range selection, call {@link #deselect(GlyphI glyph)}.
   *
   * @param glyph the GlyphI to select a range along
   * @param start start of range to select
   * @param end end of range to select
   *
   * @see #deselect
   * @see #getSelected
   * @see #getSelectedStart
   * @see #getSelectedEnd
   */
  public void select(GlyphI glyph, int start, int end);

  /**
   * Selects a range along each GlyphI in a List.
   * This will default to normal selection
   * if the glyph does not support selection of internal areas and ranges.
   * Note that there is no deselect(glyphs, start, end),
   * since only one selection per glyph is allowed,
   * either whole glyph or a contiguous range.
   * To deselect a range selection for a List of glyphs,
   * call {@link #deselect(List glyphs)}.
   *
   * @param glyphs a List of GlyphIs to select a range along
   * @param start start of range to select
   * @param end end of range to select
   *
   * @see #getSelected
   * @see #getSelectedStart
   * @see #getSelectedEnd
   */
  public void select(List<GlyphI> glyphs, int start, int end);

  /**
   * Selecting an area within a glyph.
   * This will default to normal selection
   * if the glyph does not support selection of internal areas.
   * Note that there is no deselect(glyph, start, end, width, height),
   * since only one selection per glyph is allowed,
   * either whole glyph or a contiguous area.
   * To deselect an area selection, call {@link #deselect(GlyphI glyph)}.
   */
  public void select(GlyphI glyph, double x, double y, double width, double height);

  /**
   * Selecting an area for all GlyphIs in a List.
   * This will default to normal selection
   * if the glyph does not support selection of internal areas.
   * Note that there is no <code>deselect(glyphs, start, end, width, height)</code>,
   * since only one selection per glyph is allowed,
   * either whole glyph or a contiguous area.
   * To deselect an area selection for a List of glyphs,
   * call {@link #deselect(java.util.List)}.
   */
  public void select(List<GlyphI> glyphs, double x, double y, double width, double height);

  /**
   * Get the start of the selected range of a glyph.
   * If the glyph does not suport subselection but is still selected,
   * the start of the glyph will be returned.
   */
  public int getSelectedStart(GlyphI gl);

  /**
   * Get the end of the selected range of a glyph.
   * If the glyph does not suport subselection but is still selected,
   * the end of the glyph will be returned.
   */
  public int getSelectedEnd(GlyphI gl);

  /**
   * Adds an axis number line along the primary axis at <code>offset</code>
   * along the secondary axis.
   */
  public AxisGlyph addAxis(int offset);


  /**
   * Adds an adjustable to control zooming along the primary axis.
   *
   * @param adj an {@link Adjustable} to be associated with
   *  the primary axis, typically a scrollbar.
   * @see #setOffsetZoomer
   */
  public void setRangeZoomer(JSlider adj);

  /**
   * Adds an zoom adjustable to control zooming along the secondary axis.
   *
   * @param adj a {@link Adjustable} to be associated with
   *  the secondary axis, typically a scrollbar.
   * @see #setRangeZoomer
   */
  public void setOffsetZoomer(JSlider adj);

  /**
   * sets a rubber band for this map.
   * This allows a rubber band (or subclass thereof)
   * to be configured before setting this widget to use it.
   * Note that by default
   * widgets come with their own internal rubber band.
   * This method replaces that rubber band.
   *
   * @param theBand to use. <code>null</code> turns off rubber banding.
   */
  public void setRubberBand( RubberBand theBand );

  /**
   * adds a glyph object to this map to visually represent features.
   * The glyph's starting position is <code>start</code>
   * and ending position is <code>end</code>.
   * The actual class and type of glyph added is dependent
   * on the current map configuration.
   * A <code>GlyphI</code> is returned;
   * additional child glyphs may be added to it
   * using <code>addItem(parent, child)</code>.
   *
   * @param start the integer starting coordinate of the glyph
   * @param end  the integer ending coordinate of the glyph
   * @return the GlyphI added
   */
  public GlyphI addItem(int start, int end);

  /**
   * adds a glyph object to this map to visually represent features.
   * The glyph's starting position is <code>start</code>
   * and ending position is <code>end</code>.
   * The actual class and type of glyph added is dependent
   * on the current map configuration, and the options String.
   * A <code>GlyphI</code> is returned;
   * additional child glyphs may be added to it
   * using <code>addItem(parent, child)</code>.
   *
   * @param start the integer starting coordinate of the glyph
   * @param end  the interger ending coordinate of the glyph
   * @param options a <code>String</code> of options to specify
   *                    parameters of the glyph that is being created
   * @return the <code>GlyphI</code> added.
   */
  public GlyphI addItem(int start, int end, String options);


  /**
   * adds a glyph from <code>start</code> to <code>end</code>
   * along the map's primary axis,
   * using the specified glyph factory.
   *
   * @param factory the <code>MapGlyphFactory</code> to use for
   *   creating the glyph.
   * @param start the integer starting position
   * @param end  the integer ending position
   * @return the <code>GlyphI</code> added.
   * @see #addFactory
   */
  public GlyphI addItem(MapGlyphFactory factory, int start, int end);

  /**
   * adds a glyph from <code>start</code> to <code>end</code>
   * along the map's primary axis,
   * using the specified glyph factory,
   * combined with the options specified in the options String.
   *
   * @param factory the factory <code>Object</code> to use for
   *   creating the glyph.
   * @param start the integer starting position
   * @param end  the integer ending position
   * @param options a <code>String</code> of options augmenting
   *   the specified factory configuration.
   * @return the <code>GlyphI</code> added.
   * @see #addFactory
   */
  public GlyphI addItem(MapGlyphFactory factory, int start, int end,
                        String options);

  public void repack();

  /**
   * Convenience function for zooming the Range.
   *
   * @see #zoom 
   */
  public void zoomRange(double zoom_scale);

  /**
   * Convenience function for zooming the Offset.
   *
   * @see #zoom
   */
  public void zoomOffset(double zoom_scale);

  public void setMapColor(Color col);

  public Color getMapColor();


  /**
   * Sets the transform of the scrollbar specified by id
   * to the specified transform.
   *
   * @param id    the orientation ({@link NeoWidget#X} or {@link NeoWidget#Y}) of the scrollbar to
   *              receive the specified transform.
   * @param trans the Transform to be applied to the values of the
   *              scrollbar.
   */
  public void setScrollTransform(int id, TransformI trans);

  /**
   * sets the bounds for the given axis on the map.
   * @param id  the axis to bind, X or Y
   * @param start
   * @param end
   */
  public void setBounds(int id, int start, int end);

  /**
   * returns the bounding rectangle in pixels of the displayed item, tag.
   */
  public Rectangle getPixelBounds(GlyphI gl);

  /**
   * returns a list of all <code>Glyph</code>s at
   *  <code>x,y</code> in this widget.
   *
   * @param x the double describing the X position
   * @param y the double describing the Y position
   *
   * @return a List of <code>Glyph</code>s
   * at <code>x,y</code>
   */
  public List<GlyphI> getItems(double x, double y);

  /**
   * returns a list of all <code>Glyphs</code> within the
   * <code>pixrect</code> in this widget.
   *
   * @param pixrect the <code>Rectangle</code> describing the
   *   bounding box of interest
   *
   * @return a List of <code>Glyphs</code>
   *  in <code>pixrect</code>
   */
  public List<GlyphI> getItems(Rectangle pixrect);

  /**
   * retrieve a List of all drawn glyphs that overlap
   * the coordinate rectangle coordrect.
   */
  public List<GlyphI> getItemsByCoord(java.awt.geom.Rectangle2D.Double coordrect);

  /**
   * retrieve all drawn glyphs that overlap the pixel at point x, y.
   */
  public List<GlyphI> getItemsByPixel(int x, int y);


  /**
   * creates a glyph factory for this map.
   * A glyph factory is used to create glyphs in a defined way
   * without modifying this map's default configurations.
   * Once a factory is added to a widget,
   * glyphs can be created and displayed on the map
   * using a glyph factory by calling {@link #addItem}
   * or other methods
   *
   * <pre> MapGlyphFactory fac = mapwidget.addFactory("-color blue ...etc...");
   * mapwidget.addItem(fac,100,200);</pre>
   *
   * @param config an options <code>String</code>
   *  used to describe this factory.
   * @return the MapGlyphFactory created
   */
  public MapGlyphFactory addFactory(String config);

  /**
   * creates a glyph factory for this map.
   * A glyph factory is used to create glyphs in a defined way
   * without modifying this map's default configurations.
   * Once a factory is added to a widget,
   * glyphs can be created and displayed on the map
   * using a glyph factory by calling {@link #addItem}
   * or other methods.
   *
   * <pre>
   * MapGlyphFactory fac = mapwidget.addFactory("-color blue ...etc...");
   * mapwidget.addItem(fac,100,200);</pre>
   *
   * @param config an options <code>Hashtable</code> used to describe this
   *   factory.
   * @return the MapGlyphFactory created
   */
  public MapGlyphFactory addFactory(Hashtable<String,Object> config);


  /**
   * Constrains the map's resize behavior according to the specified
   * constraint type.
   *
   * @param axisid the axis ({@link NeoWidget#X} or {@link NeoWidget#Y})
   *  to apply the constraint.
   * @param constraint the type of constraint to apply.
   *  The only valid values is+ {@link NeoWidgetI#FITWIDGET}.
   */
  //TODO: never used?  Even if is used, shouldn't this be in NeoWidgetI since NeoWidget implements it?
  public void setReshapeBehavior(int axisid, int constraint);

  /**
   * Gets the constraint set by {@link #setReshapeBehavior}.
   *
   * @param axisid the axis ({@link NeoWidget#X} or {@link NeoWidget#Y}) constrained.
   */
  public int getReshapeBehavior(int axisid);

  /**
   * adds a glyph as a child of another glyph.
   * GlyphIs can be hierarchically associated with other GlyphIs,
   * to control drawing, hit detection, packing, etc..
   * The following code creates a child glyph between 55 and 60
   * and associates it with a parent glyph between 50 and 100.
   *
   * <pre> GlyphI item = map.addItem(50,100);
   * map.addItem(item, map.addItem(55,60));</pre>
   *
   * @param parent the GlyphI that will be the parent
   * @param child the GlyphI that will be the child
   */
  public GlyphI addItem(GlyphI parent, GlyphI child);

  /**
   * set option name/value pairs for this map.
   * Configuration options are implementation-specific
   * and are the primary means
   * of conveying implementation-specific data to a NeoMapI.
   *
   * @param options  the <code>String</code> specifying options
   *  of the form "<code>-option1 value1 -option2 value 2</code> ..."
   */
  public void configure(String options);

  /**
   * set option name/value pairs for this map.
   * Configuration options are implementation-specific
   * and are the primary means
   * of conveying implementation-specific data to a NeoMapI.
   *
   * @param options a <code>Hashtable</code> specifying options of the form<BR>
   *  { "option1" ==&gt; "value1", <BR>
   *    "option2" ==&gt; "value2", <BR>
   *    ...<BR>
   *  }
   */
  public void configure(Hashtable<String,Object> options);

  /**
   * Add a previously created GlyphI to a map.
   * This allows for example the removal of a glyph from one map
   * and adding the same glyph to another map.
   */
  public void addItem(GlyphI gl);

  /**
   * Make this glyph be drawn in front of all other glyphs.
   * Except will not be drawn in front of transient glyphs.
   */
  public void toFront(GlyphI gl);

  /**
   * Make this glyph be drawn behind all other glyphs.
   */
  public void toBack(GlyphI gl);

  /**
   * Returns the an array of ints that specify the range,
   * or start and end of the primary axis, in coordinates.
   */
  public int[] getMapRange();

  /**
   * Returns the an array of ints that specify the offset,
   * or start and end of the secondary axis, in coordinates.
   */
  public int[] getMapOffset();

  /**
   * Returns a Rectangle2D with the maps bounds (x, y, width, height).
   */
  public java.awt.geom.Rectangle2D.Double getCoordBounds();

  /**
   * Returns a Rectangle2D with the
   * coordinate bounds (x, y, width, height)
   * currently displayed in the map's view.
   */
  public java.awt.geom.Rectangle2D.Double getViewBounds();

  /**
   * Adds a viewbox listener to listen for changes
   * to bounds of map's visible area.
   */
  public void addViewBoxListener(NeoViewBoxListener l);

  /**
   * Removes a viewbox listener.
   */
  public void removeViewBoxListener(NeoViewBoxListener l);

  /**
   * Adds a range listener, to listen for changes to the
   * start and end of the viewable range of the map.
   */
  public void addRangeListener(NeoRangeListener l);

  /**
   * Removes a range listener.
   */
  public void removeRangeListener(NeoRangeListener l);

  /**
   * Add a rubberband listener to listen for rubber band
   * events on the map.
   */
  public void addRubberBandListener(NeoRubberBandListener l);

  /**
   * Removes a rubberband listener.
   */
  public void removeRubberBandListener(NeoRubberBandListener l);
}
