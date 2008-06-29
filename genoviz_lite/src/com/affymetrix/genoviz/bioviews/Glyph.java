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
import java.util.*;
import java.util.List;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.genoviz.glyph.GlyphStyle;
import com.affymetrix.genoviz.glyph.GlyphStyleFactory;
import java.awt.geom.Rectangle2D;

/**
 * The original base classes that implements the GlyphI interface. 
 * Most older glyphs are subclasses of Glyph.  
 * A more memory-efficient base class can be found in 
 * {@link com.affymetrix.genoviz.glypn.efficient.EffGlyph}.
 * See {@link GlyphI} for better documentation of methods.
 * Though it has no drawn appearance of its own, Glyph can also act as an
 * invisible container for other child glyphs.
 */

//TODO: Does GlyphI need to know about which Scene it is in?
// or should it ask the View?

public abstract class Glyph implements GlyphI {

  public static enum DrawOrder {
    DrawSelfFirst, DrawChildrenFirst
  }
  
  public boolean DEBUG_DRAW = false;
  private static final boolean debug = false;
  private static final boolean DEBUG_DT = false;
  protected static final Color default_bg_color = Color.black;
  protected static final Color default_fg_color = Color.black;
  protected static GlyphStyleFactory stylefactory = new GlyphStyleFactory(); // might want to set default colors;

  protected Rectangle2D.Double coordbox;
  private Rectangle2D.Double cb2 = null; // used as a temporary variable

  protected Rectangle pixelbox;
  protected int min_pixels_width = 1; //TODO: make part of the style
  protected int min_pixels_height = 1;
  protected GlyphI parent;
  protected List<GlyphI> children;

  protected GlyphStyle style;
  protected boolean isVisible;
  protected Object info;
  protected PackerI packer;
  protected int styleIndex;
  protected boolean selected;
  protected DrawOrder drawOrder = DrawOrder.DrawSelfFirst;

  public Glyph() {
    coordbox = new Rectangle2D.Double();
    pixelbox = new Rectangle();
    min_pixels_width = 1;
    min_pixels_height = 1;
    isVisible = true;

    style = stylefactory.getStyle(default_fg_color, default_bg_color);
  }

  @Override
  public boolean withinView(ViewI view) {
    return getPositiveCoordBox().intersects(view.getCoordBox());
  }

  /**
   * Selecting a region of a glyph.
   * This base class defaults to selecting the whole glyph.
   * Subclasses can override this for a more appropriate implementation.
   *
   * @param x ignored
   * @param y ignored
   * @param width ignored
   * @param height ignored
   */
  @Override
  public void select(double x, double y, double width, double height) {
    setSelected(true);
  }

  /**
   *  Default is that glyph does not support subselection.
   *  Override this to indicate support for subselection.
   */
  @Override
  public boolean supportsSubSelection() {
    return false;
  }

  /**
   *  Default implementation returns bounding box for the
   *  entire glyph
   */
  @Override
  public Rectangle2D.Double getSelectedRegion() {
    if (selected) {
      return getPositiveCoordBox();
    } else {
      return null;
    }
  }

  public void setDrawOrder(DrawOrder order) {
    drawOrder = order;
  }

  public DrawOrder getDrawOrder() {
    return drawOrder;
  }

  @Override
  public void drawTraversal(ViewI view) {
    if (DEBUG_DT) {
      System.err.println("called Glyph.drawTraversal() on " + this);
    }
    if (drawOrder == DrawOrder.DrawSelfFirst) {
      if (withinView(view) && isVisible) {
        if (selected) {
          drawSelected(view);
        } else {
          draw(view);
        }
        if (children != null) {
          drawChildren(view);
        }
      }
    } else if (drawOrder == DrawOrder.DrawChildrenFirst) {
      if (withinView(view) && isVisible) {
        if (children != null) {
          drawChildren(view);
        }
        if (selected) {
          drawSelected(view);
        } else {
          draw(view);
        }
      }
    }
    if (DEBUG_DT) {
      System.err.println("leaving Glyph.drawTraversal()");
    }
  }

