/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.*;

public class EfficientExpandPacker extends ExpandPacker {
  boolean DEBUG_PACK = false;

  public Rectangle pack(GlyphI parent, ViewI view) {

    //    System.out.println("begin ExpandPacker.pack(glyph, view)");
    if (! (parent instanceof TierGlyph)) {
      throw new RuntimeException("EfficientExpandPacker can currently only work as packer for TierGlyph");
    }
    TierGlyph tier = (TierGlyph)parent;
    Vector sibs = tier.getChildren();
    if (sibs == null || sibs.size() <= 0) { return null; }  // return if nothing to pack

    GlyphI child;
    Rectangle2D cbox;
    Rectangle2D pbox = tier.getCoordBox();
    // resetting height of tier to just spacers
    tier.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);

    double ymin = Double.POSITIVE_INFINITY;
    double ymax = Double.NEGATIVE_INFINITY;
    double prev_xmax = Double.NEGATIVE_INFINITY;
    int sibs_size = sibs.size();

    for (int index=0; index<sibs_size; index++) {
      child = (GlyphI)sibs.get(index);
      cbox = child.getCoordBox();
      boolean prev_overlap = (prev_xmax > cbox.x);
      if (! (child instanceof LabelGlyph)) {
	pack(tier, child, index, view, prev_overlap);
      }
      ymin = Math.min(cbox.y, ymin);
      ymax = Math.max(cbox.y + cbox.height, ymax);
      prev_xmax = Math.max(cbox.x + cbox.width, prev_xmax);
    }

    // ensure that tier is expanded/shrunk vertically to just fit its children (+ spacers)
    //   (maybe can get rid of this, since also handled for each child pack in pack(tier, child, view))

    // move children so "top" edge (y) of top-most child (ymin) is "bottom" edge 
    //    (y+height) of bottom-most (ymax) child is at 
    sibs = tier.getChildren();
    pbox = tier.getCoordBox();

    double coord_height = ymax - ymin;
    coord_height = coord_height + (2 * parent_spacer);
    for (int i=0; i<sibs.size(); i++) {
      child = (GlyphI)sibs.elementAt(i);
      child.moveRelative(0, parent_spacer - ymin);
      //      System.out.println(child.getCoordBox());
    }

    Rectangle2D newbox = new Rectangle2D();
    Rectangle2D tempbox = new Rectangle2D();  
    child = (GlyphI)sibs.elementAt(0);
    newbox.reshape(pbox.x, child.getCoordBox().y, 
                   pbox.width, child.getCoordBox().height);
    sibs_size = sibs.size();
    if (STRETCH_HORIZONTAL && STRETCH_VERTICAL) {
      for (int i=1; i<sibs_size; i++) {
	child = (GlyphI)sibs.elementAt(i);
	GeometryUtils.union(newbox, child.getCoordBox(), newbox);
      }
    }
    else if (STRETCH_VERTICAL) {
      for (int i=1; i<sibs_size; i++) {
	child = (GlyphI)sibs.elementAt(i);
	Rectangle2D childbox = child.getCoordBox();
	tempbox.reshape(newbox.x, childbox.y, newbox.width, childbox.height);
	GeometryUtils.union(newbox, tempbox, newbox);
      }
    }
    else if (STRETCH_HORIZONTAL) {  // NOT YET TESTED
      for (int i=1; i<sibs_size; i++) {
	child = (GlyphI)sibs.elementAt(i);
	Rectangle2D childbox = child.getCoordBox();
	tempbox.reshape(childbox.x, newbox.y, childbox.width, newbox.height);
	GeometryUtils.union(newbox, tempbox, newbox);
      }
    }
    newbox.y = newbox.y - parent_spacer;
    newbox.height = newbox.height + (2 * parent_spacer);

