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

package com.affymetrix.genoviz.pack;

import com.affymetrix.genoviz.bioviews.*;
import java.awt.Rectangle;

/**
 * Interface for intelligent layout of glyphs in a {@link SceneI},
 * independent of the {@link ViewI}.
 */
public interface PackerI {

  /**
   * Packs a given glyph within it's parent.
   * This is typically called for each child of a glyph
   * in the other pack method of this interface.
   *
   * @param parent
   * @param child
   * @param view in which to pack glyphs.
   * @return ???? //TODO:
   */
  public Rectangle pack(GlyphI parent, GlyphI child);

  /**
   * Packs all the children of a glyph.
   *
   * @param parent
   * @return ???? //TODO:
   */
  public Rectangle pack(GlyphI parent_glyph);
}
