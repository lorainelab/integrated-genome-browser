/**
*   Copyright (c) 2001-2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genoviz.glyph.efficient;

import java.awt.*;
import java.util.List;
import java.util.Stack;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


/**
 * A more efficient implementation of the {@link GlyphI} interface.
 * This class directly extends {@link java.awt.geom.Rectangle2D.Double} rather than
 * containing a reference to a rectangle.  This makes more efficient
 * use of memory.
 * 
 * @author Gregg Helt 
 */
public class EffGlyph extends Rectangle2D.Double implements GlyphI {

  public static enum DrawOrder {
    DrawSelfFirst, DrawChildrenFirst
  }
  
  private static final boolean debug = false;
  private static final boolean DEBUG_DT = false;

  // If true, apply corrections to avoid an AWT drawing bug that can happen
  // for very large glyphs (bigger than about 32000 pixels).
  static final boolean FIX_AWT_BIG_RECT_BUG = true;

  static protected int min_pixels_width=1;
  static protected int min_pixels_height=1;

  protected SceneII scene;

  protected GlyphI parent;
  protected List<GlyphI> children;

  protected Color color = Color.black;
  protected boolean isVisible;
  protected Object info;
  protected PackerI packer;
  protected boolean selected;
  protected DrawOrder drawOrder = DrawOrder.DrawSelfFirst;

