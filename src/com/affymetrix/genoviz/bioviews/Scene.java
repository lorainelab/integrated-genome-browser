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
import java.awt.geom.Rectangle2D;
import java.util.*;
import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.glyph.TransientGlyph;

import java.util.List;

/**
 * Implementation of SceneI interface.
 * See SceneI for better documentation of methods.
 */
public class Scene implements SceneII  {
  private RootGlyph rootGlyph;
  private final List<ViewI> views;
  private Color selectColor;
  private SelectType selectType;

  /**
   * Damage flag to indicate if something has changed in the scene that
   * should force a full draw on the next Widget draw call.
   */
  protected boolean damaged = false;
  protected Rectangle2D.Double damageCoordBox;
  protected final Rectangle2D.Double scratchCoordBox;

  /**
   * List of transient glyphs that are "layered" on top of views
   * after all other glyphs have been drawn.
   */
  private List<TransientGlyph> transients;

  public Scene ()  {
    /*
     * rootGlyph is a RootGlyph glyph rather than a base glyph.
     * This should allow the scene to respond better to dynamic
     * addition of data.  This means the only place the bounding coords of the
     * scene are stored is in the rootGlyph, so Scene.coordbox has been
     * removed, and all use of coordbox has been replaced by calls to
     * rootGlyph.getCoordBox() -- GAH 12/6/97
     */
    rootGlyph = new RootGlyph();
    //rootGlyph.setScene(this);
    rootGlyph.setCoords(0,0,1,1);

    views = new ArrayList<ViewI>();
    selectColor = Color.RED;
    selectType = SelectType.SELECT_FILL;
    scratchCoordBox = new java.awt.geom.Rectangle2D.Double();
  }

  /**
   * Sets the coordinate bounds for the Scene.
   * Glyphs outside of these bounds will not be visible.
   * @param x X coordinate for top left
   * @param y Y coordinate for top left
   * @param w Width of coordinate box
   * @param h Height of coordinate box
   */
  public void setCoords(double x, double y, double w, double h) {
    rootGlyph.setCoords(x,y,w,h);
    maxDamage();
  }

  /**
   * Sets the root glyph of this scene.
   * Use {@link #addGlyph(com.affymetrix.genoviz.bioviews.GlyphI)} 
   * to add another glyph to the scene.
   * @param glyph the new RootGlyph
   */
  public void setRootGlyph(RootGlyph glyph) {
    rootGlyph = glyph;
    //rootGlyph.setScene(this);
    maxDamage();
  }

  /**
   * Returns the root glyph of this scene.
   */
  public RootGlyph getRootGlyph()  {
    return rootGlyph;
  }

  /**
   * Adds another glyph to the scene.
   */
  @Override
  public void addGlyph(GlyphI glyph) {
    if (glyph != null) {
      getRootGlyph().addChild(glyph);

      // transients should be added both to the root glyph (for pick
      //     traversal, etc.) and to the transients list (for actual drawing),
      //     but shouldn't be considered in damage expansion...
      if (glyph instanceof TransientGlyph) {
        addTransient((TransientGlyph)glyph);
      }
      else {
        expandDamage(glyph);
      }
    }
  }

  /**
   * Adds a glyph.
   * @param glyph to add.
   * @param i where to add it.
   */
  @Override
  public void addGlyph(GlyphI glyph, int i) {
    if (glyph != null) {
      getRootGlyph().addChild(glyph,i);
      expandDamage(glyph);
    }
  }

  /**
   * Adds a view representing this scene.
   */
  public void addView(ViewI view)  {
    views.add(view);
  }

  /**
   * Removes a view that had been representing the scene.
   */
  public void removeView(ViewI view)  {
    views.remove(view);
  }

  /**
   * Returns a List of the views that are currently representing the scene.
   */
  public List<ViewI> getViews()  {
    return views;
  }

  /**
   * Draws all views on all canvases.
   */
  public void draw()  {
    for (final ViewI view : views) {
      view.draw();
    }
    clearDamage();
  }

  /**
   * Draw one canvas.
   */
  //TODO: delete. Never used.
  public void draw(Component c, Graphics2D g)  {
    for (final ViewI view : views) {
      if (view.getComponent() == c) { // If this is the NeoCanvas asked for
        view.setGraphics(g);
        view.draw();
        break;
      }
    }
    // This will cause problems when trying to do damage control across
    // views on multiple canvases!!!  11-17-97
    // should probably switch to a long damage_counter for both View
    // and Scene, so View can compare to its own counter and decide based
    // on that what to do, and Scene can in turn check View counters to
    // decide when it can zero out the damage again
    clearDamage();
  }

  @Override
  public Rectangle2D.Double getCoordBox() {
    return rootGlyph.getCoordBox();
  }
 
  @Override
  public void pickTraversal(Rectangle2D.Double coordrect, 
    List<GlyphI> pickvect, ViewI view) {
    rootGlyph.pickTraversal(coordrect, pickvect, view);
  }

  /**
   * Sets visibility for a partiuclar glyph in the scene.
   * @param glyph the glyph to set.
   * @param isVisible whether or not the glyph is visible.
   */
  @Override
  public void setVisibility(GlyphI glyph, boolean isVisible) {
    glyph.setVisibility(isVisible);
    expandDamage(glyph);
  }

  /*
   * Alternatively, damage expansion on selection/removal/visibility
   * changes should be handled by the glyphs themselves -- if go that
   * route, should add a Scene field to base glyph
   */

