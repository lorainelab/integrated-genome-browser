/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import java.awt.Rectangle;
import java.util.*;

import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.GeometryUtils;
import com.affymetrix.genoviz.bioviews.*;

import com.affymetrix.genometryImpl.util.DoubleList;

/**
 *
 *  A new packer for laying out children of a TierGlyph.
 *  <p>
 *  The successor to EfficientExpandPacker, which is in turn the successor to
 *  ExpandPacker.  FasterExpandPacker should be much faster (nearly linear time),
 *  but will only work provided that certain conditions are met:
 *     1. The list of children being packed into a parent must be sorted by
 *          ascending min (child.getCoordBox().x)
 *     2. All children must be the same height (child.getCoordBox().height);
 *        If children are different heights, you *must* indicate this by calling
 *        setConstantHeights(false).  This will then act as if all glyphs
 *        are the same height as the one with the maximum height, thus leaving
 *        more blank vertical space than is necessary, but allowing the packing
 *        to remain fast.
 *  (To meet requirement (1), call TierGlyph.pack() forces a sorting of the tier
 *        if it is not sorted in ascending min)
 *  </p>
 *
 * <p>
 *  Basic idea is that since all children are the same height, there is a discreet
 *     number of y-position slots that a child can occupy.  Therefore, when packing
 *     all the children, one can sweep through the sorted list of children while keeping
 *     track of the maximum x-position (x+width) of all the children in a slot/subtier,
 *     which by definition will be the maximum x-position of the last child to occupy
 *     that subtier, and search the slot list (which is sorted by ascending/descending
 *     y position, depending on whether packing up or down) for one in which the current
 *     child will fit. In pseudo-code:
 *  <code><pre>
 *  for each child in tier.getChildren()  {
 *     for each slot in tier  {
 *        if (child.xmin > slot.xmax)  {
 *            put child in slot (change child.y to slot.y + buffer)
 *            set slot.xmax = child.xmax
 *            break
 *        }
 *     }
 *     if no slot with (child.xmin > slot.xmax)  {
 *         add new slot to slot list, with position at (max(slot.y) + slot.height)
 *         put child in new slot
 *         set slot.xmax = child.xmax
 *     }
 *  }
 *  </pre></code>
 *</p>
 *
 *<p>
 *  I think this will execute in order (N x P)/2, where N is the number of children
 *  and P is the number of slots that need to be created to lay out the children.
 *  Actually (N x P)/2 is worst case performance -- unless every possible x-position
 *  for children is overlapped by P children, should actually get much better
 *  performance, approaching N (linear time) as the number of child overlaps approaches 0
 *</p>
 *
 *<p>
 *  THE FOLLOWING IS NOT YET IMPLEMENTED
 *  A potential improvement to this layout algorithm is to also keep track of
 *  the the _minimum_ xmax (prev_min_xmax) of all slots that were checked for the
 *  previous child being packed (including the new xmax of the slot the prev child
 *  was placed in), as well as the index of the slot the prev child (prev_slot_index)
 *  Then for the current child being packed:
 *<code><pre>
 *      if (prev_min_xmax < child.xmin)  {
 *          there is a slot with index <= prev_slot_index that will fit child,
 *	    so do (for each slot in tier, etc.) same as above
 *      }
 *      else  {
 *          there are no slots with index <= prev_slot_index that will fit child,
 *          so modify (for each slot) to be
 *          (for each slot starting at slot.index = prev_slot_index+1), then same as above
 *      }
 *</pre></code>
 *  This would help performance in the problematic cases where there are many children
 *  that overlap the same region.  Without this improvement, such cases would force
 *  iteration over each potential slot to place each child, giving (NxP)/2 performance.
 *  With this improvement, some of the worst cases, such as identical ranges for all
 *  children, would actually end up with order N (linear time) performance
 *</p>
 *
 *<p>
 *  GAH 8-20-2003
 *  Added ability to specify/adjust max number of slots --
 *    and if a child needs be placed in a slot > max_slots,
 *    then instead it is place at max_slots but offset slightly
 *    to visually indicate pileup
 *</p>
 */