  protected void drawChildren(ViewI view) {
    if (children != null) {
      GlyphI child;
      int numChildren = children.size();
      for (int i = 0; i < numChildren; i++) {
        child = children.get(i);
        // TransientGlyphs are usually NOT drawn in standard drawTraversal
        if (!(child instanceof TransientGlyph) || drawTransients()) {
          child.drawTraversal(view);
        }
      }
    }
  }

  /**
   * Default implementation does nothing.
   * The glyph would be invisible, but it could still have children
   * that are drawn.
   * @param view
   */
  @Override
  public void draw(ViewI view) {
    if (debug) {
      Graphics2D g = view.getGraphics();
      g.setColor(Color.red);
      view.transformToPixels(coordbox, pixelbox);
      g.drawRect(pixelbox.x + 1, pixelbox.y + 1,
              pixelbox.width - 2, pixelbox.height - 2);
    }
  }

  /**
   * Draws the glyph in the appropriate manner
   * to indicate that it has been selected.
   * That style is specified by 
   * {@link com.affymetrix.genoviz.bioviews.SceneI#getSelectionAppearance()},
   */
  public void drawSelected(ViewI view) {

    switch (view.getScene().getSelectionAppearance()) {
      case SELECT_OUTLINE:
        drawSelectedOutline(view);
        break;
      case SELECT_FILL:
        drawSelectedFill(view);
        break;
      case BACKGROUND_FILL:
        drawSelectedBackground(view);
        break;
      case SELECT_NONE:
        draw(view);
        break;
      case SELECT_REVERSE:
        drawSelectedReverse(view);
        break;
    }
  }

  protected void drawSelectedBackground(ViewI view) {
    Graphics2D g = view.getGraphics();
    g.setColor(view.getScene().getSelectionColor());
    view.transformToPixels(getPositiveCoordBox(), pixelbox);
    g.fillRect(pixelbox.x - 3, pixelbox.y - 3,
            pixelbox.width + 6, pixelbox.height + 6);
    draw(view);
  }

  protected void drawSelectedOutline(ViewI view) {
    draw(view);
    Graphics2D g = view.getGraphics();
    g.setColor(view.getScene().getSelectionColor());
    view.transformToPixels(getPositiveCoordBox(), pixelbox);
    g.drawRect(pixelbox.x - 2, pixelbox.y - 2,
            pixelbox.width + 3, pixelbox.height + 3);
  }

  protected void drawSelectedFill(ViewI view) {
    final Color tempcolor = getBackgroundColor();
    setBackgroundColor(view.getScene().getSelectionColor());
    draw(view);
    setBackgroundColor(tempcolor);
  }

  protected void drawSelectedReverse(ViewI view) {
    final Color bg = getBackgroundColor();
    final Color fg = getForegroundColor();
    this.setBackgroundColor(fg);
    this.setForegroundColor(bg);
    this.draw(view);
    this.setBackgroundColor(bg);
    this.setForegroundColor(fg);
  }

