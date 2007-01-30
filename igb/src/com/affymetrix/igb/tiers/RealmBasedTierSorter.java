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
import com.affymetrix.igb.view.SeqMapView;
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
    SeqMapView smv = null;    
    
    public RealmBasedTierSorter(SeqMapView smv) {
      this.smv = smv;
    }
    
    public int compare(Object obj1, Object obj2) {
      return compare((TierLabelGlyph) obj1, (TierLabelGlyph) obj2);
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
      // For now:
      // realm = -1 is for (+/-) and (no-direction) strands
      // realm = 0  is for (+) strand
      // realm = 1  is for axis tier
      // realm = 2  is for (-) strand
      int realm_1 = -1;
      int realm_2 = -1;
      
      int dir1 = ref1.getDirection();
      int dir2 = ref2.getDirection();

      if (dir1 == TierGlyph.DIRECTION_REVERSE) {
        realm_1 = 2;
      } else if (dir1 == TierGlyph.DIRECTION_FORWARD) {
        realm_1 = 0;
      } else {
        realm_1 = -1;
      }
      
      if (dir2 == TierGlyph.DIRECTION_REVERSE) {
        realm_2 = 2;
      } else if (dir2 == TierGlyph.DIRECTION_FORWARD) {
        realm_2 = 0;
      } else {
        realm_2 = -1;
      }
      
      if (smv != null) {
        TierGlyph central_tier = smv.getAxisTier();
        if (ref1 == central_tier) {
          realm_1 = 1;
        }
        else if (ref2 == central_tier) {
          realm_2 = 1;
        }
      }
            
      //IAnnotStyle a1 = ref1.getAnnotStyle();
      //IAnnotStyle a2 = ref2.getAnnotStyle();            
      
      // anything declared as realm -1 can go wherever it wants
      if (realm_1 == -1 || realm_2 == -1) {
        return basicCompare(label_1, label_2);
      }

      if (realm_1 < realm_2) {
        return -1;
      } else if (realm_2 > realm_1) {
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
