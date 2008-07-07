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

package com.affymetrix.genoviz.bioviews;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.awt.NeoCanvas;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of ViewI interface.
 * See ViewI for better documentation of methods.
 */
public class View implements ViewI, NeoPaintListener,
  MouseListener, MouseMotionListener, KeyListener  {

  Rectangle scratch_pixelbox = new Rectangle();

  // if DEBUG_SPILLOVER then no clipRect call when drawing view,
  //    so can see any spillover outside of view, especially in combination
  //    with DEBUG_SHOW_ONLY_NEW_DRAW
  private static final boolean DEBUG_SPILLOVER = false;

  // if DEBUG_VIEWBOX, print out viewbox data in [top-level] draw()
  private static final boolean DEBUG_VIEWBOX = false;

  // if DEBUG_DAMAGE, draw green/yellow outlines around region of damage
  //  propogation.  This also forces full draw rather than damage-optimized
  //  draw
  private static final boolean DEBUG_DAMAGE = false;

  // if DEBUG_BACKGROUND, background of redrawn area will cycle through
  //    colors with each new draw
  private static final boolean DEBUG_BACKGROUND = false;
  private int debug_cycle = 0;
  private Color[] debug_color =
  { Color.red, Color.yellow, Color.white, Color.green };

  // if DEBUG_SCROLL_CHECKS, then if optimizedScrollDraw() fails and have
  // to resort to a normalDraw(), will print out which scrolling optimzation
  // condition was not met
  private static final boolean DEBUG_SCROLL_CHECKS = false;

  // if DEBUG_TRANSIENTS, will output comments on whether redraw involved
  // drawing anything but transients, or was really a bitblit of previously
  // buffered image and a direct screen draw of transients
  private static final boolean DEBUG_TRANSIENTS = false;

  // if DEBUG_EVENTS, will output comments concerning hearing and
  // broadcasting of MOUSE_DOWN events
  private static final boolean DEBUG_EVENTS = false;

  // if DB_NCE, will output comments concerning hearing and
  // broadcasting of NeoCanvasEvents
  private static final boolean DB_NCE = false;

  private static final boolean DEBUG_OPTSCROLL = false;

  private static final boolean optscroll_checkNotFirst = true;
  private static final boolean optscroll_checkLinTrans = true;
  private static final boolean optscroll_checkOneChange = true;
  private static final boolean optscroll_checkNoScaleChange = true;
  private static final boolean optscroll_checkIntScale = true;
  private static final boolean optscroll_checkNoDamage = true;

  protected Rectangle pixelbox;

  // GAH 2006-03-28  for experimental multiscreen support
  // View may actually be a "subview" (for example when trying to farm single NeoMap out to multiple
  //    NeoMap children, or during scroll optimizations), in which case glyphs may want
  //    to know bounds, etc. of full virtual "parent" view.
  // If this is not a subviwe, then full_view = this view
  protected ViewI full_view = null;

  protected SceneII scene;
  protected TwoDimTransform transform;

  protected NeoCanvas component;

  protected Rectangle2D.Double coordbox;
  protected Graphics2D graphics;
  protected boolean isTimed = false;
  protected com.affymetrix.genoviz.util.Timer timecheck;
  protected List<MouseListener> mouse_listeners = new CopyOnWriteArrayList<MouseListener>();
  protected List<MouseMotionListener> mouse_motion_listeners = new CopyOnWriteArrayList<MouseMotionListener>();
  protected List<KeyListener> key_listeners = new CopyOnWriteArrayList<KeyListener>();

  /**
   *  List of {@link NeoViewBoxListener}s to be notified immediately <em>before</em>
   *    view is drawn with changed bounding box
   */
  protected java.util.List<NeoViewBoxListener> predraw_viewbox_listeners = new CopyOnWriteArrayList<NeoViewBoxListener>();

  /**
   *  List of {@link NeoViewBoxListener}s to be notified immediately <em>after</em>
   *    view is drawn with changed bounding box
   */
  protected java.util.List<NeoViewBoxListener> viewbox_listeners = new CopyOnWriteArrayList<NeoViewBoxListener>();

  /** Fields to help with optimizations **/
  protected boolean scrolling_optimized = false;
  protected boolean damage_optimized = false;

  protected TwoDimTransform lastTransform;  // copy of transform from last draw
  // I think prevCalcCoordBox can be replaced with prevCoordBox -- GAH 8/4/97
  protected Rectangle2D.Double prevCalcCoordBox;
  protected Rectangle2D.Double prevCoordBox;

  protected boolean firstScrollOptimizedDraw = true;
  protected boolean firstDamageOptimizedDraw = true;
  protected boolean firstBufferOptimizedDraw = true;
  protected Rectangle damagePixelBox = new Rectangle();

  protected Dimension component_size;
  protected Rectangle component_bounds;
  protected Rectangle component_size_rect;
  protected int drawCount = 0;

  /*
      Temporary variables that aren't visible to the outside world.
      These are to store temporary stuff, for instance to have a
      destination Rectangle2D when doing transformations that actually
      map to pixel space.
   */
  protected Rectangle2D.Double scratch_coords;
  protected Point2D.Double scratch_coord;

  protected Rectangle scene_pixelbox;
  protected java.awt.geom.Rectangle2D.Double scene_coordbox;

  public View()  {
    full_view = this;
    // transforms initialized to Identity transform
    transform = new LinearTwoDimTransform();
    timecheck = new com.affymetrix.genoviz.util.Timer();
    pixelbox = new Rectangle();

    setCoordBox(new Rectangle2D.Double());

    prevCoordBox = new Rectangle2D.Double();
    prevCalcCoordBox = new Rectangle2D.Double();
    scratch_coords = new Rectangle2D.Double();
    scratch_coord = new Point2D.Double(0,0);
    scene_pixelbox = new Rectangle();
  }

  public View(SceneII scene)  {
    this();
    this.scene = scene;
  }

  public void destroy() {
    mouse_listeners.clear();
    mouse_motion_listeners.clear();
    key_listeners.clear();
    predraw_viewbox_listeners.clear();
    viewbox_listeners.clear();
    graphics = null;
    component = null;
  }

  public boolean isScrollingOptimized() { return scrolling_optimized; }
  public void setScrollingOptimized(boolean optimize) {
    if (optimize != scrolling_optimized) {
      firstScrollOptimizedDraw = true;
      scrolling_optimized = optimize;
    }
  }

  public boolean isDamageOptimized() { return damage_optimized; }
  public void setDamageOptimized(boolean optimize) {
    if (optimize != damage_optimized) {
      firstDamageOptimizedDraw = true;
      damage_optimized = optimize;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTransform (TwoDimTransform transform)  {
    this.transform = transform;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TwoDimTransform getTransform ()  {
    return this.transform;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Rectangle transformToPixels(Rectangle2D.Double src, Rectangle dst)  {
    transform.transform(src, scratch_coords);

    dst.x = (int)scratch_coords.x;
    dst.y = (int)scratch_coords.y;
    dst.width = (int)(scratch_coords.x+scratch_coords.width)-dst.x;
    dst.height = (int)(scratch_coords.y+scratch_coords.height)-dst.y;

    return dst;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point transformToPixels(Point2D.Double src, Point dst)  {
    transform.transform(src, scratch_coord);
    dst.x = (int)scratch_coord.x;
    dst.y = (int)scratch_coord.y;
    return dst;
  }

//  public boolean clipToPixelBox(Rectangle src, Rectangle dst) {
//    int sx2, sy2, vx2, vy2;
//    sx2 = dst.x + dst.width;
//    sy2 = dst.y + dst.height;
//    vx2 = pixelbox.x + pixelbox.width;
//    vy2 = pixelbox.y + pixelbox.height;
//    dst.x = src.x<pixelbox.x ? pixelbox.x : src.x;
//    dst.y = src.y<pixelbox.y ? pixelbox.y : src.y;
//    dst.width = sx2>vx2 ? vx2-dst.x : sx2-dst.x;
//    dst.height = sy2>vy2 ? vy2-dst.y : sy2-dst.y;
//    return true;
//  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Rectangle2D.Double transformToCoords(Rectangle src, Rectangle2D.Double dst)  {
    scratch_coords.x = (double)src.x;
    scratch_coords.y = (double)src.y;
    scratch_coords.width = (double)src.width;
    scratch_coords.height = (double)src.height;
    //    System.out.println("calling inverseTransform: " + transform);
    java.awt.geom.Rectangle2D.Double result = transform.inverseTransform(scratch_coords, dst);
    //    System.out.println("done calling inverseTransform");
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point2D transformToCoords(Point src, Point2D.Double dst)  {
    scratch_coord.x = (double)src.x;
    scratch_coord.y = (double)src.y;
    return transform.inverseTransform(scratch_coord, dst);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void draw()  {

    // public synchronized void draw()  {
    boolean drawn = false;
    //    System.out.println("scale = " + ((LinearTwoDimTransform)transform).getScaleX());
    drawCount++;
    if (isTimed) {
      timecheck.start();
    }
    component_size = component.getSize();
    component_size_rect = new Rectangle(component_size);

    component_bounds = component.getBounds();

    // calculate pixel bounding box of scene;
    scene_coordbox = scene.getCoordBox();
    scene_pixelbox = transformToPixels(scene_coordbox, scene_pixelbox);

    if (graphics == null) {
      getGraphics();
    }

    if (DEBUG_VIEWBOX) {
      transformToCoords(pixelbox, getCoordBox());
      System.out.println("Scale: " +
          ((LinearTwoDimTransform)transform).getScaleX() + ", " +
          getCoordBox() + ", " + getPixelBox());
    }


    calcCoordBox();

    if (!(coordbox.equals(prevCoordBox))) {
      // need to change this to a more general ViewBoxChange event...
      if (predraw_viewbox_listeners.size() > 0) {
        final Rectangle2D.Double newbox = new Rectangle2D.Double(
          coordbox.x, coordbox.y,
          coordbox.width, coordbox.height);
        final NeoViewBoxChangeEvent nevt =
          new NeoViewBoxChangeEvent(this, newbox, true);
        for (NeoViewBoxListener listener : predraw_viewbox_listeners) {
          listener.viewBoxChanged(nevt);
        }
      }
    }


    // optimizedScrollDraw checks for damage, will return false and not
    // draw if there is any damage.  optimizedDamageDraw will not be called
    // if optimizedScrollDraw() is called.  Therefore will never call both
    // at the same time.
    if (!drawn && isScrollingOptimized() && lastTransform != null) {
      drawn = optimizedScrollDraw();
      //      System.out.println("called optimizedScrollDraw(), drawn = " + drawn);
    }
    if (!drawn && isDamageOptimized()) {
      drawn = optimizedDamageDraw();
      // System.out.println("called optimizedDamageDraw(), drawn = " + drawn);
    }
    if (!drawn) {
      normalDraw();
    }

    /*
     *  Drawing "transients" directly to screen.
     */
    if (scene.hasTransients()) {
      for (TransientGlyph transglyph : scene.getTransients()) {
        if (DEBUG_TRANSIENTS)  {
          System.out.println("Drawing transient: " + transglyph);
        }
        transglyph.drawTraversal(this);
      }
    }

    try {
      lastTransform = transform.clone();
    }
    catch (CloneNotSupportedException e) {
      // This can never happen as long as TwoDimTransform extends Cloneable.
    }
    if (isTimed) {
      timecheck.print();
    }

    // WARNING!! Calling scene.clearDamage() here means that damage
    //   optimizations will most likely get screwed up when using
    //   multiple views of same scene! -- GAH 12/31/97
    scene.clearDamage();

    calcCoordBox();
    //    transformToCoords(pixelbox, coordbox);

    
    /*
     *  If view's coordbox has changed (meaning the view has "moved" to
     *  display a different region of the scene), then post a
     *  ViewBoxChangeEvent to any listeners
     */
    if (!(coordbox.equals(prevCoordBox))) {
      // need to change this to a more general ViewBoxChange event...
      if (viewbox_listeners.size() > 0) {
        final Rectangle2D.Double newbox = new Rectangle2D.Double(coordbox.x, coordbox.y,
            coordbox.width, coordbox.height);
        final NeoViewBoxChangeEvent nevt =
          new NeoViewBoxChangeEvent(this, newbox, false);

        for (final NeoViewBoxListener n : viewbox_listeners) {
          n.viewBoxChanged(nevt);
        }
      }
    }

    prevCoordBox.setRect(coordbox.x, coordbox.y,
        coordbox.width, coordbox.height);
  }

  public boolean optimizedBufferDraw() {
    if (firstBufferOptimizedDraw) {
      firstBufferOptimizedDraw = false;
      return false;
    }
    if (!transform.equals(lastTransform)) {
      // System.out.println("Transform changed, doing normal draw");
      return false;
    }
    // If no damage, can just return true
    //    and draw() method will move old image over from view's image buffer
    if (scene.getDamageCoordBox() == null) {
      if (DEBUG_TRANSIENTS) {
        System.out.println("no damage!");
      }
      return true;
    }
    else {
      if (DEBUG_TRANSIENTS) {
        System.out.println("Damage: " + scene.getDamageCoordBox());
      }
    }
    return false;
  }

  public void normalDraw() {
    transformToCoords(pixelbox, getCoordBox());

    // clipping rect to eliminate any spillover outside of viewbox
    if (!DEBUG_SPILLOVER) {
      graphics.clipRect(pixelbox.x, pixelbox.y,
          pixelbox.width, pixelbox.height);
    }

    graphics.setColor(component.getBackground());

    if (DEBUG_BACKGROUND) {
      graphics.setColor(debug_color[debug_cycle % 4]);
      debug_cycle++;
    }

    graphics.fillRect(pixelbox.x,pixelbox.y,pixelbox.width, pixelbox.height);
    graphics.setColor(component.getForeground());
    if (DEBUG_TRANSIENTS) {
      System.out.println("doing full redraw: " + pixelbox +
          ", " + coordbox);
    }
    scene.getRootGlyph().drawTraversal((ViewI)this);
  }

  protected boolean optimizedDamageDraw()  {
    // 1. Make sure that:
    //    a. not first draw after turning on damage optimization
    //    b. transforms haven't changed
    if (firstDamageOptimizedDraw) {
      firstDamageOptimizedDraw = false;
      return false;
    }
    if (!transform.equals(lastTransform)) {
      return false;
    }

    // 2. If no damage, can just return true
    //    and draw() method will move old image over from view's image buffer
    if (scene.getDamageCoordBox() == null) {
      //      System.out.println("no damage!");
      return true;
    }

    // 3. create a new view that only includes the damaged area
    //    transformToPixels(damageCoordBox, damagePixelBox);
    transformToPixels(scene.getDamageCoordBox(), damagePixelBox);

    // add one pixel spillover on each side, just to be safe
    // plus another pixel for rounding problems... GAH 12-10-97
    damagePixelBox.x -= 2;
    damagePixelBox.y -= 2;
    damagePixelBox.width += 4;
    damagePixelBox.height += 4;

    // ignore any damage outside of the visible pixelbox
    // if no damage within view, can just return true,
    // and draw() method will move old image over from view's image buffer
    if (!(damagePixelBox.intersects(pixelbox))) {
      return true;
    }

    // only need to deal with visible damage, so find intersection of
    // view's pixelbox and damage's pixelbox
    Rectangle visibleDamagePixelBox = damagePixelBox.intersection(pixelbox);

    // switching in smaller viewbox, etc. in this view for now.  Probably
    // would be cleaner to create a new view...

    Rectangle tempPixelBox = new Rectangle(pixelbox.x, pixelbox.y,
        pixelbox.width, pixelbox.height);
    setPixelBox(visibleDamagePixelBox);
    if (DEBUG_DAMAGE) { setPixelBox(tempPixelBox); }
    normalDraw();
    if (DEBUG_DAMAGE) {
      Graphics g = getGraphics();
      g.setColor(Color.blue);
      g.drawRect(visibleDamagePixelBox.x+3, visibleDamagePixelBox.y+3,
          visibleDamagePixelBox.width-6,
          visibleDamagePixelBox.height-6);

      g.setColor(Color.green);
      g.drawRect(visibleDamagePixelBox.x, visibleDamagePixelBox.y,
          visibleDamagePixelBox.width-1,
          visibleDamagePixelBox.height-1);
      g.setColor(Color.yellow);
      g.drawRect(visibleDamagePixelBox.x-2, visibleDamagePixelBox.y-2,
          visibleDamagePixelBox.width+3,
          visibleDamagePixelBox.height+3);
    }

    setPixelBox(tempPixelBox);
    return true;
  }

  protected boolean optimizedScrollDraw()  {
    // 1. Make sure that:
    //    aa. not first draw after setting scrolling_optimize to true
    //    a. both transforms are linear
    //    b. only one translation has changed (not both X and Y)
    //    c. only the translation has changed (scales are the same)
    //    d. scale is integral (ONLY TEMPORARY, TILL TRANSLATION
    //            CONSTRAINTS ARE WORKED OUT)
    //    e. no damage in Scene since last draw

    //    aa. not first draw after setting scrolling_optimize to true
    if (optscroll_checkNotFirst && firstScrollOptimizedDraw) {
      if (DEBUG_SCROLL_CHECKS)  {
        System.out.println("First optimized draw, doing normal draw instead");
      }
      firstScrollOptimizedDraw = false;
      return false;
    }

    //    a. both transforms are linear
    if (optscroll_checkLinTrans &&
        (!(transform instanceof LinearTwoDimTransform &&
           lastTransform instanceof LinearTwoDimTransform)) ) {
      if (DEBUG_SCROLL_CHECKS)  {
        System.out.println("Not LinearTransforms, doing normal draw instead");
      }
      return false;
    }

    //    b. only one translation has changed (not both X and Y)
    LinearTwoDimTransform currTransform, prevTransform;
    currTransform = (LinearTwoDimTransform)transform;
    prevTransform = (LinearTwoDimTransform)lastTransform;
    boolean xunchanged, yunchanged;
    xunchanged =  (currTransform.getOffsetX() == prevTransform.getOffsetX());
    yunchanged =  (currTransform.getOffsetY() == prevTransform.getOffsetY());
    boolean transformChanged = (!(xunchanged && yunchanged));
    if (optscroll_checkOneChange && (!(xunchanged || yunchanged)))  {
      if (DEBUG_SCROLL_CHECKS)  {
        System.out.println("Both X and Y offsets changed, " +
            "doing normal draw instead");
      }
      return false;
    }

    //    c. only the translation has changed (scales are the same)
    boolean scaleXchange, scaleYchange;
    scaleXchange = !(currTransform.getScaleX() == prevTransform.getScaleX());
    scaleYchange = !(currTransform.getScaleY() == prevTransform.getScaleY());
    transformChanged = transformChanged || scaleXchange || scaleYchange;
    if (optscroll_checkNoScaleChange && (scaleXchange || scaleYchange)) {
      if (DEBUG_SCROLL_CHECKS)  {
        System.out.println("Scale has changed, doing normal draw instead");
      }
      return false;
    }

    //    d. scale is integral (ONLY TEMPORARY, TILL TRANSLATION
    //            CONSTRAINTS ARE WORKED OUT)

    //  GAH 12-2-97
    //  Actually, integral scale only matters if there has been a change --
    //  if _no_ change, flag it and once all conditionals have been met,
    //  if no change should just return true since buffer image should
    //  work as is
    if (transformChanged) {
      double scale;
      // this assumes that check was already done to make sure only one
      // of x or y scroll had changed -- therefore
      // if transformChanged && x-scroll not changed, then
      //          y-scroll must have changed
      if (xunchanged) {
        scale = currTransform.getScaleY();
      }
      else {
        scale = currTransform.getScaleX();
      }
      if (optscroll_checkIntScale &&
          (!(((int)scale == scale) || ((int)(1/scale) == (1/scale))))) { if (DEBUG_SCROLL_CHECKS)  {
            System.out.println("Scale not integral: " +
                scale + ", " + (1/scale) +
                " doing normal draw instead");
          }
          return false;
      }
      else if (DEBUG_SCROLL_CHECKS) {
        System.out.println("Scale -- " + scale);
      }
    }

    //    e. no damage in Scene since last draw
    if (optscroll_checkNoDamage && (scene.isDamaged())) {
      if (DEBUG_SCROLL_CHECKS)  {
        System.out.println("Scene damaged, doing normal draw instead");
      }
      return false;
    }

    //  if no change should just return true since previous buffered image
    //  should work as is
    if (!transformChanged) {
      if (DEBUG_SCROLL_CHECKS) {
        System.out.println("no transform change, using whole previous image");
      }
      return true;
    }

    if (DEBUG_SCROLL_CHECKS)  {
      System.out.println("made it through optimizedScrollDraw() checks");
    }

    // 2. calculate current coordbox, retrieve coordbox from previous paint
    //  could probably avoid this by cloning prevcoordbox from last coordbox,
    //   just like prevtransform... need to make sure cloning works though...
    // also make sure that current coord box and previous coord box
    //   overlap
    transformToCoords(pixelbox, coordbox);
    java.awt.geom.Rectangle2D.Double currCoordBox = coordbox;
    setTransform(prevTransform);
    transformToCoords(pixelbox, prevCalcCoordBox);
    prevCoordBox = prevCalcCoordBox;
    setTransform(currTransform);

    if (!(currCoordBox.intersects(prevCoordBox))) {
      return false;
    }

    // 3. find intersection of previous coord box and current coord box,
    //    and convert to pixels
    java.awt.geom.Rectangle2D.Double coordOverlap = (java.awt.geom.Rectangle2D.Double) coordbox.createIntersection(prevCoordBox);
    Rectangle prevOverlapPixBox = new Rectangle();
    Rectangle currOverlapPixBox = new Rectangle();
    setTransform(prevTransform);
    transformToPixels(coordOverlap, prevOverlapPixBox);
    setTransform(currTransform);
    transformToPixels(coordOverlap, currOverlapPixBox);

    // 4. copy (bit blt) shared pixels from previous position to
    //    new position
    Graphics g = getGraphics();

    if (DEBUG_OPTSCROLL)  {
      System.out.println("Copying " + prevOverlapPixBox);
      System.out.println("         " + currOverlapPixBox);
    }
    g.copyArea(prevOverlapPixBox.x, prevOverlapPixBox.y,
        prevOverlapPixBox.width, prevOverlapPixBox.height,
        currOverlapPixBox.x-prevOverlapPixBox.x,
        currOverlapPixBox.y-prevOverlapPixBox.y);

    // 5. revise view to be portion of new coordbox that doesn't overlap
    //    old coordbox
    java.awt.geom.Rectangle2D.Double nolCoordBox = new java.awt.geom.Rectangle2D.Double();
    // already know that either xcoords are unchanged (c.x=p.x) OR
    //                          ycoords are unchanged (c.y=p.y)
    // see notes on determining non-overlapping rectangle...
    if (yunchanged) {
      nolCoordBox.y = currCoordBox.y;
      nolCoordBox.height = currCoordBox.height;
      if (currCoordBox.x >= prevCoordBox.x) {
        nolCoordBox.x = prevCoordBox.x + prevCoordBox.width;
        nolCoordBox.width = currCoordBox.x + currCoordBox.width -
          nolCoordBox.x;
      }
      else {
        nolCoordBox.x = currCoordBox.x;
        nolCoordBox.width = prevCoordBox.x - nolCoordBox.x;
      }
    }
    else if (xunchanged) {
      nolCoordBox.x = currCoordBox.x;
      nolCoordBox.width = currCoordBox.width;
      if (currCoordBox.y >= prevCoordBox.y) {
        nolCoordBox.y = prevCoordBox.y + prevCoordBox.height;
        nolCoordBox.height = currCoordBox.y + currCoordBox.height -
          nolCoordBox.y;
      }
      else {
        nolCoordBox.y = currCoordBox.y;
        nolCoordBox.height = prevCoordBox.y - nolCoordBox.y;
      }
    }
    else {
      System.out.println("Error in ScrollOptimizer!!!");
    }

    Rectangle temppixelbox =
      new Rectangle(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    setCoordBox(nolCoordBox);
    transformToPixels(nolCoordBox, pixelbox);

    //  another attempt to get rid of "doogies"  GAH 2-20-99
    // problem appears to be caused by (int) casting in transformToPixels
    //    (instead of rounding -- though not clear if rounding would totally
    //     solve problem either) -- in particular, the area copy-shifted and
    //     the area actually redrawn are sometimes separated by a single pixel
    //     column/row, which doesn't change at all, leaving bits of previous
    //     draw in that column/row.  And once this has happened, it will be
    //     propogated in subsequent optimized scrolls, since this erroneous
    //     column/row will subsequently be treated as if it was correctly
    //     drawn, and will be bit-blitted just like everything else in
    //     when areas are copy-shifted
    //  Could try to fix this in transformToPixels, or come up with a more
    //     general solution for coord-to-pixel transforms.  However hopefully
    //     a more specific solution for optimized scrolling is to expand the
    //     edges of the area that actually gets redrawn by a pixel in every
    //     direction, thus compensating for any potential double/int "rounding"
    //     problems
    //  Could also try to change optimizedScrollDraw() so that it avoids the
    //     need to convert coords to pixels in the first place, but such a
    //     change would involve much more restructuring and testing.
    //  Therefore trying approach that requires fewest changes to existing
    //     code -- expand edges of area to be redrawn
    pixelbox.x -= 1;
    pixelbox.y -= 1;
    pixelbox.width += 2;
    pixelbox.height += 2;

    setPixelBox(pixelbox);

    // GAH 12-27-2000
    // should probably set the clip box here!
    // this would allow glyphs to not worry about drawing outside the
    //    view messing up when optimizing scrolling
    normalDraw();
    setPixelBox(temppixelbox);
    setCoordBox(currCoordBox);

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void setPixelBox(Rectangle pixelbox)  {
    this.pixelbox = pixelbox;
  }

  /** {@inheritDoc} */
  @Override
  public Rectangle getPixelBox()  {
    return pixelbox;
  }

  /** {@inheritDoc} */
  @Override
  public void setCoordBox(java.awt.geom.Rectangle2D.Double coordbox)  {
    this.coordbox = coordbox;
  }

  /** {@inheritDoc} */
  @Override
  public Rectangle2D.Double getCoordBox()  {
    return this.coordbox;
  }

  /** {@inheritDoc} */
  @Override
  public void setFullView(ViewI full_view) {
    this.full_view = full_view;
  }

  /** {@inheritDoc} */
  @Override
  public ViewI getFullView() {
    return full_view;
  }

  /** {@inheritDoc} */
  @Override
  public void setGraphics(Graphics2D g)  {
    graphics = g;
  }

  /** {@inheritDoc} */
  @Override
  public Graphics2D getGraphics()  {
    if (graphics == null && component != null)  {
      // Not sure if this is a good idea -- forcing this wreaks havoc
      //   on the updates... -- Gregg
      //TODO: revisit
      setGraphics((Graphics2D) component.getGraphics());
    }
    return graphics;
  }

  /** {@inheritDoc} */
  @Override
  public void setComponent(NeoCanvas c)  {
    component_size = c.getSize();
    component = c;
  }

  /** {@inheritDoc} */
  @Override
  public NeoCanvas getComponent()  {
    return component;
  }

  /** {@inheritDoc} */
  @Override
  public SceneI getScene() {
    return scene;
  }

  public Rectangle2D.Double calcCoordBox() {
    return transformToCoords(pixelbox, coordbox);
  }

  //  Standard methods to implement the event source for event listeners
  //TODO: should the mouse listeners be added to the component (NeoCanvas) instead of the view?
  
  public void addMouseListener(MouseListener l) {
    if (!mouse_listeners.contains(l)) {
      mouse_listeners.add(l);
    }
  }

  public void removeMouseListener(MouseListener l) {
    mouse_listeners.remove(l);
  }

  public void addMouseMotionListener(MouseMotionListener l) {
    if (!mouse_motion_listeners.contains(l)) {
      mouse_motion_listeners.add(l);
    }
  }

  public void removeMouseMotionListener(MouseMotionListener l) {
    mouse_motion_listeners.remove(l);
  }

  public void addKeyListener(KeyListener l) {
    if (!key_listeners.contains(l)) {
      key_listeners.add(l);
    }
  }

  public void removeKeyListener(KeyListener l) {
    key_listeners.remove(l);
  }

  public void addPostDrawViewListener(NeoViewBoxListener l) {
    if (!viewbox_listeners.contains(l)) {
      viewbox_listeners.add(l);
    }
  }

  public void removePostDrawViewListener(NeoViewBoxListener l) {
    viewbox_listeners.remove(l);
  }

  public void addPreDrawViewListener(NeoViewBoxListener l) {
    if (!predraw_viewbox_listeners.contains(l)) {
      predraw_viewbox_listeners.add(l);
    }
  }

  public void removePreDrawViewListener(NeoViewBoxListener l)  {
    predraw_viewbox_listeners.remove(l);
  }


  public void isTimed(boolean timed) {
    isTimed = timed;
  }

  public Dimension getComponentSize() {
    return component_size;
  }

  /**
   * gets the size of the component.
   *
   * @return a Rectangle the same size as the bounds of the component
   *         with an origin of (0, 0).
   */
  @Override
  public Rectangle getComponentSizeRect() {
    return component_size_rect;
  }

  /** implementing MouseListener interface and collecting mouse events */
  @Override
  public void mouseClicked(MouseEvent e) { heardMouseEvent(e); }
  @Override
  public void mouseEntered(MouseEvent e) { heardMouseEvent(e); }
  @Override
  public void mouseExited(MouseEvent e) { heardMouseEvent(e); }
  @Override
  public void mousePressed(MouseEvent e) { heardMouseEvent(e); }
  @Override
  public void mouseReleased(MouseEvent e) { heardMouseEvent(e); }

  /** implementing MouseMotionListener interface and collecting mouse events */
  @Override
  public void mouseDragged(MouseEvent e) { heardMouseEvent(e); }
  @Override
  public void mouseMoved(MouseEvent e) { heardMouseEvent(e); }

  /** implementing KeyListener interface and collecting key events */
  @Override
  public void keyPressed(KeyEvent e) { heardKeyEvent(e); }
  @Override
  public void keyReleased(KeyEvent e) { heardKeyEvent(e); }
  @Override
  public void keyTyped(KeyEvent e) { heardKeyEvent(e); }

  /** implementing NeoPaintListener interface and triggering draw */
  @Override
  public void componentPainted(NeoPaintEvent evt) {
    setGraphics(evt.getGraphics());
    draw();
    if (DB_NCE) {
      System.out.println("view heard NeoPaintEvent: buffered = " + component.isDoubleBuffered() +
          ", viewbox = " + coordbox);
    }
  }

  /** processing key events on view's component */
  public void heardKeyEvent(KeyEvent e) {
    if (e.getSource() != component) { return; }
    int id = e.getID();
    if (DEBUG_EVENTS && id == KeyEvent.KEY_PRESSED) {
      System.out.println("View heard KEY_PRESSED: " + this);
    }
    if (key_listeners.size() > 0) {
      for (KeyListener kl : key_listeners) {
        if (id == KeyEvent.KEY_PRESSED) {
          kl.keyPressed(e);
        }
        else if (id == KeyEvent.KEY_RELEASED) {
          kl.keyReleased(e);
        }
        else if (id == KeyEvent.KEY_TYPED) {
          kl.keyTyped(e);
        }
      }
    }
  }

  /** processing mouse events on view's component */
  public void heardMouseEvent(MouseEvent e) {
    if (e.getSource() != component) { return; }
    int id = e.getID();
    int x = e.getX();
    int y = e.getY();
    if (DEBUG_EVENTS && id == MouseEvent.MOUSE_PRESSED) {
      System.out.println("View heard MOUSE_PRESSED: " + this);
    }
    Rectangle pixrect = new Rectangle(x, y, 0, 0);
    java.awt.geom.Rectangle2D.Double coordrect = new java.awt.geom.Rectangle2D.Double();

    // Check for intersection with view's pixelbox
    // if (pixrect.intersects(getPixelBox())) {
    // commented out to allow events to be heard even if they are outside
    // the views pixelbox, for example to deal with mouse drags that
    // continue beyond the window

    if (transform == null) { return; }
    transformToCoords(pixrect, coordrect);
    NeoViewMouseEvent nevt =
      new NeoViewMouseEvent(e, this, coordrect.x, coordrect.y);
    if (mouse_listeners.size() > 0) {
      for (MouseListener ml : mouse_listeners) {
        if (id == MouseEvent.MOUSE_CLICKED) { ml.mouseClicked(nevt); }
        else if (id == MouseEvent.MOUSE_ENTERED) { ml.mouseEntered(nevt); }
        else if (id == MouseEvent.MOUSE_EXITED) { ml.mouseExited(nevt); }
        else if (id == MouseEvent.MOUSE_PRESSED) { ml.mousePressed(nevt); }
        else if (id == MouseEvent.MOUSE_RELEASED) { ml.mouseReleased(nevt); }
      }
    }
    if (mouse_motion_listeners.size() > 0) {
      for (MouseMotionListener mml : mouse_motion_listeners) {
        if (id == MouseEvent.MOUSE_DRAGGED) { mml.mouseDragged(nevt); }
        else if (id == MouseEvent.MOUSE_MOVED) { mml.mouseMoved(nevt); }
      }
    }
  }

  @Override
  public Rectangle getScratchPixBox() {
    return scratch_pixelbox;
  }
}