    // trying to transform according to tier's internal transform  
    //   (since packing is done base on tier's children)
    if (tier instanceof TransformTierGlyph)  {
      TransformTierGlyph transtier = (TransformTierGlyph)tier;
      LinearTransform tier_transform = transtier.getTransform();
      tier_transform.transform(newbox, newbox);
    }

    tier.setCoords(newbox.x, newbox.y, newbox.width, newbox.height);
    //    System.out.println("packed tier, coords are: " + tier.getCoordBox());
    //    System.out.println("end ExpandPacker.pack(glyph, view)");
    return null;
  }

  /**
   *  like pack(parent, child, view, avoid_sibs), except 
   *     uses search ability of TierGlyph to speed up collection of sibs that overlap, 
   *     and also requires index of child in parent to optimize
   * packs a child.
   * This adjusts the child's offset
   * until it no longer reports hitting any of it's siblings.
   */
  public Rectangle pack(GlyphI parent, GlyphI child, int child_index, 
			ViewI view, boolean avoid_sibs) {
    TierGlyph tier = (TierGlyph)parent;
    boolean test_tier = tier.getLabel().startsWith("test");
    //    System.out.println("packing child: " + child);
    Rectangle2D childbox, siblingbox;
    Rectangle2D pbox = parent.getCoordBox();
    childbox = child.getCoordBox();
    if (movetype == UP) {
      //      System.out.println("moving up");
      child.moveAbsolute(childbox.x,
                         pbox.y + pbox.height - childbox.height - parent_spacer);
    }
    else {  
      // assuming if movetype != UP then it is DOWN 
      //    (ignoring LEFT, RIGHT, MIRROR_VERTICAL, etc. for now)
      //      System.out.println("moving down");
      child.moveAbsolute(childbox.x, pbox.y+parent_spacer);
    }
    childbox = child.getCoordBox();
    if (DEBUG_PACK && test_tier)  { System.out.println("packing glyph: " + childbox); }
    java.util.List sibsinrange = null;
    boolean childMoved = true;
    if (avoid_sibs) {
      sibsinrange = tier.getPriorOverlaps(child_index);
      if (sibsinrange == null) { childMoved = false; }
      if (DEBUG_PACK && test_tier)  { System.out.println("sibs in range: " + sibsinrange.size()); }
      this.before.x = childbox.x;
      this.before.y = childbox.y;
      this.before.width = childbox.width;
      this.before.height = childbox.height;
    }
    else {
      childMoved = false;
    }
    while (childMoved) {
      childMoved = false;
      int sibsinrange_size = sibsinrange.size();
      for (int j=0; j<sibsinrange_size; j++) {
        GlyphI sibling = (GlyphI)sibsinrange.get(j);
        if (sibling == child) { continue; }
        siblingbox = sibling.getCoordBox();
	if (DEBUG_PACK && test_tier)  { System.out.println("checking against: " + sibling); }
        if (child.hit(siblingbox, view) ) {
	  if (DEBUG_CHECKS)  { System.out.println("hit sib"); }
	  Rectangle2D cb = child.getCoordBox();
	  this.before.x = cb.x;
	  this.before.y = cb.y;
	  this.before.width = cb.width;
	  this.before.height = cb.height;
	  moveToAvoid(child, sibling, movetype);
	  childMoved |= ! before.equals(child.getCoordBox()); 
        }
      }
    }

    // adjusting tier bounds to encompass child (plus spacer)
    // maybe can get rid of this now?
    //   since also handled in pack(parent, view)
    childbox = child.getCoordBox();
    //     if first child, then shrink to fit...
    if (parent.getChildren().size() <= 1) {
      pbox.y = childbox.y - parent_spacer;
      pbox.height = childbox.height + 2 * parent_spacer;
    }
    else {
      if (pbox.y > (childbox.y - parent_spacer)) {
        double yend = pbox.y + pbox.height;
        pbox.y = childbox.y - parent_spacer;
        pbox.height = yend - pbox.y; 
      }
      if ((pbox.y+pbox.height) < (childbox.y + childbox.height + parent_spacer)) {
        double yend = childbox.y + childbox.height + parent_spacer;
        pbox.height = yend - pbox.y;
      }
    }

    return null;
  }


  /**
   * packs a child.
   * This adjusts the child's offset
   * until it no longer reports hitting any of it's siblings.
   */
  public Rectangle pack(GlyphI parent, GlyphI child, 
			ViewI view, boolean avoid_sibs) {
    TierGlyph tier = (TierGlyph)parent;
    //    System.out.println("packing child: " + child);
    Rectangle2D childbox, siblingbox;
    Rectangle2D pbox = parent.getCoordBox();
    childbox = child.getCoordBox();
    if (movetype == UP) {
      //      System.out.println("moving up");
      child.moveAbsolute(childbox.x,
                         pbox.y + pbox.height - childbox.height - parent_spacer);
    }
    else {  
      // assuming if movetype != UP then it is DOWN 
      //    (ignoring LEFT, RIGHT, MIRROR_VERTICAL, etc. for now)
      //      System.out.println("moving down");
      child.moveAbsolute(childbox.x, pbox.y+parent_spacer);
    }
    childbox = child.getCoordBox();
    java.util.List sibsinrange = null;
    boolean childMoved = true;
    Vector sibs = parent.getChildren();
    if (sibs == null) { return null; }
    if (avoid_sibs) {
      sibsinrange = new Vector();
      int sibs_size = sibs.size();
      for (int i=0; i<sibs_size; i++) {
	GlyphI sibling = (GlyphI)sibs.get(i);
	siblingbox = sibling.getCoordBox();
	if (!(siblingbox.x > (childbox.x+childbox.width) ||
	      ((siblingbox.x+siblingbox.width) < childbox.x)) ) {
	  sibsinrange.add(sibling);
	}
      }
      //      sibsinrange = tier.getIntersectedChildren(query_glyph);
      if (DEBUG_CHECKS)  { System.out.println("sibs in range: " + sibsinrange.size()); }
    
      this.before.x = childbox.x;
      this.before.y = childbox.y;
      this.before.width = childbox.width;
      this.before.height = childbox.height;
    }
    else {
      childMoved = false;
    }
    while (childMoved) {
      childMoved = false;
      int sibsinrange_size = sibsinrange.size();
      for (int j=0; j<sibsinrange_size; j++) {
        GlyphI sibling = (GlyphI)sibsinrange.get(j);
        if (sibling == child) { continue; }
        siblingbox = sibling.getCoordBox();
	if (DEBUG_CHECKS)  { System.out.println("checking against: " + sibling); }
        if (child.hit(siblingbox, view) ) {
	  if (DEBUG_CHECKS)  { System.out.println("hit sib"); }
	  Rectangle2D cb = child.getCoordBox();
	  this.before.x = cb.x;
	  this.before.y = cb.y;
	  this.before.width = cb.width;
	  this.before.height = cb.height;
	  moveToAvoid(child, sibling, movetype);
	  childMoved |= ! before.equals(child.getCoordBox()); 
        }
      }
    }

    // adjusting tier bounds to encompass child (plus spacer)
    // maybe can get rid of this now?
    //   since also handled in pack(parent, view)
    childbox = child.getCoordBox();
    //     if first child, then shrink to fit...
    if (parent.getChildren().size() <= 1) {
      pbox.y = childbox.y - parent_spacer;
      pbox.height = childbox.height + 2 * parent_spacer;
    }
    else {
      if (pbox.y > (childbox.y - parent_spacer)) {
        double yend = pbox.y + pbox.height;
        pbox.y = childbox.y - parent_spacer;
        pbox.height = yend - pbox.y; 
      }
      if ((pbox.y+pbox.height) < (childbox.y + childbox.height + parent_spacer)) {
        double yend = childbox.y + childbox.height + parent_spacer;
        pbox.height = yend - pbox.y;
      }
    }

    return null;
  }


}

