/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.Rectangle2D;
import java.util.Comparator;

  /**
   *  A Comparator that can be used to sort tiers into realms,
   *  such that positive strands are always above the axis and
   *  negative strands are always below the axis.
   *  This sorter puts {@link TierGlyph#DIRECTION_FORWARD} strands on top, 
   *  followed by the axis tier,
   *  followed by {@link TierGlyph#DIRECTION_REVERSE} strands on bottom.  Other strands (
   *  {@link TierGlyph#DIRECTION_BOTH} or
   *  {@link TierGlyph#DIRECTION_NONE}) can be placed anywhere.
   *  see {@link TierLabelManager#setTierSorter(Comparator)}
   */
  public class RealmBasedTierSorter implements Comparator {
    TierLabelManager manager;
    
    public RealmBasedTierSorter(TierLabelManager manager) {
      this.manager = manager;
    }
    
    public int compare(Object obj1, Object obj2) {
      if (getUseRealms()) {
        return compare((TierLabelGlyph) obj1, (TierLabelGlyph) obj2);
      } else {
        return basicCompare((TierLabelGlyph) obj1, (TierLabelGlyph) obj2);
      }
    }

    // Whether or not the realm-based sorting will be used.
    // If false, revert to standard sorting, which allows tiers to
    // be placed in any order the user chooses.
    public boolean getUseRealms() {
      return true;
    }
    
    public int compare(TierLabelGlyph label_1, TierLabelGlyph label_2) {
      TierGlyph ref1 = label_1.getReferenceTier();
      TierGlyph ref2 = label_2.getReferenceTier();
      
      if (ref1 == ref2) return 0; // shouldn't happen
      
      // There can be multiple realms.
      // Tiers in the same realm are sorted by the y-coords of their handles.
      // Tiers in different realms are sorted purley by their realm:
      // lower realm numbers go on top.
      //
      int realm_1 = -1;
      int realm_2 = -1;
      
      int dir1 = ref1.getDirection();
      int dir2 = ref2.getDirection();
      
      switch (dir1) {
        case TierGlyph.DIRECTION_FORWARD:
          realm_1 = 0; break;
        case TierGlyph.DIRECTION_AXIS:
          realm_1 = 1; break;
        case TierGlyph.DIRECTION_REVERSE:
          realm_1 = 2; break;
        default:
          // if either tier is DIRECTION_NONE or _BOTH, then realms do not apply
          return basicCompare(label_1, label_2);
      }
      
      switch (dir2) {
        case TierGlyph.DIRECTION_FORWARD:
          realm_2 = 0; break;
        case TierGlyph.DIRECTION_AXIS:
          realm_2 = 1; break;
        case TierGlyph.DIRECTION_REVERSE:
          realm_2 = 2; break;
        default:
          // if either tier is DIRECTION_NONE or _BOTH, then realms do not apply
          return basicCompare(label_1, label_2);
      }
 
      if (realm_1 < realm_2) {
        return -1;
      } else if (realm_1 > realm_2) {
        return 1;
      }

      // else... look at the height of the label glyph for items in the same realm
      return basicCompare(label_1, label_2);
    }
    
    int basicCompare(TierLabelGlyph g1, TierLabelGlyph g2) {
      Rectangle2D box1 = g1.getCoordBox();
      Rectangle2D box2 = g2.getCoordBox();
      
      //final double middle_1 = box1.y + box1.height/2.0;
      //final double middle_2 = box2.y + box2.height/2.0;

      if (box1.y < box2.y) { return -1; }
      else if (box1.y > box2.y) { return 1; }
      else { return 0; }
    }
  }
