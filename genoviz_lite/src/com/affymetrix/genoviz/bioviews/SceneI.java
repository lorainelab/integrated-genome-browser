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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;


/**
 * A SceneI is an abstract two dimensional space with x and y coordinates
 * specified as doubles.  The scene contains a rooted
 * hierarchy of GlyphI objects which have been placed onto the scene at
 * specific coordinates.  The scene also can have multiple {@link ViewI}s
 * that look onto the scene and manage the visual representation 
 * of a scene on an AWT component.
 *
 * <p> SceneI, along with ViewI and GlyphI, is one of the three fundamental
 * interfaces comprising Affymetrix' inner 2D structured graphics
 * architecture.
 */
public interface SceneI {

  /** 
   * Specifies how to distinguish selected {@link GlyphI}'s 
   * from others. 
   */
  public enum SelectType {
  /**
   * Do not distinguish selected glyphs
   * from non-selected glyphs.
   */
  SELECT_NONE,
  
  /**
   * Distinguish selected glyphs
   * by outlining them with selection color.
   */
  SELECT_OUTLINE,

  /**
   * Distinguish selected glyph
   * by filling them with selection color.
   */
  SELECT_FILL,

  /**
   * Distinguish selected glyph
   * by filling rectangle behind them with selection color.
   */
  BACKGROUND_FILL,

  /**
   * Distinguish selected glyph
   * by reversing forground and background colors.
   */
   SELECT_REVERSE
  };

  // This constant exists to ease transition from genoviz to genovizLite.
  public static final SelectType SELECT_OUTLINE = SelectType.SELECT_OUTLINE;
  

  /**
   *  Selection style to apply to glyphs within this scene.
   */
  public void setSelectionAppearance(SelectType id);

  /**
   *  Returns the selection appearance to apply to glyphs within this scene.
   */
  public SelectType getSelectionAppearance();

  /**
   * return color for selected glyphs within this scene
   */
  public Color getSelectionColor();

  /**
   *  return color for selected glyphs within this scene
   */
  public void setSelectionColor(Color col);

  /**
   *  Add a view onto the scene
   */
  public void addView(ViewI view);

  /**
   *  Remove a view from the scene
   */
  public void removeView(ViewI view);

  /**
   *  Return a List of all views onto the scene.
   */
  public List<ViewI> getViews();

  /**
   *  Draw all the views of this scene.
   */
  public void draw();  // draw all views on all canvases

  /**
   *  Draw all the views of this scene that use Component c.
   */
  public void draw(Component c, Graphics2D g);

  /**
   *  Add a glyph to the scene.
   */
  public void addGlyph(GlyphI glyph);

  /**
   *  Insert a glyph into the top level of the glyph hierarchy at position i.
   */
  public void addGlyph(GlyphI glyph, int i);

  /**
   *  Return the bounds of the entire scene in logical coordinate space,
   *  not pixel space.
   */
  public Rectangle2D.Double getCoordBox();
}
