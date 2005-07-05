/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import java.awt.Color;
import java.util.List;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;

public class GlyphEdgeMatcher  {

  Color col = Color.white;
  int start_match_count = 0;
  int end_match_count = 0;
  int check_count = 0;
  int intersect_count = 0;
  int call_count = 0;
  double fuzness = 0;

  static GlyphEdgeMatcher singleton_matcher = null;
  
  protected GlyphEdgeMatcher() {
  }

  public static GlyphEdgeMatcher getSingleton() {
    if (singleton_matcher == null) {
      singleton_matcher = new GlyphEdgeMatcher();
    }
    return singleton_matcher;
  }
  
  public void setColor(Color c) {
    this.col = c;
  }
  
  public Color getColor() { return col; }

  public void setFuzziness(double fuz) {
    fuzness = fuz;
  }

  public double getFuzziness() {
    return fuzness;
  }
  
  /** 
   *  Recursive glyph edge-matching.
   *  Recursively descends,
   *    only matches leaf nodes (getChildren() == null || getChildren().size()<1)
   *   
   *  Glyphs present in both query_glyphs and target_glyphs will match each other
   *    (no filtering against matching self).
   *
   *  Glyphs added for matches are collected and returned in match_glyphs vector.
   */
  public void matchEdges(NeoMapI map, List query_glyphs, List target_glyphs, List match_glyphs) {
    call_count = 0;
    start_match_count = 0;
    end_match_count = 0;
    check_count = 0;
    intersect_count = 0;
    //    System.out.println("called GlyphEdgeMatcher.matchEdges(), query_glyph count = " + query_glyphs.size());
    for (int i=0; i<query_glyphs.size(); i++) {
      GlyphI query = (GlyphI) query_glyphs.get(i);
      matchEdges(map, query, target_glyphs, match_glyphs);
    }
  }

  public void matchEdges(NeoMapI map, GlyphI query, List target_glyphs, List match_glyphs) {
    for (int k=0; k<target_glyphs.size(); k++) {
      GlyphI target = (GlyphI) target_glyphs.get(k);
      matchEdges(map, query, target, match_glyphs);
    }
  }
  
  public void matchEdges(NeoMapI map, GlyphI query, GlyphI target, List match_glyphs) {
    //    System.out.println("trying to match edges: query = " + query + ", target = " + target);
    call_count++;
    
    // Simply skip all TransientGlyph, such as the hairline shadow
    if ((target instanceof TransientGlyph) || (query instanceof TransientGlyph)) {
      return;
    }

    // pre-emptively eliminate non 1D overlappers...
    Rectangle2D qbox = query.getCoordBox();
    Rectangle2D tbox = target.getCoordBox();

    if (((qbox.x + qbox.width + fuzness) <= tbox.x) || (qbox.x >= (tbox.x + tbox.width + fuzness))) {
      return;
    }

    int qcount = query.getChildCount();
    int tcount = target.getChildCount();
    if (qcount > 0) {
      // recurse into query children
      for (int i=0; i<qcount; i++) {
        GlyphI qchild = query.getChild(i);
        matchEdges(map, qchild, target, match_glyphs);
      }
    }
    else if (tcount > 0) {
      // recurse into target children
      for (int k=0; k<tcount; k++) {
        GlyphI tchild = target.getChild(k);
        matchEdges(map, query, tchild, match_glyphs);
      }
    }
    
    else if ( target.isHitable() && query.isHitable() && target.getParent() != null) {
      // terminal case, neither query nor target have children
      // see if they intersect, if so, see if edges match
      // glyph1.start == glyph2.end is _not_considered a match
      check_count++;
    
      intersect_count++;
      double qstart = qbox.x;
      double qend = qbox.x + qbox.width;
      double tstart = tbox.x;
      double tend = tbox.x + tbox.width;
      if (Math.abs(qstart - tstart) <= fuzness) {
        if (target.getParent() != null) {
          start_match_count++;
          EfficientSolidGlyph mglyph = new EfficientFillRectGlyph();
          mglyph.setHitable(false);
          mglyph.setCoords(tstart, tbox.y-1, 1, tbox.height+2);
          mglyph.setColor(col);
          // Can add mglyph to TierGlyph, or to the target or target.getParent()
          // There are advantages to each.  See note below.
          //map.addItem(target.getParent(), mglyph);
          map.addItem(getTier(target), mglyph);
          //map.addItem(mglyph); // do not do this, breaks when tiers are moved.
          match_glyphs.add(mglyph);
        }
      }

      if (Math.abs(qend - tend) <= fuzness) {
        if (target.getParent() != null) {
          end_match_count++;
          EfficientSolidGlyph mglyph = new EfficientFillRectGlyph();
          mglyph.setHitable(false);
          mglyph.setCoords(tend-1, tbox.y-1, 1, tbox.height+2);
          mglyph.setColor(col);
          //map.addItem(target.getParent(), mglyph);
          map.addItem(getTier(target), mglyph);
          //map.addItem(mglyph); // do not do this
          match_glyphs.add(mglyph);
        }
      }
      if (qstart == tend) { } // does this count? not for now...
      if (qend == tstart) { } // does this count? not for now...
      
      // NOTE:
      // Can add match glyphs directly to TierGlyph or as child of target.getParent().
      // 
      // If added to target, or target's parent:
      //   1) That parent may decide not to show the match glyphs when zoomed-out:
      //   this speeds things up, but probably you DO want to see the match glyphs
      //   at all zoom levels.
      //   2) It may be possible to speed-up looking for matches.   (But this speed-up
      //   is probably minimal because we can easily reject matching the match
      //   glyphs because they return false for isHittable.)
      //
      // If added directly to TierGlyph, the main benefit is that you can always see
      //   the matching glyphs at any zoom level.  
      //
      // Do NOT add directly to map itself.  That doesn't work properly when
      //   tiers are resized or moved.
      
    }
  }


  // Returns the tier that the glyph is in.
  // Result undefined if the glyph is not in a tier.
  GlyphI getTier(GlyphI target) {
    GlyphI p = target;
    while (p.getParent() != null && ! (p instanceof com.affymetrix.igb.tiers.TierGlyph)) {
      p = p.getParent();
    }
    return p;
  }
  
}




