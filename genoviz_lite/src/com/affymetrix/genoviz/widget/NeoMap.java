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
import java.awt.event.*;
import java.util.*;
import java.util.List;

import com.affymetrix.genoviz.awt.NeoCanvas;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.transform.ExponentialOneDimTransform;
import com.affymetrix.genoviz.transform.LinearTwoDimTransform;
import com.affymetrix.genoviz.pack.SiblingCoordAvoid;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.pack.PackerI;
import com.affymetrix.genoviz.bioviews.RubberBand;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.drag.DragMonitor;

import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.WidgetAxis;
import com.affymetrix.genoviz.event.*;

import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.HorizontalAxisGlyph;
import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.NeoConstants.Orientation;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

/**
 * NeoMap is the <strong>implementation</strong> of NeoMapI.
 *
 * <p> Documentation for all interface methods can be found in the
 * documentation for NeoMapI.<p>
 *
 * <p> This javadoc explains the implementation
 * specific features of this widget concerning event handling and the
 * java AWT.  In paticular, all genoviz implementations of widget
 * interfaces are subclassed from <code>Container</code> and use the
 * JDK 1.1 event handling model.
 *
 * <p> NeoMap extends <code>java.awt.Container</code>,
 * and thus, inherits all of the AWT methods of
 * <code>java.awt.Container</code>, and <code>Component</code>.
 * For example, a typical application might use the following as
 * part of initialization:
 * <pre>
 *   map = new NeoMap();
 *
 *   map.setBackground(new Color(180, 250, 250));
 *   map.resize(500, 200);
 * </pre>
 */