  /**
   * Selects a glyph.
   * @param gl The glyph to select
   */
  @Override
  public void select(GlyphI gl) {
    if (gl != null) {
      gl.setSelected(true);
      expandDamage(gl);
    }
  }

  /**
   * Deselects a glyph.
   * @param gl the glyph to be deselected.
   */
  @Override
  public void deselect(GlyphI gl) {
    if (gl != null) {
      gl.setSelected(false);
      expandDamage(gl);
    }
  }

  /**
   * Removes a glyph from the scene.
   * @param gl the glyph to be removed.
   */
  public void removeGlyph(GlyphI gl) {
    expandDamage(gl);
    // special case: if gl is the top-level glyph, set to null
    if (gl == this.getRootGlyph()) {
      setRootGlyph(null);
    }
    // otherwise remove reference to gl in parent's children
    else {
      GlyphI parent = gl.getParent();
      parent.removeChild(gl);
      //      List siblings = parent.getChildren();
      //      siblings.removeElement(gl);
    }
    if (gl instanceof TransientGlyph) {
      removeTransient((TransientGlyph)gl);
    }
  }

  public SelectType getSelectionStyle() {
    return getSelectionAppearance();
  }

  public void setSelectionAppearance(SelectType s) {
    selectType = s;
  }

  public SelectType getSelectionAppearance() {
    return selectType;
  }

  public void setSelectionColor(Color col) {
    selectColor = col;
  }

  public Color getSelectionColor() {
    return selectColor;
  }

  public void maxDamage() {
    expandDamage(rootGlyph);
  }

  /**
   * Expands damaged area to include glyph's coordbox.
   */
  public void expandDamage(GlyphI glyph) {
    if (glyph == null) {
      return;
    }
    damaged = true;
    Rectangle2D.Double gcoords = glyph.getCoordBox();

    if (damageCoordBox == null) {
      damageCoordBox = new Rectangle2D.Double();
      damageCoordBox.setRect(gcoords);
    }
    else {
      damageCoordBox.add(gcoords);
    }
  }

  /*
   * Just a pass-through to expandDamage(x, y, width, height) for now,
   * but may want to use glyph-specific info in later versions, for example
   * to optimize for a selection that is expanding/contracting
   *
   * Alternatively, damage expansion on selection should be handled by
   * the glyphs themselves -- if go that route, should add a Scene field
   * to base glyph
   */
  public void expandDamage(GlyphI glyph, double x, double y,
      double width, double height) {
    expandDamage(x, y, width, height);
  }

  public void expandDamage(double x, double y, double width, double height) {
    damaged = true;
    if (width < 0) {
      x = x + width;
      width = -width;
    }
    if (height < 0) {
      y = y + height;
      height = -height;
    }

    if (damageCoordBox == null) {
      damageCoordBox = new Rectangle2D.Double(x, y, width, height);
    }
    else {
      damageCoordBox.add(x, y);
      damageCoordBox.add(x+width, y+height);
    }
  }

  public void clearDamage() {
    damaged = false;
    damageCoordBox = null;
  }

  public boolean isDamaged() {
    return damaged;
  }

  public Rectangle2D.Double getDamageCoordBox() {
    return damageCoordBox;
  }

  public boolean hasTransients() {
    return (transients != null && transients.size() > 0);
  }

  public void addTransient(TransientGlyph tg) {
    if (transients == null) {
      transients = new ArrayList<TransientGlyph>();
    }
    transients.add(tg);
  }

  public void removeTransient(TransientGlyph tg) {
    if (transients != null) { 
      transients.remove(tg);
    }
  }

  /**
   * Clears out the list of transient glyphs.
   * This allows for a more complete clearing of a NeoMap.
   * @see com.affymetrix.genoviz.widget.NeoMap#clearWidget()
   */
  public void removeAllTransients() {
    if ( null != transients ) {
      transients.clear();
    }
  }

  /**
   * Returns a non-null, but possibly empty, unmodifiable list of TransientGlyphs.
   * @return a non-null, but possibly empty, unmodifiable list
   */
  public List<TransientGlyph> getTransients() {
    return (transients == null) ? 
      Collections.<TransientGlyph>emptyList() : Collections.unmodifiableList(transients);
  }

  /**
   * Make glyph gl be drawn behind all its sibling glyphs.
   */
  public void toBackOfSiblings(GlyphI gl) {
    GlyphI parent = gl.getParent();
    if (parent != null) {
      parent.removeChild(gl);
      parent.addChild(gl, 0);
    }
  }

  /**
   * Make this glyph be drawn in front of all its sibling glyphs.
   * (with the exception that it will not be drawn in front of transient glyphs)
   */
  public void toFrontOfSiblings(GlyphI gl) {
    GlyphI parent = gl.getParent();
    if (parent != null) {
      parent.removeChild(gl);
      parent.addChild(gl);
    }
  }

  /**
   * Make glyph gl be drawn behind all other glyphs.
   * (before all other glyphs)
   */
  public void toBack(GlyphI gl) {
    GlyphI child = gl;
    GlyphI parent = gl.getParent();
    while (parent != null) {
      toBackOfSiblings(child);
      child = parent;
      parent = child.getParent();
    }
  }

  /**
   * Make this glyph be drawn in front.
   * (after all other glyphs)
   * Except, will not be drawn in front of transient glyphs.
   */
  public void toFront(GlyphI gl) {
    GlyphI child = gl;
    GlyphI parent = child.getParent();
    while (parent != null) {  // maybe also check for parent != child ???
      toFrontOfSiblings(child);
      child = parent;
      parent = child.getParent();
    }
  }

}