public final class FasterExpandPacker extends EfficientExpandPacker
   implements  PaddedPackerI, NeoConstants {

  int max_slots_allowed = 1000;

  public int getMaxSlots() { return max_slots_allowed; }
  
  /**
   *  Sets the maximum depth of glyphs to pack in the tier.
   *  @param slotnum  a positive integer or zero; zero implies there is no
   *  limit to the depth of packing.  If a negative number is given, it is
   *  reset to zero.
   */
  public void setMaxSlots(int slotnum) {
    if (slotnum >= 0 ) {
      max_slots_allowed = slotnum;
    } else {
      slotnum = 0;
    }
  }

  boolean constant_heights = true;
  
  /** Set whether or not packer can assume all children glyphs are the same height. 
   *  Default is true.  If false, it will pack into layers based on the maximum 
   *  child height.
   */
  public void setConstantHeights(boolean b) {
    constant_heights = b;
  }
  
  double getMaxChildHeight(GlyphI parent) {
    double max = 0;
    if (constant_heights) {
      max = parent.getChild(0).getCoordBox().height;
    } else {
      int children = parent.getChildCount();
      for (int i=0; i<children; i++) {
        max = Math.max(parent.getChild(i).getCoordBox().height, max);
      }
    }
    return max;
  }

  // PackerI interface (via inheritance from PaddedPackerI
  @Override
  public Rectangle pack(GlyphI parent, ViewI view) {
    boolean REPORT_SLOT_CHECKS = false;
    Vector sibs = parent.getChildren();
    Rectangle2D cbox;
    GlyphI child;
    Rectangle2D pbox = parent.getCoordBox();
    // resetting height of parent to just spacers
    //    parent.setCoords(pbox.x, pbox.y, pbox.width, 2 * parent_spacer);
    parent.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);
    double ymin = Double.POSITIVE_INFINITY;
    double ymax = Double.NEGATIVE_INFINITY;

    int child_count = parent.getChildCount();
    if (child_count <= 0) { return null; }

/*  A potential improvement to this layout algorithm is to also keep track of
 *  the the _minimum_ xmax (prev_min_xmax) of all slots that were checked for the
 *  previous child being packed (including the new xmax of the slot the prev child
 *  was placed in), as well as the index of the slot the prev child was placed in
 *  (prev_slot_index)
 */

    int slot_checks = 0;
    DoubleList slot_maxes = new DoubleList(1000);

    double slot_height = getMaxChildHeight(parent) + 2 * spacing;
    
    double prev_min_xmax = Double.POSITIVE_INFINITY;
    // min_xmax_slot_index is index of slot with max of prev_min_xmax
    int min_xmax_slot_index = 0;
    int prev_slot_index = 0;
    int optimize2_count = 0;

    for (int i=0; i<child_count; i++) {
      child = parent.getChild(i);
      child.setVisibility(true);
      cbox = child.getCoordBox();
      double child_min = cbox.x;
      double child_max = child_min + cbox.width;
      boolean child_placed = false;
      int start_slot_index = 0;
      if (prev_min_xmax >= child_min) {
	// no point in checking slots prior to and including prev_slot_index, so
	//  modify start_slot_index to be prev_slot_index++;
	start_slot_index = prev_slot_index + 1;
	//	System.out.println("prev_min_xmax = " + prev_min_xmax + ", child_min = " + child_min);
	optimize2_count++;
      }
      int slot_count = slot_maxes.size();
      //      if (start_slot_index != 0) {
      //	System.out.println("start_slot_index = " + start_slot_index +
      //			   ", slot_count = " + slot_count);
      //      }
      for (int slot_index=start_slot_index; slot_index < slot_count; slot_index++) {
	slot_checks++;
	double slot_max = slot_maxes.get(slot_index);
	if (slot_max < prev_min_xmax) {
	  min_xmax_slot_index = slot_index;
	  prev_min_xmax = slot_max;
	}
	//	prev_min_xmax = Math.min(prev_min_xmax, slot_max);
	if (slot_max < child_min) {
	  // move child to slot;
	  double new_ycoord;
	  if (this.getMoveType() == NeoConstants.UP) {  // stacking up for layout
	    new_ycoord = - ((slot_index * slot_height) + spacing);
	  }
	  else { // stacking down for layout
	    new_ycoord = (slot_index * slot_height) + spacing;
	  }
	  child.moveAbsolute(child_min, new_ycoord);
	  child_placed = true;
	  slot_maxes.set(slot_index, child_max);
	  prev_slot_index = slot_index;
	  if (slot_index == min_xmax_slot_index) {
	    prev_slot_index = 0;
	    min_xmax_slot_index = 0;
	    prev_min_xmax = slot_maxes.get(0);
	  }
	  else if (child_max < prev_min_xmax) {
	    prev_min_xmax = child_max;
	    min_xmax_slot_index = slot_index;
	  }
	  break;
	}
      }
      if (! child_placed) {
	double new_ycoord;
	// make new slot for child (unless already have max number of slots allowed,
	//   in which case layer at top/bottom depending on movetype
	if ((max_slots_allowed > 0) && slot_maxes.size() >= max_slots_allowed) {
          child.setVisibility(true);
	  if (this.getMoveType() == NeoConstants.UP) {
	    new_ycoord = - (((slot_maxes.size()) * slot_height) + spacing);
	  }
	  else {
	    new_ycoord = ((slot_maxes.size()) * slot_height) + spacing;
	  }
	  child.moveAbsolute(child_min, new_ycoord);
	  int slot_index = slot_maxes.size() - 1;
	  prev_slot_index = slot_index;
	}
	else {
	  child.setVisibility(true);
	  if (this.getMoveType() == NeoConstants.UP) {
	    new_ycoord = - ((slot_maxes.size() * slot_height) + spacing);
	  }
	  else {
	    new_ycoord = (slot_maxes.size() * slot_height) + spacing;
	  }
	  child.moveAbsolute(child_min, new_ycoord);
	  slot_maxes.add(child_max);
	  int slot_index = slot_maxes.size() - 1;
	  // prev_min_xmax = Math.min(prev_min_xmax, child_max);
	  if (child_max < prev_min_xmax) {
	    min_xmax_slot_index = slot_index;
	    prev_min_xmax = child_max;
	  }
	  prev_slot_index = slot_index;
	}
      }
      ymin = Math.min(cbox.y, ymin);
      ymax = Math.max(cbox.y + cbox.height, ymax);
    }

    if (REPORT_SLOT_CHECKS) {
      System.out.println("slot checks: " + slot_checks);
      System.out.println("optimize2 count: " + optimize2_count);
    }

    //    System.out.println("prev_min_xmax = " + prev_min_xmax +
    //		       ", min_xmax_slot_index = " + min_xmax_slot_index +
    //		       ", prev_slot_index = " + prev_slot_index);

    /*
     *  now that child packing is done, need to ensure
     *  that parent is expanded/shrunk vertically to just fit its
     *  children, plus spacers above and below
     *
     *  maybe can get rid of this, since also handled for each child pack
     *     in pack(parent, child, view);
     *
     */
    // move children so "top" edge (y) of top-most child (ymin) is "bottom" edge
    //    (y+height) of bottom-most (ymax) child is at

    sibs = parent.getChildren();
    pbox = parent.getCoordBox();

    double coord_height = ymax - ymin;
    coord_height = coord_height + (2 * parent_spacer);
    for (int i=0; i<sibs.size(); i++) {
      child = (GlyphI)sibs.elementAt(i);
      child.moveRelative(0, parent_spacer - ymin);
      //      System.out.println(child.getCoordBox());
    }

    // old implementation
    Rectangle2D newbox = new Rectangle2D();
    Rectangle2D tempbox = new Rectangle2D();
    child = (GlyphI)sibs.elementAt(0);
    newbox.reshape(pbox.x, child.getCoordBox().y,
                   pbox.width, child.getCoordBox().height);
    int sibs_size = sibs.size();
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
    if (parent instanceof TransformTierGlyph)  {
      TransformTierGlyph transtier = (TransformTierGlyph)parent;
      LinearTransform tier_transform = transtier.getTransform();
      tier_transform.transform(newbox, newbox);
    }

    parent.setCoords(newbox.x, newbox.y, newbox.width, newbox.height);
    //    System.out.println("packed tier, coords are: " + parent.getCoordBox());
    //    System.out.println("end ExpandPacker.pack(glyph, view)");

    return null;
  }

}
