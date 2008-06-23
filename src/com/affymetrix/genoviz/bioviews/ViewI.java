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

import com.affymetrix.genoviz.awt.NeoCanvas;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A ViewI is an abstract window onto a particular {@link SceneI}.
 * The view maintains a mapping (with the help of {@link TransformI})
 * between the scene's coordinate space
 * and the pixel space of a particular AWT component.
 * Among other things
 * the view is responsible for initiating the drawing of {@link GlyphI} objects,
 * and for managing the transformation of coordinates to pixels and
 * vice versa to allow glyphs to draw themselves
 *
 * <p> ViewI, along with SceneI and GlyphI, is one of the three fundamental interfaces
 * comprising Affymetrix' inner 2D structured graphics architecture.
 */
public interface ViewI  {

  public Rectangle getScratchPixBox();

  /**
   * Set the Graphics that this view should draw on.
   */
  public void setGraphics(Graphics2D g);

  /**
   * Get the Graphics that this view should draw on.
   */
  public Graphics2D getGraphics();

  /**
   * Draw this view of a scene onto the view's component.
   */
  public void draw();

  /**
   * Returns the SceneI that the view is of.
   */
  public SceneI getScene();

  /**
   * Sets the {@link NeoCanvas} component that the view draws to.
   */
  public void setComponent(NeoCanvas c);

  /**
   *  Returns the component that the view draws to.
   */
  public NeoCanvas getComponent();

  public Rectangle getComponentSizeRect();


  /// Scene gives View a coord box (e.g. range of base pairs).
  /// Scene gives View a transform between pixel & coord space.
  /// Scene gives View a pixel box (e.g. the whole canvas).
  /**
   * Sets the pixel box that bounds the view on the component the view 
   * draws to.
   * This is typically a rectangle with the dimensions of the component
   * (0, 0, component.size().width, component.size().height)
   */
  //TODO: Maybe force the pixel box to match the dimensions of the component.
  // It should be easy to nest components to achieve the same thing that this allows.
  public void setPixelBox(Rectangle rect);

  /**
   * Gets the pixel box that bounds the view on the component the view draws to.
   * This is typically the dimensions of the component
   * (0, 0, size().width, size().height)
   */
  public Rectangle getPixelBox();

  /**
   *  Sets the coordinate box that bounds the view, in other words the
   *  portion of the scene that is visible within this view.
   */
  //TODO: document whether this is the WHOLE coord box, or just the
  // coords that are visible in the current zoom.
  public void setCoordBox(Rectangle2D.Double coordbox);

  /**
   *  Returns the coordinate box that bounds the view, in other words the
   *  portion of the scene that is visible within this view.
   */
  public Rectangle2D.Double getCoordBox();

  
  /**
   * If this View is a sub-view of a portion of a larger view,
   * use this to set a reference to the full view.
   * This is used, for example, when a single NeoMap is being drawn as
   * multiple sub-views on several different monitors or graphics cards.
   * If this is not a sub-view, then the full view equals this view itself.
   * @param full
   */
  public void setFullView(ViewI full);

  /**
   * If this View is a sub-view of a portion of a larger view,
   * use this to set a reference to the full view.
   * This is used, for example, when a single NeoMap is being drawn as
   * multiple sub-views on several different monitors or graphics cards.
   * If this is not a sub-view, then the full view equals this view itself.
   * @return The full view, which in most cases is this same object itself.
   */
  public ViewI getFullView();

  /**
   *  Sets the {@link TransformI} that is used to transform widget coordinates 
   *  to pixels and vice versa (via an inverse transform).
   */
  public void setTransform(TransformI t);

  /**
   *  Returns the {@link TransformI} that is used to transform widget coordinates to
   *  pixels and vice versa (via an inverse transform).
   */
  public TransformI getTransform();

  /**
   *    Transforms src rectangle in coordinate space
   *    to dst rectangle in pixel (screen) space.
   * 
   *   <p>The view is responsible for mapping coordinates to pixels and
   *    vice versa, via transformToPixels() and transformToCoords().
   * 
   *  <p>The view is the only object that knows about the mapping from
   *  coordinate spaces to pixel space, hence
   *  transformToPixels() and transformToCoords().
   *
   *  @param src the coordinates in coordinate space (double values)
   *  @param dst the coordinates in pixel space (32-bit integer values)
   * 
   *    @return altered destination Rectangle
   */
  public Rectangle transformToPixels(Rectangle2D.Double src, Rectangle dst);

  /**
   *    Transforms src rectangle in pixel (screen) space
   *    to dst rectangle in coord space.
   * 
   *   <p>The view is responsible for mapping coordinates to pixels and
   *    vice versa, via transformToPixels() and transformToCoords().
   * 
   *  <p>The view is the only object that knows about the mapping from
   *  coordinate spaces to pixel space, hence
   *  transformToPixels() and transformToCoords().
   * 
   *  @param src the coordinates in pixel space (32-bit integer values)
   *  @param dst the coordinates in coordinate space (double values)
   *
   *    @return altered destination Rectangle2D
   */
  public Rectangle2D.Double transformToCoords(Rectangle src, Rectangle2D.Double dst);

  /**
   *
   * Transforms src Point2D in coordinate space to dst Point in pixel
   *    (screen) space.
   *  Returns altered destination Point
   */
  public Point transformToPixels(Point2D.Double src, Point dst);

  /**
   *    Transforms src Point in pixel (screen) space to dst Point2D in
   *    coord space.
   *
   *    Returns alterred destination Point2D
   */
  public Point2D transformToCoords(Point src, Point2D.Double dst);
}
