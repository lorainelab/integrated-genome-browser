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
import com.affymetrix.genoviz.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.StretchContainerGlyph;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.genoviz.util.NeoConstants;

// note that this packer ignores coord fuzziness
public class ModSiblingCoordAvoid extends AbstractCoordPacker {

  public Rectangle pack(GlyphI parent,
      GlyphI child, ViewI view) {
    Rectangle2D childbox, siblingbox;
    childbox = child.getCoordBox();
    Vector children = parent.getChildren();

    if (children == null) { return null; }

    Vector sibsinrange = new Vector();
    boolean hit = true;
    GlyphI sibling;
    int i, j;
    for (i=0; i<children.size(); i++) {
      sibling = (GlyphI)children.elementAt(i);
      if (sibling instanceof TransientGlyph) {
        continue;
      }
      siblingbox = sibling.getCoordBox();
      if (!(siblingbox.x > (childbox.x+childbox.width) ||
            ((siblingbox.x+siblingbox.width) < childbox.x)) ) {
        sibsinrange.addElement(sibling);
      }
    }
    while (hit) {
      hit = false;
      for (j=0; j<sibsinrange.size(); j++) {
        sibling = (GlyphI)sibsinrange.elementAt(j);
        if (sibling == child) { continue; }
        siblingbox = sibling.getCoordBox();
        if (child.hit(siblingbox, view)) {
          // need to move
          hit = true;
          moveToAvoid(child, sibling, movetype);
        }
      }
    }
    if (parent instanceof StretchContainerGlyph) {
      ((StretchContainerGlyph)parent).propagateStretch(child);
    }
    return null;
  }

}