  @Override
  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> picks,
          ViewI view) {
    if (isVisible && intersects(pickRect, view)) {
      if (hit(pickRect, view)) {
        if (!picks.contains(this)) {
          picks.add(this);
        }
      }
      if (children != null) {
        GlyphI child;
        int childnum = children.size();
        for (int i = 0; i < childnum; i++) {
          child = children.get(i);
          child.pickTraversal(pickRect, picks, view);
        }
      }
    }
  }


  /* By the time hit detection, packing, etc. calls
   * a <code>Glyph.pickTraversal</code> method,
   * any pixelboxes have been converted
   * to coordboxes and the call is
   * to <code>pickTraversal(<em>coordbox</em>, vec, view)</code>
   */
  @Override
  public void pickTraversal(Rectangle pickRect, List<GlyphI> picks,
          ViewI view) {
    if (isVisible && intersects(pickRect, view)) {
      if (hit(pickRect, view)) {
        if (!picks.contains(this)) {
          picks.add(this);
        }
      }
      if (children != null) {
        GlyphI child;
        int childnum = children.size();
        for (int i = 0; i < childnum; i++) {
          child = children.get(i);
          child.pickTraversal(pickRect, picks, view);
        }
      }
    }
  }


  /**
   * Detects whether or not this glyph is "hit"
   * by a rectangle of pixel space within a view.
   *
   *<p> Note that this base implementation always returns false.
   *    This is because container glyphs must return false.
   *    They can intersect other rectangles.
   *    But, they cannot be "hit".
   *    Glyphs that extend this class and are not container glyphs
   *    should override this method.
   *
   * @param pixel_hitbox ignored
   * @param view ignored
   * @return false
   */
  public boolean hit(Rectangle pixel_hitbox, ViewI view) {
    return false;
  }

  /**
   * Detects whether or not this glyph is "hit"
   * by a rectangle of coordinate space within a view.
   *
   *<p> Note that this base implementation always returns false.
   *    This is because container glyphs must return false.
   *    They can intersect other rectangles.
   *    But, they cannot be "hit".
   *    Glyphs that extend this class and are not container glyphs
   *    should override this method.
   *
   * @param coord_hitbox ignored
   * @param view ignored
   * @return false
   */
  @Override
  public boolean hit(java.awt.geom.Rectangle2D.Double coord_hitbox, ViewI view) {
    return false;
  }

  /** Default implementation of method from GlyphI, always returns false
   *  unless overridden in sub-class.
   */
  @Override
  public boolean isHitable() {
    return false;
  }

  /**
   * Returns whether or not this glyph is visible and intersects the rectangle.
   * @param rect  rectangle in pixels
   * @return true if this glyph is both visible and intersects the rectangle
   */
  public boolean intersects(Rectangle rect) {
    return isVisible && rect.intersects(pixelbox);
  }

  /**
   * Returns whether or not this glyph is visible and intersects the rectangle.
   * @param rect  rectangle in coordinate space
   * @return true if this glyph is both visible and intersects the rectangle
   */
  //TODO: Why is the view argument there?
  @Override
  public boolean intersects(Rectangle2D.Double rect, ViewI view) {
    return isVisible && rect.intersects(getPositiveCoordBox());
  }

  protected boolean intersects(Rectangle rect, ViewI view) {
    return isVisible && rect.intersects(pixelbox);
  }

  /**
   * Returns whether or not this glyph is visible and the point is inside it (in pixel space).
   * @param x pixel value
   * @param y pixel value
   * @return true if this glyph is both visible and the pixelbox contains (x,y)
   */
  public boolean inside(int x, int y) {
    return isVisible && this.pixelbox.contains(x, y);
  }

  /**
   *  Adds a child glyph.
   *  Because the pickTraversal() method calls itself
   *  recursively on its children, a glyph cannot be a
   *  child of itself.
   *  Note that this will also call {@link GlyphI#setParent(com.affymetrix.genoviz.bioviews.GlyphI)} 
   *  on the child.
   * @param glyph child
   * @param position location in child list
   *  @throws IllegalArgumentException if you try to add a glyph as a child
   *    of itself.
   */
  @Override
  public void addChild(GlyphI glyph, int position) {
    if (this == glyph) {
      throw new IllegalArgumentException("Illegal to add a Glyph as a child of itself!");
    }
    GlyphI prev_parent = glyph.getParent();
    if (prev_parent != null) {
      prev_parent.removeChild(glyph);
    }
    if (children == null) {
      children = new ArrayList<GlyphI>();
    }
    if (position == children.size()) {
      children.add(glyph);
    } else {
      children.add(position, glyph);
    }
    glyph.setParent(this);
  }

  /** Adds the child to this object's list of children.
   *  Note:  there is nothing preventing you from
   *  adding the same child multiple times, although
   *  that would probably be a bad thing to do.
   */
  @Override
  public void addChild(GlyphI glyph) {
    GlyphI prev_parent = glyph.getParent();
    if (prev_parent != null) {
      prev_parent.removeChild(glyph);
    }
    if (children == null) {
      children = new ArrayList<GlyphI>();
    }
    children.add(glyph);
    glyph.setParent(this);
  }

  /** Removes the child from this object's list of children,
   *  and sets its parent to null (for improved garbage collection).
   *  Note:  if the same child was added multiple times,
   *  this will only remove one of the references to it and
   *  will not set the parent to null.
   *  Probably {@link #addChild(GlyphI)} should be re-written
   *  to disallow that in the first place.
   */
  @Override
  public void removeChild(GlyphI glyph) {
    if (children != null) {
      children.remove(glyph);
      if (children.size() == 0) {
        children = null;
      }
    }
    // null out the scene if glyph is removed
    //glyph.setScene(null);
  }

  @Override
  public void removeAllChildren() {
//    if (children != null) {
//      for (int i = 0; i < children.size(); i++) {
//        children.get(i).setScene(null);
//      }
//    }
    children = null;
  }

  @Override
  public int getChildCount() {
    if (children == null) {
      return 0;
    } else {
      return children.size();
    }
  }

  @Override
  public GlyphI getChild(int index) {
    return children.get(index);
  }

  @Override
  public List<GlyphI> getChildren() {
    return children;
  }

  @Override
  public void setParent(GlyphI glyph) {
    parent = glyph;
//    if (glyph != null) {
//      setScene(glyph.getScene());
//    } else {
//      setScene(null);
//    }
  }

  @Override
  public GlyphI getParent() {
    return parent;
  }

  public void calcPixels(ViewI view) {
    pixelbox = view.transformToPixels(coordbox, pixelbox);
  }

  public Rectangle getPixelBox() {
    return pixelbox;
  }

  @Override
  public Rectangle getPixelBox(ViewI view) {
    pixelbox = view.transformToPixels(coordbox, pixelbox);
    return pixelbox;
  }

  /** Sets the minimum size in pixels. If d.width or d.height is negative,
      this uses their absolute value instead. */
  @Override
  public void setMinimumPixelBounds(Dimension d) {
    // to save a miniscule amount of memory per Glyph, this is saved as
    // two integers rather than one Dimension object.
    //TODO: save in the style object instead
    min_pixels_width = Math.abs(d.width);
    min_pixels_height = Math.abs(d.height);
  }

  /**
   * Sets the coordinates of the Glyph.
   * This will convert rectangles of a negative width and/or height
   * to an equivalent rectangle with positive width and height.
   * @throws IllegalArgumentException if width or height is negative
   */
  @Override
  public void setCoords(double x, double y, double width, double height) {
    if (width < 0 || Double.isNaN(width) || Double.isInfinite(width)) {
      throw new IllegalArgumentException("Width cannot be negative: " + width);
//      x = x + width;
//      width = -width;
    }
    if (height < 0 || Double.isNaN(height) || Double.isInfinite(height)) {
      throw new IllegalArgumentException("Height cannot be negative: " + height);
//      y = y + height;
//      height = -height;
    }
    coordbox.setRect(x, y, width, height);
  }

  @Override
  public Rectangle2D.Double getCoordBox() {
    return coordbox;
  }

  /** Returns the coordbox,
   *  but converts rectangles with negative width or height
   *  to an equivalent one with positive width and height.
   */
  protected final Rectangle2D.Double getPositiveCoordBox() {
    if (coordbox.width >= 0 && coordbox.height >= 0) {
      return coordbox;
    }
    else {
//      if (coordbox.width < 0 || Double.isNaN(coordbox.width)) {
//        coordbox.width = 10;
//      }
//      if (coordbox.height < 0 || Double.isNaN(coordbox.height)) {
//        coordbox.height = 10;
//      }
//      return coordbox;
      throw new RuntimeException("Glyph has non-positive width or height" + coordbox);
    }
  }


  /**
   * Replaces the coord box with the given object.
   * Any later changes to that coordbox object will be 
   * reflected in this glyph.
   * (You will be responsible for making sure those coordinates
   * are never set to negative width or heicht.)
   * @see #setCoords
   */
  @Override
  public void setCoordBox(Rectangle2D.Double coordbox) {
    this.coordbox = coordbox;
  }

  @Override
  public void setForegroundColor(Color color) {
    this.style = stylefactory.getStyle(color, style.getBackgroundColor(), style.getFont());
  }

  @Override
  public Color getForegroundColor() {
    return this.style.getForegroundColor();
  }

  @Override
  public void setBackgroundColor(Color color) {
    this.style = stylefactory.getStyle(style.getForegroundColor(), color, style.getFont());
  }

  @Override
  public Color getBackgroundColor() {
    return this.style.getBackgroundColor();
  }

  /** Semi-deprecated. Use {@link #setBackgroundColor(Color)}. */
  @Override
  public void setColor(Color color) {
    this.setBackgroundColor(color);
  }

  /** Semi-deprecated. Use {@link #getBackgroundColor}. */
  @Override
  public Color getColor() {
    return this.getBackgroundColor();
  }

  public void setFont(Font f) {
    this.style = stylefactory.getStyle(style.getForegroundColor(), style.getBackgroundColor(), f);
  }

  public Font getFont() {
    return this.style.getFont();
  }

  @Override
  public void setInfo(Object info) {
    this.info = info;
  }

  @Override
  public Object getInfo() {
    return info;
  }

  @Override
  public void setVisibility(boolean isVisible) {
    this.isVisible = isVisible;
  }

  @Override
  public boolean isVisible() {
    return isVisible;
  }

  @Override
  public void setPacker(PackerI packer) {
    this.packer = packer;
  }

  @Override
  public PackerI getPacker() {
    return packer;
  }

  @Override
  public void pack(ViewI view) {
    if (packer == null) {
      return;
    }
    packer.pack(this, view);
  }

  @Override
  public void moveRelative(double diffx, double diffy) {
    coordbox.x += diffx;
    coordbox.y += diffy;
    if (children != null) {
      int numchildren = children.size();
      for (int i = 0; i < numchildren; i++) {
        children.get(i).moveRelative(diffx, diffy);
      }
    }
  }

  @Override
  public void moveAbsolute(double x, double y) {
    double diffx = x - coordbox.x;
    double diffy = y - coordbox.y;
    this.moveRelative(diffx, diffy);
  }

