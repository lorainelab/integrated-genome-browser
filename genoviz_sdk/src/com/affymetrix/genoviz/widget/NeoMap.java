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

package com.affymetrix.genoviz.widget;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.affymetrix.genoviz.awt.NeoCanvas;

import com.affymetrix.genoviz.bioviews.ExponentialTransform;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.bioviews.SiblingCoordAvoid;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.RubberBand;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.DragMonitor;

import com.affymetrix.genoviz.event.*;

import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.RootGlyph;
import javax.swing.JScrollBar;

/**
 * NeoMap is the <strong>implementation</strong> of NeoMap.
 *
 * <p> Documentation for all interface methods can be found in the
 * documentation for NeoMap.<p>
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
 *
 * @version $Id$
 */
public class NeoMap extends NeoWidget implements
	   NeoDragListener, NeoViewBoxListener, NeoRubberBandListener, ComponentListener {
	/**
	 * For methods inherited from NeoWidgetI that require a sub-component id.
	 * For NeoMapI the component <em>is</em> the only sub-component,
	 * and its id is MAP.
	 */
	public static final int MAP = 400;

		   protected int orient;

		   protected MapGlyphFactory default_factory;

		   int stretchCount, reshapeCount;
		   protected boolean fit_check = true;

		   protected boolean use_border_layout = false;

		   private static final boolean DEBUG_STRETCH = false;
		   private static final boolean DEBUG_RESHAPE = false;
		   private static final boolean debug = false;

		   private static final boolean NM_DEBUG_COLORS = false;
		   private static final boolean NM_DEBUG_PAINT = false;

		   protected static Color default_map_background = new Color(180, 250, 250);
		   protected static Color default_panel_background = Color.lightGray;
		   // not sure if foreground is used at all at the moment...
		   protected static Color default_panel_foreground = Color.darkGray;

		   // a vector of axes added to the map
		   // this is maintained in order to stretch them
		   // when the range coords of the map change.
		   private Vector<AxisGlyph> axes = new Vector<AxisGlyph>();

		   // fields for map glyph factories
		   // a hashtable to map name strings to MapGlyphFactories
		   // TODO: Use MapGlyphFactoryI ?
		   Hashtable<String,MapGlyphFactory> factory_hash;

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

		   protected int selectionMethod = NO_SELECTION;
		   protected Vector<NeoViewBoxListener> viewbox_listeners = new Vector<NeoViewBoxListener>();
		   protected Vector<NeoRangeListener> range_listeners = new Vector<NeoRangeListener>();

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
			*  <li> selected Vector
			*  <li> axes Vector
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
			   this(hscroll_show, vscroll_show, HORIZONTAL,
					   new LinearTransform());
		   }

		   /**
			* Creates a horizontal map.
			*
			* @param hscroll_show determines whether or not to show a horizontal scrollbar.
			* @param vscroll_show determines whether or not to show a vertical scrollbar.
			* @param hscroll_location determines where to show the horizontal scrollbar.
			* @param vscroll_location determines where to show the vertical scrollbar.
			*/
		   /*public NeoMap(boolean hscroll_show, boolean vscroll_show, String hscroll_location, String vscroll_location) {
			 this(hscroll_show, vscroll_show, HORIZONTAL,
			 new LinearTransform(), hscroll_location, vscroll_location);
			 }*/

		   /**
			* creates a horizontal or vertical map with scrollbars.
			*
			* @param orient must be {@link #HORIZONTAL} or {@link #VERTICAL}.
			*/
		   /*
			  public NeoMap(int orient) {
			  this(true, true, orient, new LinearTransform());
			  }*/

		   /**
			* constructs a map with the given configuration.
			* If scroll bars are requested, they are put in their default locations.
			*
			* @param hscroll_show determines whether or not to show a horizontal scrollbar.
			* @param vscroll_show determines whether or not to show a vertical scrollbar.
			* @param orient must be {@link #HORIZONTAL} or {@link #VERTICAL}.
			* @param tr LinearTransform for zooming.
			*/
		   public NeoMap(boolean hscroll_show, boolean vscroll_show,
				   int orient, LinearTransform tr) {
			   // use default hscroll_loc and vscroll_loc
			   this(hscroll_show, vscroll_show, orient, tr, hscroll_default_loc, vscroll_default_loc);
		   }

		   /**
			* constructs a map with the given configuration.
			*
			* @param hscroll_show determines whether or not to show a horizontal scrollbar
			* @param vscroll_show determines whether or not to show a vertical scrollbar
			* @param orient must be {@link #HORIZONTAL} or {@link #VERTICAL}.
			* @param tr LinearTransform for zooming
			* @param hscroll_location can be "North", otherwise "South" is assumed.
			* @param vscroll_location can be "West", otherwise "East" is assumed.
			*/
		   private NeoMap(boolean hscroll_showb, boolean vscroll_show,
				   int orient, LinearTransform tr, String hscroll_location, String vscroll_location) {
			   super();
			   this.hscroll_show = hscroll_showb;
			   this.vscroll_show = vscroll_show;
			   this.hscroll_loc = hscroll_location;
			   this.vscroll_loc = vscroll_location;
			   this.orient = orient;

			   this.trans = tr;

			   scene = new Scene();
			   canvas = new NeoCanvas();
			   enableDragScrolling(drag_scrolling_enabled);

			   default_factory = new MapGlyphFactory(orient);
			   default_factory.setScene(scene);

			     // GAH 2/25/2009
			     //  switched to using JScrollBar
			     //  previous problems with Swing JScrollBar (and I think AWT Scrollbar as well)
			     //      seem to have been resolved as of JDK 1.5
			     setRangeScroller(new JScrollBar(JScrollBar.HORIZONTAL));
			     setOffsetScroller(new JScrollBar(JScrollBar.VERTICAL));

			   zoomer[X] = null;
			   zoomer[Y] = null;
			   scale_constraint[X] = NONE;
			   scale_constraint[Y] = NONE;
			   zoom_behavior[X] = CONSTRAIN_MIDDLE;
			   zoom_behavior[Y] = CONSTRAIN_MIDDLE;
			   zoom_coord[X] = 0;
			   zoom_coord[Y] = 0;

			   setMapRange(0,100);
			   setMapOffset(0,100);

			   view = new View(scene);
			   scene.addView(view);
			   view.setComponent(canvas);
			   view.setTransform(trans);

			   setPixelBounds();

			   Toolkit tkit = Toolkit.getDefaultToolkit();
			   seqmetrics = tkit.getFontMetrics(font_for_max_zoom);
			   max_pixels_per_coord[X] = seqmetrics.charWidth('C');

			   max_pixels_per_coord[Y] = 10;
			   min_pixels_per_coord[X] = min_pixels_per_coord[Y] = 0.01f;

			   initComponentLayout();

			   /*
				* checking for whether these scrollbars are used
				* (should really default to AUTO_SCROLL_INCREMENT anyway
				*  and reset in widgets that don't want it [like NeoSeq] )
				*/
			   if (hscroll_show && scroller[X] instanceof Component)  {
				   setScrollIncrementBehavior(X, AUTO_SCROLL_INCREMENT);
			   }

			   if (vscroll_show && scroller[Y] instanceof Component)  {
				   setScrollIncrementBehavior(Y, AUTO_SCROLL_INCREMENT);
			   }

			   factory_hash = new Hashtable<String,MapGlyphFactory>();
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
			* In the case of the base NeoMap, the NeoCanvas and (optionally) scroller components.
			* This has been separated out from constructor
			* to allow for subclasses to more easily change layout.
			*/
		   public void initComponentLayout() {
			   if (use_border_layout) {
				   this.setLayout(new BorderLayout());
				   if (hscroll_show && scroller[X] instanceof Component)  {
					   add(hscroll_loc, (Component)scroller[X]);
				   }
				   if (vscroll_show && scroller[Y] instanceof Component)  {
					   add(vscroll_loc, (Component)scroller[Y]);
				   }
				   add("Center", canvas);
			   }

			   else {
				   GridBagLayout gbl = new GridBagLayout();
				   GridBagConstraints gbc = new GridBagConstraints();
				   this.setLayout(gbl);
				   if (hscroll_show && scroller[X] instanceof Component)  {
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
					   gbl.setConstraints((Component)scroller[X], gbc);
					   add((Component)scroller[X]);
				   }
				   if (vscroll_show && scroller[Y] instanceof Component)  {
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
					   gbl.setConstraints((Component)scroller[Y], gbc);
					   add((Component)scroller[Y]);
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
			* so that we can clear the rubber band and its listeners.
			*/
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
			   public void reshape(int x, int y, int width, int height) {
				   reshapeCount++;
				   if (width < 1) width = 1;
				   if (height < 1) height = 1;
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
					  and scroller reshape() and repaint(), but that seems more acceptable
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

				   // force NeoCanvas to nullify offscreen image, to ensure Image retrieval
				   // doesn't try to return stale Image
				   if (canvas.isDoubleBuffered()) {
					   canvas.nullOffscreenImage();
				   }
				   if (view.isBuffered()) {
					   // do same thing with view???
				   }
			   }
		   }


		   /**
			* setMapRange and setMapOffset are the only places that the map coord box
			*  and coord_* should change.
			*/
		   public void setMapRange(int start, int end) {
			   // scene.setCoords() is now handled in setBounds()

			   if (orient == VERTICAL) {
				   //      this.setBounds(Y,start,end);
				   this.setFloatBounds(Y,(double)start,(double)end);
			   }
			   else {
				   //      this.setBounds(X,start,end);
				   this.setFloatBounds(X,(double)start,(double)end);
			   }

			   if (axes != null) {
				   for (int i=0; i<axes.size(); i++) {
					   axes.elementAt(i).rangeChanged(); // notify the axis of the range change.
				   }
			   }

		   }

		   public int[] getMapRange() {
			   int[] range = new int[2];
			   Rectangle2D.Double cb = getCoordBounds();
			   if (orient == VERTICAL) {
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
			   if (orient == VERTICAL) {
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
			* setMapRange and setMapOffset are the only places that the map coord box
			*  and coord_* should change.
			*/
		   public void setMapOffset(int start, int end) {
			   // scene.setCoords() is now handled in setBounds()
			   if (orient == VERTICAL) {
				   this.setBounds(X, start, end);
			   }
			   else  {
				   this.setBounds(Y,start,end);
			   }
		   }

		   public int[] getMapOffset() {
			   int[] range = new int[2];
			   Rectangle2D.Double cb = getCoordBounds();
			   if (orient == VERTICAL) {
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
			   Rectangle2D.Double cb = getViewBounds();
			   if (orient == VERTICAL) {
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
			   stretchToFit((reshape_constraint[X] == FITWIDGET),
					   (reshape_constraint[Y] == FITWIDGET));
		   }

		   /**
			*  xfit and yfit override reshape_constraint[X] and reshape_constraint[Y]
			*/
		   public void stretchToFit(boolean xfit, boolean yfit) {
			   stretchCount++;
			   if (DEBUG_STRETCH)  {
				   System.out.println("in NeoMap.stretchToFit(" + xfit + ", " + yfit + ")");
				   System.out.println(canvas.isVisible() + ", " + canvas.isShowing() +
						   ", " + canvas);
			   }
			   scene.maxDamage();  // max out scene damage to ensure full redraw
			   trans = (LinearTransform)view.getTransform();
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

			   if (!set_min_pix_per_coord[X]) {
				   min_pixels_per_coord[X] = trans.getScaleX();
			   }
			   if (!set_min_pix_per_coord[Y]) {
				   min_pixels_per_coord[Y] = trans.getScaleY();
			   }

			   // checking in case map is small, stretchToFit may build a transform with
			   //   scale larger than max_pixels_per_coord
			   if (min_pixels_per_coord[X] >= max_pixels_per_coord[X]) {
				   min_pixels_per_coord[X] = max_pixels_per_coord[X];
				   trans.setScaleX(min_pixels_per_coord[X]);
				   trans.setOffsetX(canvas.getSize().width/2 -
						   trans.getScaleX()*scene.getCoordBox().width/2);
			   }
			   if (min_pixels_per_coord[Y] >= max_pixels_per_coord[Y]) {
				   min_pixels_per_coord[Y] = max_pixels_per_coord[Y];
				   trans.setScaleY(min_pixels_per_coord[Y]);
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
				   Rectangle2D.Double viewbox, scenebox;
				   scenebox = scene.getCoordBox();
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

			   pixels_per_coord[X] = trans.getScaleX();
			   coords_per_pixel[X] = 1/pixels_per_coord[X];

			   pixels_per_coord[Y] = trans.getScaleY();
			   coords_per_pixel[Y] = 1/pixels_per_coord[Y];

			   if (zoomer[X] != null)  {
				   // setting maxy of exponential tranform to (max - visible amount) to
				   // compensate for the fact that in JDK1.1 and Swing Scrollbars,
				   // the maximum for the value is really the scrollbar maximum minus
				   // the visible amount (the thumb)
				   zoomtrans[X] = new ExponentialTransform
					   (min_pixels_per_coord[X],
						max_pixels_per_coord[X],
						zoomer[X].getMinimum(),
						zoomer[X].getMaximum()-zoomer[X].getVisibleAmount());

				   adjustZoomer(X);
			   }
			   adjustScroller(X);
			   if (zoomer[Y] != null) {
				   // setting maxy of exponential tranform to (max - visible amount) to
				   // compensate for the fact that in JDK1.1 and Swing Scrollbars,
				   // the maximum for the value is really the scrollbar maximum minus
				   // the visible amount (the thumb)
				   zoomtrans[Y] = new ExponentialTransform
					   (min_pixels_per_coord[Y],
						max_pixels_per_coord[Y],
						zoomer[Y].getMinimum(),
						// zoomer[Y].getMaximum() );
					   zoomer[Y].getMaximum()-zoomer[Y].getVisibleAmount());
				   adjustZoomer(Y);
			   }
			   adjustScroller(Y);

			   if (DEBUG_STRETCH)  {
				   System.out.println("leaving NeoMap.stretchToFit(): " + stretchCount);
			   }

		   }

		   public LinearTransform calcFittedTransform() {
			   LinearTransform new_trans = new LinearTransform();
			   new_trans.fit(scene.getCoordBox(), view.getPixelBox());
			   return new_trans;
		   }

		   public AxisGlyph addAxis(int offset) {
			   AxisGlyph axis = null;
			   if (orient == VERTICAL) {
				   axis = new AxisGlyph(VERTICAL);
				   axis.setCoords(offset-10, scene.getCoordBox().y, 20,
						   scene.getCoordBox().height);
			   }
			   else {
				   axis = new AxisGlyph();
				   axis.setCoords(scene.getCoordBox().x, offset-10,
						   scene.getCoordBox().width, 20);
			   }
			   axis.setForegroundColor(Color.black);
			   scene.getGlyph().addChild(axis);
			   axes.addElement(axis);
			   return axis;
		   }


		   public void setRangeZoomer(Adjustable adj) {
			   setZoomer(X, adj);
		   }

		   public void setOffsetZoomer(Adjustable adj) {
			   setZoomer(Y, adj);
		   }

		   public void scrollOffset(double value) {
			   scroll(Y, value);
		   }

		   public void scrollRange(double value) {
			   scroll(X, value);
		   }


		   public void zoomRange(double zoom_scale) {
			   zoom(X, zoom_scale);
		   }

		   public void zoomOffset(double zoom_scale) {
			   zoom(Y, zoom_scale);
		   }

		   public MapGlyphFactory addFactory(String config_string) {
			   Hashtable<String,Object> config_hash = null;
			   config_hash = GeneralUtils.parseOptions(config_string);
			   return addFactory(config_hash);
		   }

		   public MapGlyphFactory addFactory(Hashtable<String,Object> config_hash) {

			   MapGlyphFactory fac = new MapGlyphFactory(orient);
			   fac.setScene(scene);
			   fac.configure(config_hash);

			   // allowing specification of factory name via -name option
			   //  this use of factory is DEPRECATED though
			   String name = (String)config_hash.get("-name");
			   if (name != null) {
				   factory_hash.put(name, fac);
			   }
			   return fac;
		   }

		   /**
			* Add a factory made glyph to the map.
			* Note that this assumes adding based on sequence!
			* So that it will <em>include</em> the end.
			* Thus if start = 0 and end = 1,
			* we are really creating an annotation
			* that starts at 0 and is 2 map units long.
			*/
		   public GlyphI addItem(MapGlyphFactory factory, int start, int end) {
			   String nullstring = null;
			   return addItem(factory, start, end, nullstring);
		   }

		   public GlyphI addItem(MapGlyphFactory fac, int start, int end,
				   String option_string) {
			   if (fac == null) {
				   throw new NullPointerException("factory cannot be null.");
			   }
			   GlyphI gl;
			   if (start <= end) {
				   if (option_string == null) {
					   gl = fac.makeGlyph((double)start, (double)(end+1));
				   }
				   else {
					   gl = fac.makeGlyph((double)start, (double)(end+1), option_string);
				   }
			   }
			   else {
				   if (option_string == null) {
					   gl = fac.makeGlyph((double)(start+1), (double)end);
				   }
				   else {
					   gl = fac.makeGlyph((double)(start+1), (double)end, option_string);
				   }
			   }
			   scene.addGlyph(gl);
			   return gl;
		   }

		   public GlyphI addItem(int start, int end) {

			   GlyphI g = null;
			   if (start <= end) {
				   g = default_factory.makeItem(start, end+1);
			   }
			   else {
				   g = default_factory.makeItem(start+1, end);
			   }
			   return g;
		   }

		   public GlyphI addItem(int start, int end, String options){
			   if (start <= end) {
				   return default_factory.makeItem(start, end+1, options);
			   }
			   else {
				   return default_factory.makeItem(start+1, end, options);
			   }
		   }

		   //
		   // Packing is not performed here!!!
		   // It is, for now, a separate operation.
		   //
		   /**
			* Simply calls parent.addChild(child), so we don't really need this method.
			* @return null
			*/
		   public GlyphI addItem(GlyphI parent, GlyphI child) {
			   parent.addChild(child);
			   return null;
		   }

		   public Object addToItem(Object obj, int start, int end) {
			   if (!(obj instanceof GlyphI)) {
				   return null;
			   }
			   GlyphI parent = (GlyphI)obj;
			   MapGlyphFactory factory = default_factory;
			   PackerI packer = factory.getPacker();
			   factory.setPacker(null);
			   GlyphI child = factory.makeGlyph(start, end);
			   parent.addChild(child);
			   factory.setPacker(packer);
			   return child;
		   }

		   /**
			* setRangeScroller() and setOffsetScroller() should probably be combined
			* with setZoomer() to have a more general
			* setAdjustable(int id, Adjustable adj) method. 
			*/
		   public void setRangeScroller(Adjustable nscroll) {
			   setScroller(X, nscroll);
		   }

		   public void setOffsetScroller(Adjustable nscroll) {
			   setScroller(Y, nscroll);
		   }

		   public Adjustable getScroller(int id) {
			   //  public NeoAdjustable getScroller(int id) {
			   return scroller[id];
			   }

		   /**
			* @param id should be {@link #X} or {@link #Y}.
			* @return the Adjustable responsible for zooming in the <var>id</var> direction.
			*/
		   public Adjustable getZoomer(int id) {
			   return zoomer[id];
		   }

		   public void removeItem(GlyphI gl) {
			   scene.removeGlyph(gl);
			   glyph_hash.remove(gl);

			   selected.removeElement(gl);

			   Object model = gl.getInfo();
			   if (model != null) {
				   Object item2= model_hash.get(model);
				   if (item2 == gl) {
					   model_hash.remove(model);
				   }
				   else if (item2 instanceof Vector) {
					   ((Vector)item2).removeElement(gl);
				   }
			   }

		   }

		   public void removeItem(Vector<GlyphI> vec) {
			   /*
				* Remove from end of child Vector instead of beginning! -- that way, won't
				* get issues with trying to access elements off end of Vector as
				* Vector shrinks during removal, if Vector is actually one of map/glyph/etc.
				* internal Vectors
				*/
			   int count = vec.size();
			   for (int i=count-1; i>=0; i--) {
				   GlyphI g = vec.elementAt(i);
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
		   public void clearWidget() {
			   super.clearWidget();
			   // create new eveGlyph, set its coords and expansion behavior to old eveGlyph
			   RootGlyph oldeve = (RootGlyph)scene.getGlyph();
			   Rectangle2D.Double evebox = oldeve.getCoordBox();
			   RootGlyph neweve = new RootGlyph();
			   neweve.setExpansionBehavior(neweve.X, oldeve.getExpansionBehavior(oldeve.X));
			   neweve.setExpansionBehavior(neweve.Y, oldeve.getExpansionBehavior(oldeve.Y));
			   neweve.setCoords(evebox.x, evebox.y, evebox.width, evebox.height);
			   scene.setGlyph(neweve);

			   // reset glyph_hash
			   glyph_hash = new Hashtable<GlyphI,Object>();

			   // reset model_hash
			   model_hash = new Hashtable<Object,Object>();

			   // reset axes
			   axes.removeAllElements();

			   // remove all the transient glyphs.
			   scene.removeAllTransients();

			   // let the listeners know.
			   fireNeoWidgetEvent( new NeoWidgetEvent( this, 0 ) );

		   }


		   public void setSelectionEvent(int theMethod) {
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
		   public int getSelectionEvent() {
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

			   /* Ajusting canvas and view to enable glyph optimizations!
				  for View-controlled glyph optimizations to work, need to turn turn off
				  both double buffering and opacity of NeoCanvas, and turn on
				  double buffering in View.

				  Alternatively, could leave on canvas double-buffering (and opacity),
				  and still turn on double buffering in view.  This will effectively
				  result in a triple-buffering, with the view drawing to its own buffer,
				  then copying the image into the canvas' offscreen buffer, and then
				  the canvas copying that buffer onto the screen.  However, this is
				  somewhat slower since it requires an extra image copy

				  only implementing first alternative for now
				  */
			   if (optimize_damage || optimize_scrolling || optimize_transients) {
				   canvas.setDoubleBuffered(false);
				   canvas.setOpaque(false);
				   view.setBuffered(true);
			   }
			   else {
				   canvas.setDoubleBuffered(true);
				   canvas.setOpaque(true);
				   view.setBuffered(false);
			   }
			   view.setDamageOptimized(optimize_damage);
			   view.setScrollingOptimized(optimize_scrolling);
			   // view will automatically attempt transient optimizations if transient
			   //   glyphs are present in the scene
		   }

		   public void select(GlyphI gl, int start, int end) {
			   if (end < start) {
				   int temp = start;
				   start = end;
				   end = temp;
			   }
			   scene.select(gl, (double)start, gl.getCoordBox().y,
					   (double)end-start, gl.getCoordBox().height);

			   if ( gl.isSelected() ) { // Selection was not suppressed by an unselectable glyph!
				   if (!(selected.contains(gl))) {
					   selected.addElement(gl);
				   }
			   }
		   }

		   public void select(Vector glyphs, int start, int end) {
			   for (int i=0; i<glyphs.size(); i++) {
				   select((GlyphI)glyphs.elementAt(i), start, end);
			   }
		   }

		   public int getSelectedStart(GlyphI gl) {
			   if (orient == VERTICAL)  {
				   return (int)Math.round(gl.getSelectedRegion().y);
			   }
			   else {
				   return (int)Math.round(gl.getSelectedRegion().x);
			   }
		   }

		   public int getSelectedEnd(GlyphI gl) {
			   if (orient == VERTICAL) {
				   return (int)Math.round(gl.getSelectedRegion().y +
						   gl.getSelectedRegion().height - 1);
			   }
			   else {
				   return (int)Math.round(gl.getSelectedRegion().x +
						   gl.getSelectedRegion().width - 1);
			   }
		   }

		   public void update(Graphics g) {
			   if (NM_DEBUG_PAINT)  {
				   System.out.println("NeoMap.update() called");
			   }
			   paint(g);
		   }

		   public void repaint() {
			   if (NM_DEBUG_PAINT)  {
				   System.out.println("NeoMap.repaint() called");
			   }
			   super.repaint();
			   if (NM_DEBUG_PAINT)  {
			   }
		   }


		   public void paint(Graphics g) {
			   if (NM_DEBUG_PAINT) {
				   System.out.println("NeoMap.paint() called");
			   }
			   super.paint(g);
			   if (NM_DEBUG_PAINT)  {
			   }
		   }


		   public void setMapColor(Color col) {
			   canvas.setBackground(col);
		   }

		   public Color getMapColor() {
			   return canvas.getBackground();
		   }

		   public void setBackground(Color theColor) {
			   super.setBackground(theColor);
			   setBackground(MAP, theColor);
		   }

		   /**
			* @param id must be NeoMap.MAP
			* @param col the color for the map background.
			* @see NeoMap#MAP
			*/
		   public void setBackground(int id, Color col) {
			   if (id == MAP) {
				   setMapColor(col);
			   }
			   else {
				   throw new IllegalArgumentException("NeoMap.setBackground() can only " +
						   " accept an id of MAP");
			   }
		   }

		   /**
			* @param id must be NeoMap.MAP
			* @return the color of the map background.
			* @see NeoMap#MAP
			*/
		   public Color getBackground(int id) {
			   if (id == MAP) {
				   return getMapColor();
			   }
			   else {
				   throw new IllegalArgumentException("NeoMap.getBackground() can only " +
						   " accept an id of MAP");
			   }
		   }

		   public PackerI getPacker() {
			   return this.getScene().getGlyph().getPacker();
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
		   public void addItem(GlyphI gl) {
			   if (gl != null) {
				   Scene glyph_scene = gl.getScene();
				   if (glyph_scene == null) {
					   scene.addGlyph(gl);
				   }
				   else {
					   throw new IllegalArgumentException("must remove item from previous " +
							   "map before adding it to new map");
				   }
			   }
		   }

		   public void toFront(GlyphI gl) {
			   scene.toFront(gl);
		   }

		   public void toBack(GlyphI gl) {
			   scene.toBack(gl);
		   }

		   // This assumes root glyph has been assigned a packer...
		   public void repack() {
			   scene.maxDamage();
			   RootGlyph rglyph = (RootGlyph)scene.getGlyph();
			   rglyph.pack(getView());
		   };

		   public void setPacker(PackerI packer) {
			   this.getScene().getGlyph().setPacker(packer);
			   //    ((MapScene)this.getScene()).setPacker(packer);
			   default_factory.setPacker(packer);
		   }


		   /**
			* Configure map by setting given options.
			* @param options   An option String of the form "-option1 value1
			*    -option2 value2..."
			*/
		   public void configure( String options ) {
			   default_factory.configure(options);
		   }

		   /**
			* Configure map by setting given options.
			* @param options   An option Hashtable of the form<BR>
			* {"option1" ==&gt; "value1",<BR>
			*  "option2" ==&gt; "value2", ...}<BR>
			*/
		   public void configure(Hashtable<String,Object> options) {
			   default_factory.configure(options);
		   }

		   public MapGlyphFactory getFactory() {
			   return default_factory;
		   }

		   /**
			* Set max zoom to exact width of font.
			*/
		   public void setMaxZoomToFont(Font fnt) {
			   font_for_max_zoom = fnt;
			   Toolkit tkit = Toolkit.getDefaultToolkit();
			   seqmetrics = tkit.getFontMetrics(font_for_max_zoom);
			   int font_width = seqmetrics.charWidth('C');
			   setMaxZoom(X, font_width);
		   }

		   /**
			* Gets the offscreen Image buffer associated with the map.
			* <em>Just</em> the map is returned, does not include scrollbars...
			* This can then be used to generate GIFs of the map.
			*/
		   public Image getMapImage() {
			   Image mi;
			   if (view.isBuffered()) {
				   mi = view.getBufferedImage();
				   return mi;
			   }
			   else if (canvas.isDoubleBuffered()) {
				   mi = canvas.getBufferedImage();
				   if (mi == null) {
					   // trying to deal with initialization problem --
					   // some JVMs call paint() directly when bringing up
					   // canvas for first time, and since double buffer image is dealt with
					   // in update(), it stays null till a call to update() is made.
					   // Therefore if getting a null image, trying to force an
					   // update->paint to create and draw onto the image buffer
					   // This works, at least in Cafe 1.8 on Win95 -- GAH 8-26-98
					   // System.err.println("trying to jump start image");
					   // Note that this is somewhat inefficient, forcing a double draw.
					   // However, this conditional should only trigger when an Image is
					   // requested after a NeoMap first appears, or after reshapes, but
					   // before any updateWidgets() occur
					   canvas.update(canvas.getGraphics());
					   mi = canvas.getBufferedImage();
				   }
				   return mi;
			   }
			   else { // no image
				   return null;
			   }
		   }

		   /**
			* Listens for NeoDragEvents generated by a {@link DragMonitor}.
			* The DragMonitor, in turn, is listening to the canvas for mouse events
			* and generating appropriately timed NeoDragEvents.
			* This is used to implement drag scrolling.
			* @see #enableDragScrolling(boolean)
			*/
		   public void heardDragEvent(NeoDragEvent evt) {
			   if (!drag_scrolling_enabled) { return; }
			   Object src = evt.getSource();
			   int direction = evt.getDirection();
			   if (src == canvas_drag_monitor) {

				   Rectangle2D.Double vbox = getViewBounds();
				   String str;
				   double scroll_to_coord;
				   int pixels_per_scroll = 10;
				   if (direction == NORTH) {
					   scroll_to_coord =
						   trans.inverseTransform(LinearTransform.Y, -pixels_per_scroll);
					   scroll(Y, scroll_to_coord);
					   updateWidget();
				   }
				   else if (direction == SOUTH) {
					   scroll_to_coord =
						   trans.inverseTransform(LinearTransform.Y, pixels_per_scroll);
					   scroll(Y, scroll_to_coord);
					   updateWidget();
				   }
				   else if (direction == EAST) {
					   scroll_to_coord =
						   trans.inverseTransform(LinearTransform.X, pixels_per_scroll);
					   scroll(X, scroll_to_coord);
					   updateWidget();
					   str = "EAST";
				   }
				   else if (direction == WEST) {
					   scroll_to_coord =
						   trans.inverseTransform(LinearTransform.X, -pixels_per_scroll);
					   scroll(X, scroll_to_coord);
					   updateWidget();
					   str = "WEST";
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
		   public void heardMouseEvent(MouseEvent e) {
			   if (! (e instanceof NeoViewMouseEvent)) { return; }
			   NeoViewMouseEvent nme = (NeoViewMouseEvent)e;
			   Object source = nme.getSource();
			   int id = nme.getID();
			   // else if (NO_SELECTION != selectionMethod && evt.target == this.scene) {
			   if (NO_SELECTION != selectionMethod && source == this.view) {
				   boolean shiftDown = nme.isShiftDown();
				   boolean controlDown = nme.isControlDown();
				   boolean metaDown = nme.isMetaDown();
				   //      boolean altDown = nme.isAltDown();
				   if ((id == NeoMouseEvent.MOUSE_PRESSED &&
							   ON_MOUSE_DOWN == selectionMethod) ||
						   (id == NeoMouseEvent.MOUSE_RELEASED &&
							ON_MOUSE_UP == selectionMethod)) {
					   Vector prev_items = this.getSelected();
					   int prev_items_size = prev_items.size();
					   if (prev_items_size > 0 &&  !(shiftDown || controlDown || metaDown)) {
						   this.deselect(prev_items);
					   }
					   Vector<GlyphI> candidates = this.getItems(nme.getCoordX(), nme.getCoordY());
					   if (candidates.size() > 0 && (shiftDown || controlDown)) {

						   Vector<GlyphI> in = new Vector<GlyphI>(), out = new Vector<GlyphI>();
						   for (GlyphI obj : candidates) {
							   if (prev_items.contains(obj)) {
								   out.addElement(obj);
							   }
							   else {
								   in.addElement(obj);
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

				   for (int i=0; i<viewbox_listeners.size(); i++) {
					   viewbox_listeners.elementAt(i).viewBoxChanged(vevt);
				   }
			   }
			   if (range_listeners.size() > 0) {
				   Rectangle2D.Double vbox = e.getCoordBox();
				   NeoRangeEvent nevt = null;
				   if (orient == VERTICAL) {
					   nevt = new NeoRangeEvent(this, vbox.y, vbox.y + vbox.height);
				   }
				   else {
					   nevt = new NeoRangeEvent(this, vbox.x, vbox.x + vbox.width);
				   }

				   for (int i=0; i<range_listeners.size(); i++) {
					   range_listeners.elementAt(i).rangeChanged(nevt);
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
				   for (int i=0; i<rubberband_listeners.size(); i++) {
					   rubberband_listeners.elementAt(i).rubberBandChanged(nevt);
				   }
			   }
		   }

		   public void addViewBoxListener(NeoViewBoxListener l) {
			   if (!viewbox_listeners.contains(l)) {
				   viewbox_listeners.addElement(l);
			   }
		   }

		   public void removeViewBoxListener(NeoViewBoxListener l) {
			   viewbox_listeners.removeElement(l);
		   }

		   public void addRangeListener(NeoRangeListener l) {
			   if (!range_listeners.contains(l)) {
				   range_listeners.addElement(l);
			   }
		   }

		   public void removeRangeListener(NeoRangeListener l) {
			   range_listeners.removeElement(l);
		   }


		   private NeoWidgetListener listeners = null;
		   public void addWidgetListener( NeoWidgetListener l ) {
			   listeners = NeoEventMulticaster.add( listeners, l );
		   }
		   public void removeWidgetListener( NeoWidgetListener l ) {
			   listeners = NeoEventMulticaster.remove( listeners, l );
		   }
		   /**
			* notify all the listeners that an event has occurred.
			*/
		   protected void fireNeoWidgetEvent( NeoWidgetEvent e ) {
			   if ( null != listeners ) {
				   listeners.widgetCleared( e );
			   }
		   }

		   public Hashtable getModelMapping() {
			   return model_hash;
		   }

		   }