  public EffGlyph() {
    super();
    isVisible = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean withinView(ViewI view) {
    return this.intersects(view.getCoordBox());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void select(double x, double y, double width, double height) {
    setSelected(true);
  }

  /**
   * Always returns false.
   * @return false
   */
  @Override
  public boolean supportsSubSelection() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Rectangle2D.Double getSelectedRegion() {//TODO: delete?
    if (selected) { return this; }
    else { return null; }
  }

  /**
   * Sets the {@link DrawOrder}.
   */
  public void setDrawOrder(DrawOrder order) {
    this.drawOrder = order;
  }

  /**
   * @return the {@link DrawOrder}.
   */
  public DrawOrder getDrawOrder() {
    return drawOrder;
  }

  @Override
  public void drawTraversal(ViewI view)  {
    if (DEBUG_DT) {
      System.err.println("called Glyph.drawTraversal() on " + this);
    }
    if (drawOrder == DrawOrder.DrawSelfFirst) {
      if (withinView(view) && isVisible) {
        if (selected) { 
          drawSelected(view); 
        }
        else {
          draw(view); 
        }
        if (children != null) {
          drawChildren(view); 
        }
      }
    }
    else if (drawOrder == DrawOrder.DrawChildrenFirst) {
      if (withinView(view) && isVisible) {
        if (children != null) {
          drawChildren(view);
        }
        if (selected) {
          drawSelected(view); 
        }
        else { 
          draw(view); 
        }
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

  /**
   * Does nothing.  Subclasses can override to draw this
   * glyph, but not the children.  The children are drawn
   * in {@link #drawChildren(ViewI)}
   * @param view
   */
  @Override
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

  /**
   * Draws the glyph in a special way to indicate that it is selected.
   * Calls the appropriate sub-method, 
   * such as {@link #drawSelectedOutline(ViewI)}.
   * All of those sub-methods will at some point make a call
   * to {@link #draw(ViewI)}.
   * @param view
   */
  public void drawSelected(ViewI view) {
    switch (view.getScene().getSelectionAppearance()) {
      case SELECT_OUTLINE:
        drawSelectedOutline(view);
        break;
      case SELECT_FILL:
        drawSelectedFill(view);
        break;
      case SELECT_REVERSE:
        drawSelectedReverse(view);
        break;
      case SELECT_NONE:
        draw(view);
        break;
      default:
        throw new RuntimeException();
    }
  }

  /**
   * Draws the glyph with a different background to indicate that it is selected.
   */
  protected void drawSelectedBackground(ViewI view) {
    Graphics g = view.getGraphics();
    Rectangle pixelbox = view.getScratchPixBox();
    g.setColor(view.getScene().getSelectionColor());
    view.transformToPixels(this, pixelbox);
    g.fillRect(pixelbox.x-3, pixelbox.y-3,
               pixelbox.width+6, pixelbox.height+6);
    draw(view);
  }

  /**
   * Draws the glyph with an outline to indicate that it is selected.
   */
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

  /**
   * Switches foreground and background colors to indicate 
   * that the glyph is selected.
   */
  protected void drawSelectedReverse( ViewI view ) {
    Color bg = this.getBackgroundColor();
    Color fg = this.getForegroundColor();
    this.setBackgroundColor( fg );
    this.setForegroundColor( bg );
    this.draw(view);
    this.setBackgroundColor( bg );
    this.setForegroundColor( fg );
  }

  /** {@inheritDoc} */
  @Override
  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList, ViewI view)  {
    if (isVisible && intersects(pickRect, view))  {
      if (debug)  {
        System.out.println("intersects");
      }
      if (hit(pickRect, view))  {
        if (!pickList.contains(this)) {
 //TODO: why not just override equals() in EffGlyph?
          // Note that contains() performs a test using "equals()".
          // EffGlyph extends Rectangle2D which tests equality based
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
          child.pickTraversal(pickRect, pickList, view );
        }
      }
    }
  }
  
  //TODO: implement or delete
  /** NOT YET IMPLEMENTED. */
  @Override
  public void pickTraversal(Rectangle pickrect, List<GlyphI> pickList, ViewI view) {
    //TODO: need to covert pickRect to coords...
    /*
    if (isVisible && intersects(pickRect, view))  {
      if (debug)  {
        System.out.println("intersects");
      }
      if (hit(pickRect, view))  {
        if (!pickVec.contains(this)) {
          pictList.add(this);
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
          child.pickTraversal(pickRect, pictList, view);
        }
      }
    }
    */
  }

  /**
   *  This base class always returns false.  Sub-classes must implement.
   */
  public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean intersects(Rectangle2D.Double rect, ViewI view)  {
    return isVisible && rect.intersects(this);
  }


  /**
   * Always returns false, unless overridden.
   * @return false
   */@Override
  public boolean isHitable() { return false; }

  /** {@inheritDoc} */
  @Override
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

  /** {@inheritDoc} */
  @Override
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

  /** {@inheritDoc} */
  @Override
  public void removeChild(GlyphI glyph)  {
    if (children != null)      {
      children.remove(glyph);
      if (children.size() == 0) { children = null; }
    }
    glyph.setScene(null);
  }

  /** {@inheritDoc} */
  @Override
  public void removeAllChildren() {
    if (children != null)  {
      for (int i=0; i<children.size(); i++) {
        children.get(i).setScene(null);
      }
    }
    children = null;
  }

  /** {@inheritDoc} */
  @Override
  public int getChildCount() {
    if (children == null) { return 0; }
    else { return children.size(); }
  }

  /** {@inheritDoc} */
  @Override
  public GlyphI getChild(int index) {
    return children.get(index);
  }

  /** {@inheritDoc} */
  @Override
  public List<GlyphI> getChildren()  {
    return children;
  }

  /** {@inheritDoc} */
  @Override
  public void setParent(GlyphI glyph)  {
    parent = glyph;
    setScene(glyph.getScene());
  }

  /** {@inheritDoc} */
  @Override
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
  @Override
  public Rectangle getPixelBox(ViewI view)  {
    Rectangle pixelbox = view.getScratchPixBox();
    pixelbox = view.transformToPixels (this, pixelbox);
    //Rectangle copied_pbox = new Rectangle(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    return pixelbox;
  }

  /** Sets the minimum size in pixels. 
   * If d.width or d.height is negative,
   * this uses their absolute value instead. 
   */
  @Override
  public void setMinimumPixelBounds(Dimension d)   {
    // to save a miniscule amount of memory, this is saved as
    // two integers rather than one Dimension object.
    min_pixels_width  = Math.abs(d.width);
    min_pixels_height = Math.abs(d.height);
  }
  
  protected final void applyMinimumPixelBounds(final Rectangle pixelbox) {
    pixelbox.width = Math.max( pixelbox.width, min_pixels_width );
    pixelbox.height = Math.max( pixelbox.height, min_pixels_height );
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

  /** {@inheritDoc} */
  @Override
  public Rectangle2D.Double getCoordBox()   {
    return this;
  }


  /**
   * Replaces the coord box.
   * Note that this does not make the assurances of setCoords().
   * @see #setCoords
   */
  @Override
  public void setCoordBox(Rectangle2D.Double coordbox)   {
    this.x = coordbox.x;
    this.y = coordbox.y;
    this.width = coordbox.width;
    this.height = coordbox.height;
  }

  /** For {@link EffGlyph} there is no difference between foreground and background color. */
  public void setForegroundColor(Color color)  {
    //    this.style = stylefactory.getStyle( color, style.getBackgroundColor(), style.getFont() );
    this.color = color;
  }

  /** For {@link EffGlyph} there is no difference between foreground and background color. */
  public Color getForegroundColor()  {
    return color;
    //    return this.style.getForegroundColor();
  }

  /** For {@link EffGlyph} there is no difference between foreground and background color. */
  @Override
  public void setBackgroundColor(Color color)  {
    //    this.style = stylefactory.getStyle( style.getForegroundColor(), color, style.getFont() );
    this.color = color;
  }

  /** For {@link EffGlyph} there is no difference between foreground and background color. */
  @Override
  public Color getBackgroundColor()  {
    //    return this.style.getBackgroundColor();
    return color;
  }

  /** {@inheritDoc} */
  @Override
  public void setColor(Color color)  {
        this.setBackgroundColor( color );
  }

  /** {@inheritDoc} */
  @Override
  public Color getColor()  {
    return this.getBackgroundColor();
  }

  /** {@inheritDoc} */
  @Override
  public void setInfo(Object info)  {
    this.info = info;
  }

  /** {@inheritDoc} */
  @Override
  public Object getInfo()  {
    return info;
  }

  /** {@inheritDoc} */
  @Override
  public void setVisibility(boolean isVisible)  {
    this.isVisible = isVisible;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isVisible()  {
    return isVisible;
  }

  /** {@inheritDoc} */
  @Override
  public void setPacker(PackerI packer)  {
    this.packer = packer;
  }

  /** {@inheritDoc} */
  @Override
  public PackerI getPacker()  {
    return packer;
  }

  /** {@inheritDoc} */
  @Override
  public void pack(ViewI view) {
    if (packer == null) { return; }
    packer.pack(this, view);
  }

  /** {@inheritDoc} */
  @Override
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

  /** {@inheritDoc} */
  @Override
  public void moveAbsolute(double absx, double absy) {
    double diffx = absx - this.getX();
    double diffy = absy - this.getY();
    this.moveRelative(diffx, diffy);
  }

  /** {@inheritDoc} */
  @Override
  public void setScene(SceneII s) {
    scene = s;
    if (children != null) {
      int size = children.size();
      for (int i=0; i<size; i++) {
        children.get(i).setScene(s);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public SceneII getScene() {
    return scene;
  }


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


  public boolean drawTransients() {
    return false;
  }

  /**
   *  Set trans to global transform for this glyph (based on
   *    getChildTransform() of parent).
   */
  @Override
  public boolean  getGlobalTransform(ViewI view, LinearTransform trans) {
    trans.copyTransform((LinearTransform)view.getTransform());
    return getParent().getGlobalChildTransform(view, trans);
  }

  /** Default implementation does nothing. */
  @Override
  public void getChildTransform(ViewI view, LinearTransform trans) {
    return;
  }

  @Override
  public boolean getGlobalChildTransform(ViewI view, LinearTransform trans) {
    Stack<GlyphI> glstack = new Stack<GlyphI>();
    GlyphI rootgl = ((SceneII) view.getScene()).getRootGlyph(); //TODO: unchecked cast (and why not use the GlyphI's own Scene?
    GlyphI gl = this;
    glstack.push(gl);
    while (gl != rootgl) {
      gl = gl.getParent();
      // if get a null parent before getting root glyph, then fail and return
      if (parent == null) { return false; }
      glstack.push(gl);
    }
    trans.copyTransform((LinearTransform)view.getTransform());
    while (! glstack.empty()) {
      glstack.pop().getChildTransform(view, trans);
    }
    return true;
  }


  /**
   * Only returns true if this is the same objects as the given object.
   * (It is not uncommon to have two or more identical glyphs representing
   * different objects.  The glyph packers need to recognize them as
   * distinct glyphs.)
   * @param obj
   */
    @Override
    public boolean equals(Object obj) {
      return (this == obj);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }  
}
