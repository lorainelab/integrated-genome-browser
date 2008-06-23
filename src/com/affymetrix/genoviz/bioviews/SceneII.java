/**
*   Copyright (c) 2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import java.awt.geom.Rectangle2D;
import java.util.List;


/**
 * Extends SceneI to add damage control and transient glyphs.
 */
public interface SceneII extends SceneI {


  public RootGlyph getRootGlyph();

  public void pickTraversal(Rectangle2D.Double coordrect, List<GlyphI> pickvect, 
    ViewI view);


  public void setRootGlyph(RootGlyph root);

  /**
   * Expands damaged area to its maximum size.
   */
  public void maxDamage();
  
  /**
   * Expands damaged area to include glyph's coordbox.
   */
  public void expandDamage(GlyphI glyph);

  public void expandDamage(GlyphI glyph, double x, double y,
      double width, double height);

  public void expandDamage(double x, double y, double width, double height);

  public void clearDamage();

  public boolean isDamaged();

  public Rectangle2D.Double getDamageCoordBox();


  public boolean hasTransients();

  public void addTransient(TransientGlyph tg);

  public void removeTransient(TransientGlyph tg);

  /**
   * Clears out the list of transient glyphs.
   * This allows for a more complete clearing of a NeoMap.
   * @see com.affymetrix.genoviz.widget.NeoMap#clearWidget()
   */
  public void removeAllTransients();

  /**
   * Returns a list of TransientGlyphs.
   * @return a non-null, but possibly empty, unmodifiable list
   */
  public List<TransientGlyph> getTransients();

  /** Sets glyph visibility and expands damage. */
  public void setVisibility(GlyphI gl, boolean visible);

  /** Selects glyph and expands damage. */
  public void select(GlyphI g);

  /** Selects glyph and expands damage. */
  public void select(GlyphI g, double x, double y, double width, double height);

  public void toBack(GlyphI gl);
  
  public void toBackOfSiblings(GlyphI glyph);

  public void toFront(GlyphI gl);

  public void toFrontOfSiblings(GlyphI glyph);

  public void removeGlyph(GlyphI gl);

  public void deselect(GlyphI g);

  public void setCoords(double start, double y, double size, double height);
}
