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

package com.affymetrix.genoviz.bioviews;

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.util.*;
import com.affymetrix.genoviz.glyph.StretchContainerGlyph;

/**
 * This packer makes sure there is space between glyphs.
 * You can set the number of pixels that must appear between glyphs.
 * i.e. This packer uses the coordFuzziness property.
 */
public class VariableCoordAvoid extends AbstractCoordPacker {

  Rectangle2D expanded_childbox = new Rectangle2D();

  /**
   * packs a child.
   * This adjusts the child's offset
   * until none of it's siblings reports hitting it.
   * Each sibling is asked if it hits a box
   * that is coordFuzziness pixels around the child.
   * i.e. if coordFuzziness is 1 the "expanded box" is 2 pixels wider and taller.
   */
  public Rectangle pack(GlyphI parent,
      GlyphI child, ViewI view) {
    Rectangle2D childbox, siblingbox;
    childbox = child.getCoordBox();
    expanded_childbox.reshape(childbox.x - coord_fuzziness,
        childbox.y - coord_fuzziness,
        childbox.width + 2*coord_fuzziness,
        childbox.height + 2*coord_fuzziness);
    Vector children = parent.getChildren();

    if (children == null) { return null; }

    Vector sibsinrange = new Vector();
    GlyphI sibling;
    int i, j;
    for (i=0; i<children.size(); i++) {
       sibling = (GlyphI)children.elementAt(i);
       siblingbox = sibling.getCoordBox();
       if (!(siblingbox.x > (expanded_childbox.x + expanded_childbox.width) ||
             ((siblingbox.x+siblingbox.width) < expanded_childbox.x)) ) {
         sibsinrange.addElement(sibling);
       }
    }

    boolean somethingMoved = true;
    while (somethingMoved) {
      expanded_childbox.reshape(childbox.x - coord_fuzziness,
          childbox.y - coord_fuzziness,
          childbox.width + 2*coord_fuzziness,
          childbox.height + 2*coord_fuzziness);
      somethingMoved = false;
      for (j=0; j<sibsinrange.size(); j++) {
        sibling = (GlyphI)sibsinrange.elementAt(j);
        if (sibling == child) { continue; }
        if (sibling.hit(expanded_childbox, view)) {
          if ( child instanceof com.affymetrix.genoviz.glyph.LabelGlyph ) {
            /* LabelGlyphs cannot be so easily moved as other glyphs.
             * They will immediately snap back to the glyph they are labeling.
             * This can cause an infinite loop here.
             * What's worse is that the "snapping back" may happen outside the loop.
             * Hence the checking with "before" done below may not always work
             * for LabelGlyphs.
             * Someday, we might try changing the LabelGlyph's orientation
             * to its labeled glyph.
             * i.e. move it to the other side or inside it's labeled glyph.
             */
          }
          else {
            Rectangle2D cb = child.getCoordBox();
            this.before.x = cb.x;
            this.before.y = cb.y;
            this.before.width = cb.width;
            this.before.height = cb.height;
            moveToAvoid(child, sibling, movetype);
            somethingMoved |= ! before.equals(child.getCoordBox());
          }
        }
      }
    }
    if (parent instanceof StretchContainerGlyph) {
      ((StretchContainerGlyph)parent).propagateStretch(child);
    }
    return null;
  }

}
