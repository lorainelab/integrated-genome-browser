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

package com.affymetrix.genoviz.widget.tieredmap;

import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;

/**
 * TODO: NOT YET IMPLEMENTED.
 *  (just starting...)
 * a packer that stacks all the glyphs currently "in" view.
 * Currently treats view as a column,
 * (so in view means horizontal range overlaps view),
 * and puts other glyphs out of the way.
 */

public class InViewStackerPacker implements PackerI {

  Rectangle2D columnbox = new Rectangle2D();
  int yspacer = 3;

  /**
   * packs a given glyph within it's parent.
   * currently returns null rather than a real Rectangle
   *
   * @param parent_glyph
   * @param child_glyph
   * @param view in which to pack glyphs.
   */
  public Rectangle pack(GlyphI parent_glyph, GlyphI child_glyph, ViewI view) {
    // not implemented...
    // rely on pack(parent, view) instead?
    throw new IllegalArgumentException("InViewStackerPacker.pack(parent, " +
        "child, view) not implemented, use pack(parent, view) instead");
  }

  /**
   * packs a all the children of a glyph in a view.
   * currently returns null rather than a real Rectangle
   *
   * @param parent_glyph the glyph to pack
   * @param view
   */
  public Rectangle pack(GlyphI parent_glyph, ViewI view) {
    Rectangle2D viewbox = view.getCoordBox();
    Rectangle2D scenebox = view.getScene().getCoordBox();
    columnbox.reshape(viewbox.x, scenebox.y, viewbox.width, scenebox.height);
    double ycurrent = viewbox.y;
    double ymax = viewbox.y + viewbox.height;

    Vector children = parent_glyph.getChildren();
    if (children == null) { return null; }
    GlyphI child;
    Rectangle2D cbox;
    for (int i=0; i<children.size(); i++) {
      child = (GlyphI)children.elementAt(i);
      cbox = child.getCoordBox();
      columnbox.y = cbox.y;
      columnbox.height = cbox.height;
      if (cbox.intersects(columnbox)) {
        child.moveAbsolute(cbox.x, ycurrent);
        ycurrent = ycurrent + cbox.height + yspacer;
      }
    }
    return null;
  }


}