public class NeoMap extends NeoWidget implements NeoMapI,
NeoDragListener, NeoViewBoxListener, NeoRubberBandListener, ComponentListener {

  static final long serialVersionUID = 1L;

  int stretchCount, reshapeCount;
  protected boolean fit_check = true;

  private static final boolean DEBUG_STRETCH = false;
  private static final boolean DEBUG_RESHAPE = false;
  private static final boolean debug = false;

  private static final boolean NM_DEBUG_PAINT = false;

  protected static final Color default_map_background = new Color(180, 250, 250);
  protected static final Color default_panel_background = Color.lightGray;
  // not sure if foreground is used at all at the moment...
  protected static final Color default_panel_foreground = Color.darkGray;

  // a List of axes added to the map
  // this is maintained in order to stretch them
  // when the range coords of the map change.
  private List<AxisGlyph> axes = new ArrayList<AxisGlyph>();
  private List<HorizontalAxisGlyph> horizontalAxes = new ArrayList<HorizontalAxisGlyph>();

  // fields to keep track of whether ranges have been explicitly set or not
  boolean axis_range_set = false;
  boolean offset_range_set = false;

  // fields for optimizations
  boolean optimize_scrolling = false;
  boolean optimize_damage = false;
  boolean optimize_transients = false;

  // fields for dealing with sequence residue font
  // (only one residue font should be allowed per map)
  Font font_for_max_zoom = new Font("Courier", Font.BOLD, 12);
  FontMetrics seqmetrics;
  DragMonitor canvas_drag_monitor;
  boolean drag_scrolling_enabled = false;

  protected SelectionType selectionMethod = SelectionType.NO_SELECTION;
  protected List<NeoViewBoxListener> viewbox_listeners = new CopyOnWriteArrayList<NeoViewBoxListener>();
  protected List<NeoRangeListener> range_listeners = new CopyOnWriteArrayList<NeoRangeListener>();

  /**
   * Constructs a horizontal NeoMap with scrollbars.
   */
  public NeoMap() {
    this(true, true);
  }

  /**
   * Constructor to create a NeoMap that presents another view
   * of the same scene that a previously created NeoMap shows.
   *
   *  <p>  What is shared between the rootmap and the new map:
   *  <ul>
   *  <li> scene
   *  <li> selected List
   *  <li> axes List
   *  <li> glyph_hash
   *  <li> model_hash
   *  <li> selection appearance (by way of scene)
   *  </ul>
   *
   *  <p>  What is not shared:
   *  <ul>
   *  <li> canvas
   *  <li> view
   *  <li> transform
   *  <li> everything else for now...  (I think...)
   *  </ul>
   *
   * @param rootmap the other map that holds the scene to view.
   */
  public NeoMap(NeoMap rootmap) {
    // this() will set up a normal NeoMap with its own
    //    NeoCanvas, Scene, and View
    this(rootmap.hscroll_show, rootmap.vscroll_show);
    setRoot(rootmap);
  }

  public void setRoot(NeoMap root) {
    // Now need to replace Scene and View with root's Scene, and a
    //   new View onto root's Scene
    canvas.removeNeoPaintListener(view);
    view.removePostDrawViewListener(this);
    view.removeMouseListener(this);
    view.removeMouseMotionListener(this);
    view.removeKeyListener(this);

    // Now set up NeoMap with root's scene, and a new view onto that scene
    scene = root.getScene();

    // don't remove old view from root's scene! this is needed by root!
    view = new View(scene);
    // notify scene of new view on it
    scene.addView(view);
    // configure new view with same component and transform as old view
    view.setComponent(canvas);
    view.setTransform(trans);

    // Now add back event routing for new view
    // view listens to canvas for repaint and AWT events
    canvas.addNeoPaintListener(view);
    canvas.addMouseListener(view);
    canvas.addMouseMotionListener(view);
    canvas.addKeyListener(view);
    canvas.addComponentListener(this);

    // map listens to view for view box change events, mouse events, key events
    view.addPostDrawViewListener(this);
    view.addMouseListener(this);
    view.addMouseMotionListener(this);
    view.addKeyListener(this);
    // Finally, set various fields that need to be shared between
    // this NeoMap and the root
    //
    // some of these fields have no accessors and are protected!
    // should be okay, since setting these fields is only done between
    // two widgets of the same class
    this.glyph_hash = root.glyph_hash;
    this.model_hash = root.model_hash;
    this.axes = root.axes;
    this.horizontalAxes = root.horizontalAxes;
    this.selected = root.getSelected();

    // Set the background color of the new map to that of the root map
    this.setMapColor(root.getMapColor());
  }

  /**
   * creates a horizontal map with 0, 1, or 2 scrollbars.
   *
   * @param hscroll_show determines whether or not to show a horizontal scrollbar.
   * @param vscroll_show determines whether or not to show a vertical scrollbar.
   */
  public NeoMap(boolean hscroll_show, boolean vscroll_show) {
    this(hscroll_show, vscroll_show, NeoConstants.Orientation.Horizontal,
         new LinearTwoDimTransform());
  }

  /**
   * Creates a horizontal map.
   *
   * @param hscroll_show determines whether or not to show a horizontal scrollbar.
   * @param vscroll_show determines whether or not to show a vertical scrollbar.
   * @param hscroll_location determines where to show a the horizontal scrollbar.
   * @param vscroll_location determines where to show a the vertical scrollbar.
   */
  public NeoMap(boolean hscroll_show, boolean vscroll_show, String hscroll_location, String vscroll_location) {
    this(hscroll_show, vscroll_show, NeoConstants.Orientation.Horizontal,
         new LinearTwoDimTransform(), hscroll_location, vscroll_location);
  }

  /**
   * Creates a horizontal or vertical map with scrollbars.
   *
   * @param orient must be {@link com.affymetrix.genoviz.util.NeoConstants.Orientation#Horizontal} 
   * or {@link com.affymetrix.genoviz.util.NeoConstants.Orientation#Vertical}. 
   */
  public NeoMap(NeoConstants.Orientation orient) {
    this(true, true, orient, new LinearTwoDimTransform());
  }

  /**
   * constructs a map with the given configuration.
   * If scroll bars are requested, they are put in their default locations.
   *
   * @param hscroll_show determines whether or not to show a horizontal scrollbar.
   * @param vscroll_show determines whether or not to show a vertical scrollbar.
   * @param orient must be {@link Orientation#Horizontal} or {@link Orientation#Vertical}.
   * @param tr LinearTwoDimTransform for zooming.
   */
  public NeoMap(boolean hscroll_show, boolean vscroll_show,
    NeoConstants.Orientation orient, LinearTwoDimTransform tr) {
    // use default hscroll_loc and vscroll_loc
    this(hscroll_show, vscroll_show, orient, tr, hscroll_default_loc, vscroll_default_loc);
  }

  /**
   * constructs a map with the given configuration.
   *
   * @param hscroll_show determines whether or not to show a horizontal scrollbar
   * @param vscroll_show determines whether or not to show a vertical scrollbar
   * @param orient must be {@link Orientation#Horizontal} or {@link Orientation#Vertical}.
   * @param tr LinearTwoDimTransform for zooming
   * @param hscroll_location can be "North", otherwise "South" is assumed.
   * @param vscroll_location can be "West", otherwise "East" is assumed.
   */
  public NeoMap(boolean hscroll_show, boolean vscroll_show,
      NeoConstants.Orientation orient, LinearTwoDimTransform tr, String hscroll_location, String vscroll_location) {
    super();
    this.hscroll_show = hscroll_show;
    this.vscroll_show = vscroll_show;
    this.hscroll_loc = hscroll_location;
    this.vscroll_loc = vscroll_location;
    this.orient = orient;

    this.trans = tr;

    scene = new Scene();
    canvas = new NeoCanvas();
    enableDragScrolling(drag_scrolling_enabled);

    setRangeScroller(new JScrollBar(JScrollBar.HORIZONTAL));
    setOffsetScroller(new JScrollBar(JScrollBar.VERTICAL));

    zoomer[Xint] = null;
    zoomer[Yint] = null;
    scale_constraint[Xint] = ScaleConstraint.NONE;
    scale_constraint[Yint] = ScaleConstraint.NONE;
    zoom_behavior[Xint] = ZoomConstraint.CONSTRAIN_MIDDLE;
    zoom_behavior[Yint] = ZoomConstraint.CONSTRAIN_MIDDLE;
    zoom_coord[Xint] = 0;
    zoom_coord[Yint] = 0;

    setMapRange(0,100);
    setMapOffset(0,100);

    view = new View(scene);
    scene.addView(view);
    view.setComponent(canvas);
    view.setTransform(trans);

    setPixelBounds();

    seqmetrics = GeneralUtils.getFontMetrics(font_for_max_zoom);
    max_pixels_per_coord[Xint] = seqmetrics.charWidth('C');

    max_pixels_per_coord[Yint] = 10;
    min_pixels_per_coord[Xint] = min_pixels_per_coord[Yint] = 0.01f;

    initComponentLayout();

    /*
     * checking for whether these scrollbars are used
     * (should really default to AUTO_SCROLL_INCREMENT anyway
     *  and reset in widgets that don't want it [like NeoSeq] )
     */
    if (hscroll_show)  {
      setScrollIncrementBehavior(WidgetAxis.Primary, ScrollIncrementBehavior.AUTO_SCROLL_INCREMENT);
    }

    if (vscroll_show)  {
      setScrollIncrementBehavior(WidgetAxis.Secondary, ScrollIncrementBehavior.AUTO_SCROLL_INCREMENT);
    }

    glyph_hash = new Hashtable<GlyphI,Object>();
    model_hash = new Hashtable<Object,Object>();

    // defaults to black background!!!
    setBackground(default_panel_background);
    setForeground(default_panel_foreground);
    setMapColor(default_map_background);

    // Set up and activate a default rubber band.
    RubberBand defaultBand = new RubberBand(canvas);
    defaultBand.setColor(Color.blue);
    setRubberBand( defaultBand );

    // view listens to canvas for repaint and AWT events
    canvas.addNeoPaintListener(view);
    canvas.addMouseListener(view);
    canvas.addMouseMotionListener(view);
    canvas.addKeyListener(view);
    canvas.addComponentListener(this);

    // map listens to view for view box change events, mouse events, key events
    view.addPostDrawViewListener(this);
    view.addMouseListener(this);
    view.addMouseMotionListener(this);
    view.addKeyListener(this);

    // Set a default NullPacker so that the packer property can work in a bean box.
    setPacker(new SiblingCoordAvoid());

  }

  /**
   * Lay out the Components contained within this NeoMap.
   * In the case of the base NeoMap, the NeoCanvas and NeoScrollbars.
   * This has been separated out from constructor
   * to allow for subclasses to more easily change layout.
   */
  public void initComponentLayout() {
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      this.setLayout(gbl);
      if (hscroll_show && scroller[Xint] instanceof Component)  {
        gbc.fill = GridBagConstraints.HORIZONTAL;
        if (hscroll_loc.equalsIgnoreCase("North")) {
          gbc.anchor = GridBagConstraints.NORTH;
        }
        else {
          gbc.anchor = GridBagConstraints.SOUTH;
        }
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbl.setConstraints((Component)scroller[Xint], gbc);
        add((Component)scroller[Xint]);
      }
      if (vscroll_show && scroller[Yint] instanceof Component)  {
        gbc.fill = GridBagConstraints.VERTICAL;
        if (vscroll_loc.equalsIgnoreCase("West")) {
          gbc.anchor = GridBagConstraints.WEST;
        }
        else {
          gbc.anchor = GridBagConstraints.EAST;
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        gbl.setConstraints((Component)scroller[Yint], gbc);
        add((Component)scroller[Yint]);
      }
      gbc.fill = GridBagConstraints.BOTH;
      gbc.anchor = GridBagConstraints.CENTER;
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 1;
      gbc.gridheight = 1;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbl.setConstraints(canvas, gbc);
      add(canvas);
  }


  public void setRubberBand( RubberBand theBand ) {
    if ( null != this.rband ) {
      this.rband.removeRubberBandListener( this );
      setRubberBandBehavior( false );
    }
    this.rband = theBand;
    if ( null != this.rband ) {
      this.rband.setComponent( canvas );
      this.rband.addRubberBandListener(this);
    }
    setRubberBandBehavior( null != this.rband );
  }

  /**
   * Destructor that unlocks graphic resources, cuts links.
   * Call only when the map is no longer being displayed.
   * This overrides {@link NeoWidget#destroy()}
   * so that we can clear the rubber band and it's listeners.
   */
  @Override
  public void destroy() {
    clearWidget();
    super.destroy();
    if ( this.rband != null ) {
      this.rband.removeRubberBandListener( this );
    }
    this.rband = null;
  }

  /**
   * Reshapes the NeoMap.
   * Often called during window resize events.
   * Depending on layout,
   * this descends down telling panels what size they have available.
   * Overridden here to force a layout (which usually happens anyway).
   * <p> Note: if width or heigh is less than 1, it will be set to 1.
   *
   * @deprecated use {@link #setBounds(int,int,int,int)}, but if you need
   *  to override reshape behavior, override this method, not setBounds
   *  (due to weirdness in the Container source code from Sun).
   */
  @Deprecated
  @Override
  public void reshape(int x, int y, int width, int height) {
    reshapeCount++;
    if (width < 1) {
      width = 1;
    }
    if (height < 1) {
      height = 1;
    }
    if (debug || DEBUG_RESHAPE) {
      System.out.println("NeoMap being reshaped " + reshapeCount +
                         ": " + x + " " + y +
                         " " + width + " " + height );
    }
    super.reshape(x, y, width, height);

    this.doLayout();

    /*
      Forcing a layout in reshape for two reasons

      First, this allows the components to be laid out even in the
      absence of peers (I think), whereas Component.reshape() will only
      call invalidate() and hence layout() if peer != null.  I believe
      this is the cause of a number of layout problems, where the size of
      the canvas is not taken into account because the layout did not
      occur...

      Second, it seems to fix a bug in Cafe that would cause occasional
      failure to repaint and/or correctly reshape the map canvas when
      the component containing them was resized
      -- oops, that bug is back --  GAH 8/11/97

      Unfortunately, this usually results in redundant calls to NeoCanvas
      and NeoScrollbar reshape() and repaint(), but that seems more acceptable
      than the problems it fixes, since resizing is a fairly rare event
     */
    if (debug || DEBUG_RESHAPE) {
        System.out.println("done with NeoMap.reshape()" + reshapeCount + ",  " +
                           x + " " + y + " " + width + " " + height );
    }
  }

  /** Does nothing. */
  public void componentHidden(ComponentEvent evt) { }
  /** Does nothing. */
  public void componentMoved(ComponentEvent evt) { }
  /** Does nothing. */
  public void componentShown(ComponentEvent evt) { }

    /*
     * GAH 3-2002
     *   trying a new approach here, where setPixelBounds(),
     *     stretchToFit(), canvas image nulling, are all handled in
     *     another method in response to changes in canvas size
     *     (with the NeoMap listening to the canvas for reshape events)
     *   I'm trying this because it appears that the assumption that reshape
     *      (and layout?) calls to a Container (in this case the NeoMap) will
     *      result in children's layout / reshaping in same thread is no longer valid.
     *      In tests I've been doing, it appears that the Container.reshape() call can
     *      return in the current thread and then the reshaping of the children gets
     *      triggered later (likely on the same thread (EventThread)) but
     *      scheduled for later.  Blech!
     */
  public void componentResized(ComponentEvent evt) {
    if (evt.getSource() == canvas) {
      //-----  this is the only place pixel_* should change -----
      setPixelBounds();
      stretchToFit();
    }
  }


  /**
   * Set the minimum and maximum range (primary axis) coordinates.
   * Note that {@link #setMapRange(int,int)} and {@link #setMapOffset(int, int)} 
   * are the only places that the map coord box should change.
   */
  public void setMapRange(int start, int end) {
    // scene.setCoords() is now handled in setBounds()

    if (! isHorizontal()) {
      //      this.setBounds(Secondary,start,end);
      this.setFloatBounds(WidgetAxis.Secondary,(double)start,(double)end);
    }
    else {
      //      this.setBounds(Primary,start,end);
      this.setFloatBounds(WidgetAxis.Primary,(double)start,(double)end);
    }

    if (axes != null) {
      for (int i=0; i<axes.size(); i++) {
        axes.get(i).rangeChanged(); // notify the axis of the range change.
      }
    }
    if (horizontalAxes != null) {
      for (int i=0; i<horizontalAxes.size(); i++) {
        horizontalAxes.get(i).rangeChanged(); // notify the axis of the range change.
      }
    }

  }

  public int[] getMapRange() {
    int[] range = new int[2];
    java.awt.geom.Rectangle2D.Double cb = getCoordBounds();
    if (! isHorizontal()) {
      range[0] = (int) cb.y;
      range[1] = (int) (cb.y + cb.height);
    }
    else {
      range[0] = (int) cb.x;
      range[1] = (int) (cb.x + cb.width);
    }
    return range;
  }

  public int[] getVisibleRange() {
    int[] range = new int[2];
    Rectangle2D.Double cb = getViewBounds();
    if (! isHorizontal()) {
      range[0] = (int) cb.y;
      range[1] = (int) (cb.y + cb.height);
    }
    else {
      range[0] = (int) cb.x;
      range[1] = (int) (cb.x + cb.width);
    }
    return range;
  }


  /**
   * Sets the visible range to exactly match the given range.
   * This can cause the zoom level to change.
   * @param start
   * @param end
   */
  public void setVisibleRangeTo(int start, int end) {
  }
  
  /**
   * Scroll the map such that the specified range is
   * included somewhere in the visible range.  This does not
   * change the zoom level and does not set the visible range
   * to exactly match the given coordinates.
   * @param start
   * @param end
   */
  public void scrollRangeToVisible(int start, int end) {
    
  }
  
  /**
   * Set the minimum and maximum offset (secondary axis) coordinates.
   * Note that {@link #setMapRange(int,int)} and {@link #setMapOffset(int, int)} 
   * are the only places that the map coord box should change.
   */
  public void setMapOffset(int start, int end) {
    // scene.setCoords() is now handled in setBounds()
    
    //TODO!: is this right?
    
    if (! isHorizontal()) {
      this.setBounds(WidgetAxis.Primary, start, end);
    }
    else  {
      this.setBounds(WidgetAxis.Secondary,start,end);
    }
  }

  public int[] getMapOffset() {
    int[] range = new int[2];
    java.awt.geom.Rectangle2D.Double cb = getCoordBounds();
    if (! isHorizontal()) {
      range[0] = (int) cb.x;
      range[1] = (int) (cb.x + cb.width);
    }
    else {
      range[0] = (int) cb.y;
      range[1] = (int) (cb.y + cb.height);
    }
    return range;
  }

  public int[] getVisibleOffset() {
    int[] range = new int[2];
    java.awt.geom.Rectangle2D.Double cb = getViewBounds();
    if (! isHorizontal()) {
      range[0] = (int) cb.x;
      range[1] = (int) (cb.x + cb.width);
    }
    else {
      range[0] = (int) cb.y;
      range[1] = (int) (cb.y + cb.height);
    }
    return range;
  }

  public void stretchToFit() {
    stretchToFit((reshape_constraint[Xint] == FITWIDGET),
                 (reshape_constraint[Yint] == FITWIDGET));
  }

  /**
   *  xfit and yfit override reshape_constraint[Primary] and reshape_constraint[Secondary]
   */
  @Override
  public void stretchToFit(boolean xfit, boolean yfit) {
    stretchCount++;
    if (DEBUG_STRETCH)  {
      System.out.println("in NeoMap.stretchToFit(" + xfit + ", " + yfit + ")");
      System.out.println(canvas.isVisible() + ", " + canvas.isShowing() +
                         ", " + canvas);
    }
    scene.maxDamage();  // max out scene damage to ensure full redraw
    trans = view.getTransform();
    double xscale, xoffset, yscale, yoffset;
    xscale = trans.getScaleX();
    xoffset = trans.getOffsetX();
    yscale = trans.getScaleY();
    yoffset = trans.getOffsetY();

    /*
     * GAH 4-10-2002
     *  added a callout to calcFittedTransform() here so that subclasses
     *  (particularly new tiered map implementation) can calculate fitted
     *  transform differently if desired, without having to deal with
     *  reimplementing rest of stretchToFit complexity
     *
     *  (in the case of new tiered map implementation, want to calculate fitted
     *   transform differently because there may be tiers of fixed pixel size that
     *   must be taken into account)
     */
    // not sure if setPixelBox() call is needed...
    view.setPixelBox(new Rectangle(0, 0, canvas.getSize().width, canvas.getSize().height));
    trans = calcFittedTransform();  // GAH 4-10-2002
    view.setTransform(trans);
    view.setComponent(canvas);

    if (!set_min_pix_per_coord[Xint]) {
      min_pixels_per_coord[Xint] = trans.getScaleX();
    }
    if (!set_min_pix_per_coord[Yint]) {
      min_pixels_per_coord[Yint] = trans.getScaleY();
    }

    // checking in case map is small, stretchToFit may build a transform with
    //   scale larger than max_pixels_per_coord
    if (min_pixels_per_coord[Xint] >= max_pixels_per_coord[Xint]) {
      min_pixels_per_coord[Xint] = max_pixels_per_coord[Xint];
      trans.setScaleX(min_pixels_per_coord[Xint]);
      trans.setOffsetX(canvas.getSize().width/2 -
      trans.getScaleX()*scene.getCoordBox().width/2);
    }
    if (min_pixels_per_coord[Yint] >= max_pixels_per_coord[Yint]) {
      min_pixels_per_coord[Yint] = max_pixels_per_coord[Yint];
      trans.setScaleY(min_pixels_per_coord[Yint]);
      trans.setOffsetY(
        canvas.getSize().height/2 -
        trans.getScaleY()*scene.getCoordBox().height/2);
    }

    if (!(xfit && yfit)) {

      /*
       * put in check and fix for when neomap has been reshaped
       * bigger, and transform is such that keeping x/yoffset at left/top
       * edge will result in waste of real estate and display ends up
       * out of sync with h/vscroller
       */
      java.awt.geom.Rectangle2D.Double viewbox;
      java.awt.geom.Rectangle2D.Double scenebox = scene.getCoordBox();
      double scene_start, scene_end, view_start, view_end, visible_start;
      Rectangle scenepix;
      int pixel_value;
      if (!xfit) {
        trans.setScaleX(xscale);
        trans.setOffsetX(xoffset);
        view.calcCoordBox();
        viewbox = view.getCoordBox();
        if (fit_check)  {
          scene_start = scenebox.x;
          scene_end = scenebox.x + scenebox.width;
          view_start = viewbox.x;
          view_end = viewbox.x + viewbox.width;
          if (scene_end < view_end  && scene_start < view_start) {
            scenepix = new Rectangle();
            view.transformToPixels(scenebox, scenepix);
            if (viewbox.width > scenebox.width) {
              visible_start = scene_start;
            }
            else {
              visible_start = scene_end - viewbox.width;
            }
            pixel_value = (int)(visible_start * trans.getScaleX());
            trans.setOffsetX(-pixel_value);
          }
        }
      }
      if (!yfit) {
        trans.setScaleY(yscale);
        trans.setOffsetY(yoffset);
        view.calcCoordBox();
        viewbox = view.getCoordBox();
        if (fit_check) {
          scene_start = scenebox.y;
          scene_end = scenebox.y + scenebox.height;
          view_start = viewbox.y;
          view_end = viewbox.y + viewbox.height;
          if (scene_end < view_end  && scene_start < view_start) {
            scenepix = new Rectangle();
            view.transformToPixels(scenebox, scenepix);
            if (viewbox.height > scenebox.height) {
              visible_start = scene_start;
            }
            else {
              visible_start = scene_end - viewbox.height;
            }
            pixel_value = (int)(visible_start * trans.getScaleY());
            trans.setOffsetY(-pixel_value);
          }
        }
      }
    }

    pixels_per_coord[Xint] = trans.getScaleX();
    coords_per_pixel[Xint] = 1/pixels_per_coord[Xint];

    pixels_per_coord[Yint] = trans.getScaleY();
    coords_per_pixel[Yint] = 1/pixels_per_coord[Yint];

    if (zoomer[Xint] != null)  {
      // setting maxy of exponential tranform to (max - visible amount) to
      // compensate for the fact that in JDK1.1 and Swing Scrollbars,
      // the maximum for the value is really the scrollbar maximum minus
      // the visible amount (the thumb)
      zoomtrans[Xint] = new ExponentialOneDimTransform(
        min_pixels_per_coord[Xint],
        max_pixels_per_coord[Xint],
        zoomer[Xint].getMinimum(),
        zoomer[Xint].getMaximum()-zoomer[Xint].getExtent());

      adjustZoomer(WidgetAxis.Primary);
    }
    adjustScroller(WidgetAxis.Primary);
    if (zoomer[Yint] != null) {
      // setting maxy of exponential tranform to (max - visible amount) to
      // compensate for the fact that in JDK1.1 and Swing Scrollbars,
      // the maximum for the value is really the scrollbar maximum minus
      // the visible amount (the thumb)
      zoomtrans[Yint] = new ExponentialOneDimTransform(
        min_pixels_per_coord[Yint],
        max_pixels_per_coord[Yint],
        zoomer[Yint].getMinimum(),
        zoomer[Yint].getMaximum()-zoomer[Yint].getExtent());
      adjustZoomer(WidgetAxis.Secondary);
    }
    adjustScroller(WidgetAxis.Secondary);

    if (DEBUG_STRETCH)  {
      System.out.println("leaving NeoMap.stretchToFit(): " + stretchCount);
    }

  }

  public LinearTwoDimTransform calcFittedTransform() {
    LinearTwoDimTransform new_trans = new LinearTwoDimTransform();
    new_trans.fit(scene.getCoordBox(), view.getPixelBox());
    return new_trans;
  }

  public AxisGlyph addAxis(int offset) {
    AxisGlyph axis = null;
    if (! isHorizontal()) {
      axis = new AxisGlyph(NeoConstants.Orientation.Vertical);
      axis.setCoords(offset-10, scene.getCoordBox().y, 20,
                     scene.getCoordBox().height);
    }
    else {
      axis = new AxisGlyph();
      axis.setCoords(scene.getCoordBox().x, offset-10,
                     scene.getCoordBox().width, 20);
    }
    axis.setForegroundColor(Color.black);
    scene.getRootGlyph().addChild(axis);
    axes.add(axis);
    return axis;
  }


  public HorizontalAxisGlyph addHorizontalAxis(int offset) {
    HorizontalAxisGlyph axis = new HorizontalAxisGlyph();
    axis.setCoords(scene.getCoordBox().x, offset-10,
                   scene.getCoordBox().width, 20);
    axis.setForegroundColor(Color.black);
    scene.getRootGlyph().addChild(axis);
    horizontalAxes.add(axis);
    return axis;
  }

  @Override
  public void setRangeZoomer(JSlider slider) {
    setZoomer(WidgetAxis.Primary, slider);
  }

  @Override
  public void setOffsetZoomer(JSlider slider) {
    setZoomer(WidgetAxis.Secondary, slider);
  }

  public void scrollOffset(double value) {
    scroll(WidgetAxis.Secondary, value);
  }

  public void scrollRange(double value) {
    scroll(WidgetAxis.Primary, value);
  }


  @Override
  public void zoomRange(double zoom_scale) {
    zoom(WidgetAxis.Primary, zoom_scale);
  }

  @Override
  public void zoomOffset(double zoom_scale) {
    zoom(WidgetAxis.Secondary, zoom_scale);
  }

  /**
   * Simply calls parent.addChild(child), so we don't really need this method.
   * @return null
   */
  @Override
  public GlyphI addItem(GlyphI parent, GlyphI child) {
    parent.addChild(child);
    return null;
  }

  /**
   * setRangeScroller() and setOffsetScroller() should probably be combined
   * with setZoomer() to have a more general
   * setAdjustable(int id, Adjustable adj) method.  But at the moment the
   * scroller[] entries are expected to be NeoScrollbars
   */
  public void setRangeScroller(JScrollBar nscroll) {
    setScroller(WidgetAxis.Primary, nscroll);
  }

  public void setOffsetScroller(JScrollBar nscroll) {
    setScroller(WidgetAxis.Secondary, nscroll);
  }

  public JScrollBar getScroller(WidgetAxis dim) {
    return scroller[dim.ordinal()];
  }

  /**
   * @param dim should be {@link #Primary} or {@link #Secondary}.
   * @return the slider responsible for zooming in the <var>id</var> direction.
   */
  public JSlider getZoomer(WidgetAxis dim) {
    return zoomer[dim.ordinal()];
  }

  @Override
  public void removeItem(GlyphI gl) {
    scene.removeGlyph(gl);
    glyph_hash.remove(gl);

    selected.remove(gl);

    Object model = gl.getInfo();
    if (model != null) {
      Object item2= model_hash.get(model);
      if (item2 == gl) {
        model_hash.remove(model);
      }
      else if (item2 instanceof List) {
        ((List)item2).remove(gl);
      }
    }

  }

  public void removeItem(List<GlyphI> vec) {
    /*
     * Remove from end of child List instead of beginning! -- that way, won't
     * get issues with trying to access elements off end of List as
     * List shrinks during removal, if List is actually one of map/glyph/etc.
     * internal Lists
     */
    // xxx
    int count = vec.size();
    for (int i=count-1; i>=0; i--) {
      GlyphI g = vec.get(i);
      if (null != g) {
        removeItem(g);
      }
    }
  }

  //  also need to add a removeFactory method...

  /**
   * Removes all glyphs.
   * However, factories, dataadapters, coord bounds, etc. remain.
   */
  @Override
  public void clearWidget() {
    super.clearWidget();
    // create new eveGlyph, set it's coords and expansion behavior to old eveGlyph
    RootGlyph oldeve = scene.getRootGlyph();
    Rectangle2D.Double evebox = oldeve.getCoordBox();
    RootGlyph neweve = new RootGlyph();
    neweve.setExpansionBehavior(XY.X, oldeve.getExpansionBehavior(XY.X));
    neweve.setExpansionBehavior(XY.Y, oldeve.getExpansionBehavior(XY.Y));
    neweve.setCoords(evebox.x, evebox.y, evebox.width, evebox.height);
    scene.setRootGlyph(neweve);

    // reset glyph_hash
    glyph_hash = new Hashtable<GlyphI,Object>();

    // reset model_hash
    model_hash = new Hashtable<Object,Object>();

    // reset axes
    axes.clear();
    horizontalAxes.clear();

    // remove all the transient glyphs.
    scene.removeAllTransients();

    // let the listeners know.
    fireNeoWidgetEvent( new NeoWidgetEvent( this, 0 ) );

  }


  @Override
  public void setSelectionEvent(SelectionType theMethod) {
    switch (theMethod) {
    case NO_SELECTION:
    case ON_MOUSE_DOWN:
    case ON_MOUSE_UP:
      this.selectionMethod = theMethod;
      break;
    default:
       throw new IllegalArgumentException("theMethod must be one of "
         + "NO_SELECTION, ON_MOUSE_DOWN, or ON_MOUSE_UP");
    }
  }

  @Override
  public SelectionType getSelectionEvent() {
    return this.selectionMethod;
  }

  /**
   *  not allowing mixing of optimizations yet
   */
  public void setDamageOptimized(boolean optimize_damage) {
    this.optimize_damage = optimize_damage;
    setOptimizations();
  }
  public boolean isDamageOptimized() {
    return this.optimize_damage;
  }

  public void setScrollingOptimized(boolean optimize_scrolling) {
    this.optimize_scrolling = optimize_scrolling;
    setOptimizations();
  }
  public boolean isScrollingOptimized() {
    return this.optimize_scrolling;
  }

  public void setTransientOptimized(boolean optimize_transients) {
      this.optimize_transients = optimize_transients;
      setOptimizations();
  }

  /**
   * Indicates whether or not this map is optimized for drawing transient glyphs.
   *
   * @see com.affymetrix.genoviz.glyph.TransientGlyph
   */
  public boolean isTransientOptimized() {
    return optimize_transients;
  }


  public void setOptimizations() {
    view.setDamageOptimized(optimize_damage);
    view.setScrollingOptimized(optimize_scrolling);
  }

  @Override
  public void update(Graphics g) {
    if (NM_DEBUG_PAINT)  {
      System.out.println("NeoMap.update() called");
    }
    paint(g);
  }

  @Override
  public void repaint() {
    if (NM_DEBUG_PAINT)  {
      System.out.println("NeoMap.repaint() called");
    }
    super.repaint();
    if (NM_DEBUG_PAINT)  {
    }
  }


  @Override
  public void paint(Graphics g) {
    if (NM_DEBUG_PAINT) {
      System.out.println("NeoMap.paint() called");
    }
    super.paint(g);
    if (NM_DEBUG_PAINT)  {
    }
  }


  @Override
  public void setMapColor(Color col) {
    canvas.setBackground(col);
  }

  @Override
  public Color getMapColor() {
    return canvas.getBackground();
  }

  public PackerI getPacker() {
    return this.getScene().getRootGlyph().getPacker();
  }

  /**
   * Add a glyph to the map.
   * @param gl to add.
   *
   * <p> <strong>Warning</strong>
   * -- Before adding a glyph to a map with this method
   * you <em>must</em> remove it from the map it previously belonged to.
   * If this is not what you want, you should duplicate the glyph
   * and add the duplicate to the new map, rather than the original.
   *
   * <p> This restriction exists because glyphs can only exist on one map,
   *     except where additional maps are derived from a root map via
   *     setRoot() or constructor(root_map) (in which case putting a glyph
   *     on one map automatically propogates to other maps derived from the
   *     same root, so addItem(tag) is not needed for such cases.)
   */
  @Override
  public void addItem(GlyphI gl) {
    if (gl != null) {
      scene.addGlyph(gl);
    }
  }

  @Override
  public void toFront(GlyphI gl) {
    scene.toFront(gl);
  }

  @Override
  public void toBack(GlyphI gl) {
    scene.toBack(gl);
  }

  @Override
  public void repack() {
    scene.maxDamage();
    scene.getRootGlyph().pack();
  };

  public void setPacker(PackerI packer) {
    this.getScene().getRootGlyph().setPacker(packer);
    //    ((MapScene)this.getScene()).setPacker(packer);
  }

  /**
   * Set max zoom to exact width of font.
   */
  public void setMaxZoomToFont(Font fnt) {
    font_for_max_zoom = fnt;
    seqmetrics = view.getGraphics().getFontMetrics(font_for_max_zoom);
    int font_width = seqmetrics.charWidth('C');
    setMaxZoom(WidgetAxis.Primary, font_width);
  }


  /**
   * Listens for NeoDragEvents generated by a {@link DragMonitor}.
   * The DragMonitor, in turn, is listening to the canvas for mouse events
   * and generating appropriately timed NeoDragEvents.
   * This is used to implement drag scrolling.
   * @see #enableDragScrolling(boolean)
   */
  @Override
  public void heardDragEvent(NeoDragEvent evt) {
    if (!drag_scrolling_enabled) { return; }
    Object src = evt.getSource();
    NeoConstants.Direction direction = evt.getDirection();
    if (src == canvas_drag_monitor) {


      double scroll_to_coord;
      int pixels_per_scroll = 10;
      if (direction == NeoConstants.Direction.UP) {
        scroll_to_coord =
          trans.inverseTransform(WidgetAxis.Secondary, -pixels_per_scroll);
        scroll(WidgetAxis.Secondary, scroll_to_coord);
        updateWidget();
      }
      else if (direction == NeoConstants.Direction.DOWN) {
        scroll_to_coord =
          trans.inverseTransform(WidgetAxis.Secondary, pixels_per_scroll);
        scroll(WidgetAxis.Secondary, scroll_to_coord);
        updateWidget();
      }
      else if (direction == NeoConstants.Direction.RIGHT) {
        scroll_to_coord =
          trans.inverseTransform(WidgetAxis.Primary, pixels_per_scroll);
        scroll(WidgetAxis.Primary, scroll_to_coord);
        updateWidget();
      }
      else if (direction == NeoConstants.Direction.LEFT) {
        scroll_to_coord =
          trans.inverseTransform(WidgetAxis.Primary, -pixels_per_scroll);
        scroll(WidgetAxis.Primary, scroll_to_coord);
        updateWidget();
      }
    }
  }


  /**
   * Enable scrolling of map when there is a mouse down inside canvas
   * then dragged outside canvas.
   * Uses a {@link DragMonitor} and {@link NeoDragEvent}s to allow scrolling of map.
   */
  public void enableDragScrolling(boolean enable) {
    drag_scrolling_enabled = enable;
    if (drag_scrolling_enabled) { // drag scrolling turned on
      if (canvas_drag_monitor != null) {
        canvas.removeMouseListener(canvas_drag_monitor);
        canvas.removeMouseMotionListener(canvas_drag_monitor);
        canvas_drag_monitor.removeDragListener(this);
      }
      // DragMonitor constructor also adds itself as listener to canvas!
      canvas_drag_monitor = new DragMonitor(canvas);
      canvas_drag_monitor.addDragListener(this);
    }
    else {  // drag scrolling turned off
      if (canvas_drag_monitor != null) {
        canvas.removeMouseListener(canvas_drag_monitor);
        canvas.removeMouseMotionListener(canvas_drag_monitor);
        canvas_drag_monitor.removeDragListener(this);
      }
      canvas_drag_monitor = null;
    }
  }

  /**
   * Handles internal selection when
   * selection event has been set to ON_MOUSE_DOWN or ON_MOUSE_UP.
   *
   *<p> Calls super.heardMouseEvent() to invoke further NeoWidget handling
   * of mouse events (for propagation of event to MouseListeners
   * and MouseMotionListeners registered to listen for mouse events
   * on widget/map.
   */
  @Override
  public void heardMouseEvent(MouseEvent e) {
    if (! (e instanceof NeoViewMouseEvent)) { return; }
    NeoViewMouseEvent nme = (NeoViewMouseEvent)e;
    Object source = nme.getSource();
    int id = nme.getID();
    // else if (NO_SELECTION != selectionMethod && evt.target == this.scene) {
    if (SelectionType.NO_SELECTION != selectionMethod && source == this.view) {
      boolean shiftDown = nme.isShiftDown();
      boolean controlDown = nme.isControlDown();
      boolean metaDown = nme.isMetaDown();
      //      boolean altDown = nme.isAltDown();
      if ((id == NeoMouseEvent.MOUSE_PRESSED &&
          SelectionType.ON_MOUSE_DOWN == selectionMethod) ||
        (id == NeoMouseEvent.MOUSE_RELEASED &&
         SelectionType.ON_MOUSE_UP == selectionMethod)) {
        List<GlyphI> prev_items = this.getSelected();
        int prev_items_size = prev_items.size();
        if (prev_items_size > 0 &&  !(shiftDown || controlDown || metaDown)) {
          this.deselect(prev_items);
        }
        List<GlyphI> candidates = this.getItems(nme.getCoordX(), nme.getCoordY());
        if (candidates.size() > 0 && (shiftDown || controlDown)) {

          List<GlyphI> in = new ArrayList<GlyphI>(), out = new ArrayList<GlyphI>();
          for (GlyphI obj : candidates) {
            if (prev_items.contains(obj)) {
              out.add(obj);
            }
            else {
              in.add(obj);
            }
            if (0 < out.size()) {
              this.deselect(out);
            }
            if (0 < in.size()) {
              this.select(in);
            }
          }
        }
        if (0 < candidates.size() && !(shiftDown || controlDown)) {
          this.select(candidates);
        }
        if (0 < candidates.size() + prev_items_size) {
          this.updateWidget();
        }
      }
    }
    // Call super event handling to use NeoWidget
    // for propagating events to listeners.
    super.heardMouseEvent(e);
  }

  public void viewBoxChanged(NeoViewBoxChangeEvent e) {
    if (viewbox_listeners.size() > 0) {
      NeoViewBoxChangeEvent vevt =
        new NeoViewBoxChangeEvent(this, e.getCoordBox());

      for (NeoViewBoxListener nvbl : viewbox_listeners) {
        nvbl.viewBoxChanged(vevt);
      }
    }
    if (range_listeners.size() > 0) {
      java.awt.geom.Rectangle2D.Double vbox = e.getCoordBox();
      NeoRangeEvent nevt = null;
      if (! isHorizontal()) {
          nevt = new NeoRangeEvent(this, vbox.y, vbox.y + vbox.height);
      }
      else {
          nevt = new NeoRangeEvent(this, vbox.x, vbox.x + vbox.width);
      }

      for (NeoRangeListener rl : range_listeners) {
        rl.rangeChanged(nevt);
        // currently range events are generated for _any_ viewbox change
        //    event, so sometimes the range may not actually have changed,
        //    might be only "offset" that is changing
      }
    }
  }

  public void rubberBandChanged(NeoRubberBandEvent e) {
    if (rubberband_listeners.size() > 0) {
      // not transforming to widget pixels (yet)
      NeoRubberBandEvent nevt =
        new NeoRubberBandEvent(this, e.getID(), e.getWhen(), e.getModifiers(),
                               e.getX(), e.getY(), e.getClickCount(),
                               e.isPopupTrigger(), e.getRubberBand());
      for (NeoRubberBandListener rbl : rubberband_listeners) {
        rbl.rubberBandChanged(nevt);
      }
    }
  }

  public void addViewBoxListener(NeoViewBoxListener l) {
    if (!viewbox_listeners.contains(l)) {
      viewbox_listeners.add(l);
    }
  }

  public void removeViewBoxListener(NeoViewBoxListener l) {
    viewbox_listeners.remove(l);
  }

  public void addRangeListener(NeoRangeListener l) {
    if (!range_listeners.contains(l)) {
      range_listeners.add(l);
    }
  }

  public void removeRangeListener(NeoRangeListener l) {
    range_listeners.remove(l);
  }

  public Hashtable getModelMapping() {
    return model_hash;
  }

}
