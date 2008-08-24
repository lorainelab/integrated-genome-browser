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
import com.affymetrix.genoviz.glyph.StretchContainerGlyph;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

/**
 * This packer makes sure siblings do not overlap.
 * i.e. it makes sure all the direct children of the parent do not overlap.
 * This does not try to recursivly pack each child.
 * <p> Note that this packer ignores the coordFuzziness property.
 */
public class SiblingCoordAvoid extends AbstractCoordPacker {

  /**
   * Packs a child.
   * This adjusts the child's offset
   * until it no longer reports hitting any of it's siblings.
   */
  @Override
  public Rectangle pack(GlyphI parent, GlyphI child) {
    Rectangle2D.Double childbox, siblingbox;
    childbox = child.getCoordBox();
    List<GlyphI> children = parent.getChildren();
    if (children == null) { return null; }

    List<GlyphI> sibsinrange = new LinkedList<GlyphI>();
    
    for (GlyphI sibling : children) {
       siblingbox = sibling.getCoordBox();
       if (!(siblingbox.x > (childbox.x+childbox.width) ||
             ((siblingbox.x+siblingbox.width) < childbox.x)) ) {
         sibsinrange.add(sibling);
       }
    }

    before.setRect(childbox);
    boolean childMoved = true;
    while (childMoved) {
      childMoved = false;
      for (GlyphI sibling : sibsinrange) {
        if (sibling == child) {
          continue;
        }
        siblingbox = sibling.getCoordBox();
        if (child.getCoordBox().intersects(siblingbox)) {
          Rectangle2D.Double cb = child.getCoordBox();
          before.setRect(cb);
          moveToAvoid(child, sibling, movetype);
          childMoved |= !before.equals(child.getCoordBox());
        }
      }
    }

    if (parent instanceof StretchContainerGlyph) {
      ((StretchContainerGlyph)parent).propagateStretch(child);
    }

    return null;
  }

}
