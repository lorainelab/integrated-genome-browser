/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class EfficientGlyph extends Rectangle2D.Double implements com.affymetrix.genoviz.bioviews.GlyphI {
  public static final int DRAW_SELF_FIRST = 0;
  public static final int DRAW_CHILDREN_FIRST = 1;

  private static final boolean debug = false;
  private static final boolean DEBUG_DT = false;

  // If true, apply corrections to avoid an AWT drawing bug that can happen
  // for very large glyphs (bigger than about 32000 pixels).
  static final boolean FIX_AWT_BIG_RECT_BUG = true;

  static protected int min_pixels_width=1;
  static protected int min_pixels_height=1;

  protected Scene scene;

  protected GlyphI parent;
  protected List<GlyphI> children;
  protected Color color = Color.black;
  protected boolean isVisible;
  protected Object info;
  protected PackerI packer;
  protected boolean selected;
  protected int draw_order = DRAW_SELF_FIRST;

  public EfficientGlyph() {
    super();
    isVisible = true;
  }

  public boolean withinView(ViewI view) {
    return this.intersects(view.getCoordBox());
  }

  public void select(double x, double y, double width, double height) {
    setSelected(true);
  }

  public boolean supportsSubSelection() {
    return false;
  }

  public Rectangle2D.Double getSelectedRegion() {
    if (selected) { return this; }
    else { return null; }
  }

  public void setDrawOrder(int order) {
    if ((draw_order == DRAW_SELF_FIRST) ||
        (draw_order == DRAW_CHILDREN_FIRST)) {
      draw_order = order;
    }
  }

  public int getDrawOrder() {
    return draw_order;
  }

  public void drawTraversal(ViewI view)  {
    if (DEBUG_DT) {
      System.err.println("called Glyph.drawTraversal() on " + this);
    }
    if (draw_order == DRAW_SELF_FIRST) {
      if (withinView(view) && isVisible) {
        if (selected) { drawSelected(view); }
        else { draw(view); }
        if (children != null) { drawChildren(view); }
      }
    }
    else if (draw_order == DRAW_CHILDREN_FIRST) {
      if (withinView(view) && isVisible) {
        if (children != null)  { drawChildren(view); }
        if (selected) { drawSelected(view); }
        else { draw(view); }
      }
    }
    if (DEBUG_DT) {
      System.err.println("leaving Glyph.drawTraversal()");
    }
  }

  protected void drawChildren(ViewI view) {
    if (children != null)  {
      GlyphI child;
      int numChildren = getChildCount();
      for ( int i = 0; i < numChildren; i++ ) {
        child = children.get(i);
        // TransientGlyphs are usually NOT drawn in standard drawTraversal
        if (!(child instanceof TransientGlyph) || drawTransients()) {
          child.drawTraversal(view);
        }
      }
    }
  }

  public void draw(ViewI view)  {
    if (debug) {
      Graphics g = view.getGraphics();
      g.setColor(Color.red);
      Rectangle pixelbox = view.getScratchPixBox();
      view.transformToPixels(this, pixelbox);
      g.drawRect(pixelbox.x+1, pixelbox.y+1,
                 pixelbox.width-2, pixelbox.height-2);
    }
  }

  public void drawSelected(ViewI view) {
    int selection_style = view.getScene().getSelectionAppearance();
    if (selection_style == Scene.SELECT_OUTLINE) {
      drawSelectedOutline(view);
    }
    else if (selection_style == Scene.SELECT_FILL) {
      drawSelectedFill(view);
    }
    else if (selection_style == Scene.BACKGROUND_FILL) {
      // this option does not seem to exist in Scene -- tss 3/99
      drawSelectedBackground(view);
    }
    else if( selection_style == Scene.SELECT_REVERSE ) {
      drawSelectedReverse(view);
    }
    else if (selection_style == Scene.SELECT_NONE) {
      draw(view);
    }
  }

  protected void drawSelectedBackground(ViewI view) {
    Graphics g = view.getGraphics();
    Rectangle pixelbox = view.getScratchPixBox();
    g.setColor(view.getScene().getSelectionColor());
    view.transformToPixels(this, pixelbox);
    g.fillRect(pixelbox.x-3, pixelbox.y-3,
               pixelbox.width+6, pixelbox.height+6);
    draw(view);
  }

  protected void drawSelectedOutline(ViewI view) {
    draw(view);
    Graphics g = view.getGraphics();
    Rectangle pixelbox = view.getScratchPixBox();
    g.setColor(view.getScene().getSelectionColor());
    view.transformToPixels(this, pixelbox);
    g.drawRect(pixelbox.x-2, pixelbox.y-2,
               pixelbox.width+3, pixelbox.height+3);
  }

  protected void drawSelectedFill(ViewI view) {
    Color tempcolor = this.getBackgroundColor();
    this.setBackgroundColor(view.getScene().getSelectionColor());
    this.draw(view);
    this.setBackgroundColor(tempcolor);
  }

  protected void drawSelectedReverse( ViewI view ) {
    Color bg = this.getBackgroundColor();
    Color fg = this.getForegroundColor();
    this.setBackgroundColor( fg );
    this.setForegroundColor( bg );
    this.draw(view);
    this.setBackgroundColor( bg );
    this.setForegroundColor( fg );
  }

  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList, ViewI view)  {
    if (isVisible && intersects(pickRect, view))  {
      if (debug)  {
        System.out.println("intersects");
      }
      if (hit(pickRect, view))  {
        if (!pickList.contains(this)) {
          // Note that Vector.contains() performs a test using "equals()".
          // EfficientGlyph extends Rectangle2D.Double which tests equality based
          // on coordinates.  This means that you can't "select" both a parent glyph
          // and a child glyph that have identical coordinates.
          pickList.add(this);
        }
        if (debug)   {
          System.out.println("Hit " + this);
        }
      }
      if (children != null)  {
        int childnum = children.size();
        for ( int i = 0; i < childnum; i++ ) {
          GlyphI child = children.get(i);
          child.pickTraversal( pickRect, pickList, view );
        }
      }
    }
  }

  /** NOT YET IMPLEMENTED. */
  public void pickTraversal(Rectangle pickRect, List<GlyphI> pickList, ViewI view) {
    //TODO: need to covert pickRect to coords...
    /*
    if (isVisible && intersects(pickRect, view))  {
      if (debug)  {
        System.out.println("intersects");
      }
      if (hit(pickRect, view))  {
        if (!pickList.contains(this)) {
          pickList.add(this);
        }
        if (debug)   {
          System.out.println("Hit " + this);
        }
      }
      if (children != null)  {
        GlyphI child;
        // We avoid object creation overhead by avoiding Enumeration.
        int childnum = children.size();
        for (int i=0; i<childnum; i++) {
          child = (GlyphI)children.get(i);
          child.pickTraversal(pickRect, pickList, view);
        }
      }
    }
    */
  }

  public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
    return false;
  }

  public boolean intersects(Rectangle2D.Double rect, ViewI view)  {
    return isVisible && rect.intersects(this);
  }

  public boolean isHitable() { return false; }

  public void addChild(GlyphI glyph, int position) {
    GlyphI prev_parent = glyph.getParent();
    if (prev_parent != null) {
      prev_parent.removeChild(glyph);
    }
    if (children == null)  {
      children = new ArrayList<GlyphI>();
    }
    if (position == children.size()) {
      children.add(glyph);
    }
    else  {
      children.add(position, glyph);
    }
    // setParent() also calls setScene()
    glyph.setParent(this);
  }

  public void addChild(GlyphI glyph)  {
    GlyphI prev_parent = glyph.getParent();
    if (prev_parent != null) {
      prev_parent.removeChild(glyph);
    }
    if (children == null)  {
      children = new ArrayList<GlyphI>();
    }
    children.add(glyph);
    glyph.setParent(this);
  }

  public void removeChild(GlyphI glyph)  {
    if (children != null)      {
      children.remove(glyph);
      if (children.size() == 0) { children = null; }
    }
    glyph.setScene(null);
  }

  public void removeAllChildren() {
    if (children != null)  {
      for (int i=0; i<children.size(); i++) {
        children.get(i).setScene(null);
      }
    }
    children = null;
  }

  public int getChildCount() {
    if (children == null) { return 0; }
    else { return children.size(); }
  }

  public GlyphI getChild(int index) {
    return children.get(index);
  }

  public List<GlyphI> getChildren()  {
    return children;
  }

  public void setParent(GlyphI glyph)  {
    parent = glyph;
    setScene(glyph.getScene());
  }

  public GlyphI getParent()  {
    return parent;
  }

  /**
   *  Returns the pixelbox.
   *  WARNING -- inefficient if called often, since
   *     it's making a new Rectangle object with each call.  A more efficient (but more
   *     risky) approach would be to just return the view's pixelbox after transformation,
   *     with the caveat that it will only be valid until the view's pixelbox is modified.
   *  @deprecated
   */
  @Deprecated
  public Rectangle getPixelBox(ViewI view)  {
    Rectangle pixelbox = view.getScratchPixBox();
    pixelbox = view.transformToPixels (this, pixelbox);
    Rectangle copied_pbox = new Rectangle(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    return pixelbox;
  }

  /** Sets the minimum size in pixels. If d.width or d.height is negative,
      this uses their absolute value instead. */
  public void setMinimumPixelBounds(Dimension d)   {
    // to save a miniscule amount of memory, this is saved as
    // two integers rather than one Dimension object.
    min_pixels_width  = Math.abs(d.width);
    min_pixels_height = Math.abs(d.height);
  }

  /**
   * Sets the coordinates of the Glyph.
   * Follow AWT args convention: x, y, width, height.
   * This will convert rectangles of a negative width and/or height
   * to an equivalent rectangle with positive width and height.
   */
  public void setCoords(double x, double y, double width, double height)  {
    if (width < 0) {
      x = x + width;
      width = -width;
    }
    if (height < 0) {
      y = y + height;
      height = -height;
    }
    this.setRect(x, y, width, height);

  }

  public Rectangle2D.Double getCoordBox()   {
    return this;
  }


  /**
   * Replaces the coord box.
   * Note that this does not make the assurances of setCoords().
   * @see #setCoords
   */
  public void setCoordBox(Rectangle2D.Double coordbox)   {
    this.x = coordbox.x;
    this.y = coordbox.y;
    this.width = coordbox.width;
    this.height = coordbox.height;
  }

  /** For {@link EfficientGlyph} there is no difference between foreground and background color. */
  public void setForegroundColor(Color color)  {
    //    this.style = stylefactory.getStyle( color, style.getBackgroundColor(), style.getFont() );
    this.color = color;
  }

  /** For {@link EfficientGlyph} there is no difference between foreground and background color. */
  public Color getForegroundColor()  {
    return color;
    //    return this.style.getForegroundColor();
  }

  /** For {@link EfficientGlyph} there is no difference between foreground and background color. */
  public void setBackgroundColor(Color color)  {
    //    this.style = stylefactory.getStyle( style.getForegroundColor(), color, style.getFont() );
    this.color = color;
  }

  /** For {@link EfficientGlyph} there is no difference between foreground and background color. */
  public Color getBackgroundColor()  {
    //    return this.style.getBackgroundColor();
    return color;
  }

  public void setColor(Color color)  {
        this.setBackgroundColor( color );
  }

  public Color getColor()  {
    return this.getBackgroundColor();
  }

  public void setInfo(Object info)  {
    this.info = info;
  }

  public Object getInfo()  {
    return info;
  }

  public void setVisibility(boolean isVisible)  {
    this.isVisible = isVisible;
  }

  public boolean isVisible()  {
    return isVisible;
  }

  public void setPacker(PackerI packer)  {
    this.packer = packer;
  }

  public PackerI getPacker()  {
    return packer;
  }

  public void pack(ViewI view) {
    if (packer == null) { return; }
    packer.pack(this, view);
  }

  public void moveRelative(double diffx, double diffy) {
    this.x += diffx;
    this.y += diffy;
    if (children != null) {
      int numchildren = children.size();
      for (int i=0; i<numchildren; i++) {
        children.get(i).moveRelative(diffx, diffy);
      }
    }
  }

  public void moveAbsolute(double absx, double absy) {
    double diffx = absx - this.x;
    double diffy = absy - this.y;
    this.moveRelative(diffx, diffy);
  }

  public void setScene(Scene s) {
    scene = s;
    if (children != null) {
      int size = children.size();
      for (int i=0; i<size; i++) {
        children.get(i).setScene(s);
      }
    }
  }

  public Scene getScene() {
    return scene;
  }


  protected boolean selectable = true;

  /**
   * Sets the selectability of the glyph.
   *
   * @param selectability
   */
  public void setSelectable(boolean selectability) {
    if (!selectability) {
      setSelected(false);
    }
    this.selectable = selectability;
  }

  /**
   * Indicates whether or not the glyph can be selected.
   */
  public boolean isSelectable() {
    return this.selectable;
  }

  /**
   * Selects the glyph if it is selectable.
   * If it is not then this does nothing.
   *
   * @param selected true if the glyph is to be selected,
   * false otherwise.
   * @deprecated use {@link #setSelected(boolean)} instead.
   */
  /*public void select(boolean selected) {
    setSelected(selected);
  }*/

  /**
   * Selects the glyph if it is selectable.
   * If it is not then this does nothing.
   *
   * @param selected true if the glyph is to be selected,
   * false otherwise.
   */
	@Deprecated
  public void setSelected(boolean selected) {
    if (this.selectable) {
      this.selected = selected;
    }
  }

  /**
   * Indicates whether or not the glyph has been selected.
   */
  public final boolean isSelected() {
    return selected;
  }


  public boolean drawTransients() {
    return false;
  }

  /**
   *  Set trans to global transform for this glyph (based on
   *    getChildTransform() of parent).
   */
  public boolean  getGlobalTransform(ViewI view, LinearTransform trans) {
    trans.setTransform(view.getTransform());
    return getParent().getGlobalChildTransform(view, trans);
  }

  /** Default implementation does nothing. */
  public void getChildTransform(ViewI view, LinearTransform trans) {
    return;
  }

  public boolean getGlobalChildTransform(ViewI view, LinearTransform trans) {
    Stack<GlyphI> glstack = new Stack<GlyphI>();
    GlyphI rootgl = ((Scene)view.getScene()).getGlyph();
    GlyphI gl = this;
    glstack.push(gl);
    while (gl != rootgl) {
      gl = gl.getParent();
      // if get a null parent before getting root glyph, then fail and return
      if (parent == null) { return false; }
      glstack.push(gl);
    }
    trans.setTransform(view.getTransform());
    while (! glstack.empty()) {
      glstack.pop().getChildTransform(view, trans);
    }
    return true;
  }

  /** Fixes a bug that can happen with AWT when drawing very large rectangles, by
   *  trimming the pixelbox of a large rectangle to the region that intersects the view.
   */
  public static final Rectangle fixAWTBigRectBug(ViewI view, Rectangle pixelbox) {
    if (FIX_AWT_BIG_RECT_BUG) {
      if (pixelbox.width >= 1024) {
        Rectangle compbox = view.getComponentSizeRect();
		pixelbox = pixelbox.intersection(compbox);
	  }
    }
    return pixelbox;
  }

}