//  public void setScene(SceneII s) {
//    scene = s;
//    if (children != null) {
//      int size = children.size();
//      for (int i = 0; i < size; i++) {
//        children.get(i).setScene(s);
//      }
//    }
//  }
//
//  public SceneII getScene() {
//    return scene;
//  }

  protected boolean selectable = true;

  /**
   * Sets the selectability of the glyph.
   *
   * @param selectability
   */
  @Override
  public void setSelectable(boolean selectability) {
    if (!selectability) {
      setSelected(false);
    }
    this.selectable = selectability;
  }

  /**
   * Indicates whether or not the glyph can be selected.
   */
  @Override
  public boolean isSelectable() {
    return this.selectable;
  }

  /**
   * Selects the glyph if it is selectable.
   * If it is not then this does nothing.
   *
   * @param selected true if the glyph is to be selected,
   * false otherwise.
   */
  @Override
  public void setSelected(boolean selected) {
    if (this.selectable) {
      this.selected = selected;
    }
  }

  /**
   * Indicates whether or not the glyph has been selected.
   */
  @Override
  public final boolean isSelected() {
    return selected;
  }

  /**
   * Returns false.  Subclases can override.
   * @return false
   */
  public boolean drawTransients() {
    return false;
  }

//  /**
//   *  Set trans to global transform for this glyph.
//   *  (Based on getChildTransform() of parent.)
//   */
//  public boolean getGlobalTransform(ViewI view, LinearTransform trans) {
//    trans.copyTransform((LinearTransform) view.getTransform());
//    return getParent().getGlobalChildTransform(view, trans);
//  }

  /** Default implementation does nothing. */
  @Override
  public void getChildTransform(ViewI view, LinearTransform trans) {
    return;
  }

//  public boolean getGlobalChildTransform(ViewI view, LinearTransform trans) {
//    Stack<GlyphI> glstack = new Stack<GlyphI>();
//    GlyphI rootgl = ((SceneII) view.getScene()).getRootGlyph(); //TODO: unchecked cast
//    GlyphI gl = this;
//    glstack.push(gl);
//    while (gl != rootgl) {
//      gl = gl.getParent();
//      // if get a null parent before getting root glyph, then fail and return
//      if (parent == null) {
//        return false;
//      }
//      glstack.push(gl);
//    }
//    trans.copyTransform((LinearTransform) view.getTransform());
//    while (!(glstack.empty())) {
//      gl = glstack.pop();
//      gl.getChildTransform(view, trans);
//    }
//    return true;
//  }
}
