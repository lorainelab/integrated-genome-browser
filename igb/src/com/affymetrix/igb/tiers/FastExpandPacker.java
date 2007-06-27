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

import java.awt.Rectangle;
import java.util.*;

import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.GeometryUtils;
import com.affymetrix.genoviz.bioviews.*;

import com.affymetrix.genometryImpl.util.DoubleList;

/**
 *  A new packer for laying out children of a TierGlyph.
 * <pre>
 *  The successor to EfficientExpandPacker, which is in turn the successor to 
 *  ExpandPacker.  FastExpandPacker should be much faster (nearly linear time), 
 *  but will only work provided that certain conditions are met:
 *     1. The list of children being packed into a parent must be sorted by 
 *          ascending min (child.getCoordBox().x)
 *     2. All children must be the same height (child.getCoordBox().height);
 *  Basic idea is that since all children are the same height, there is a discreet 
 *     number of y-position slots that a child can occupy.  Therefore, when packing 
 *     all the children, one can sweep through the sorted list of children while keeping 
 *     track of the maximum x-position (x+width) of all the children in a slot/subtier, 
 *     which by definition will be the maximum x-position of the last child to occupy 
 *     that subtier, and search the slot list (which is sorted by ascending/descending 
 *     y position, depending on whether packing up or down) for one in which the current 
 *     child will fit. In pseudo-code:
 *  
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
 *
 *  I think this will execute in order (N x P)/2, where N is the number of children 
 *  and P is the number of slots that need to be created to lay out the children.  
 *  Actually (N x P)/2 is worst case performance -- unless every possible x-position 
 *  for children is overlapped by P children, should actually get much better 
 *  performance, approaching N (linear time) as the number of child overlaps approaches 0
 *
 *
 *  THE FOLLOWING IS NOT YET IMPLEMENTED
 *  A potential improvement to this layout algorithm is to also keep track of 
 *  the the _minimum_ xmax (prev_min_xmax) of all slots that were checked for the 
 *  previous child being packed (including the new xmax of the slot the prev child 
 *  was placed in), as well as the index of the slot the prev child (prev_slot_index)
 *  Then for the current child being packed:
 *      if (prev_min_xmax < child.xmin)  {
 *          there is a slot with index <= prev_slot_index that will fit child, 
 *	    so do (for each slot in tier, etc.) same as above
 *      }
 *      else  {
 *          there are no slots with index <= prev_slot_index that will fit child, 
 *          so modify (for each slot) to be 
 *          (for each slot starting at slot.index = prev_slot_index+1), then same as above
 *      }
 *  This would help performance in the problematic cases where there are many children 
 *  that overlap the same region.  Without this improvement, such cases would force 
 *  iteration over each potential slot to place each child, giving (NxP)/2 performance.  
 *  With this improvement, some of the worst cases, such as identical ranges for all 
 *  children, would actually end up with order N (linear time) performance
 *  </pre>
 */
public class FastExpandPacker extends EfficientExpandPacker 
   implements  PaddedPackerI, NeoConstants {

  // PackerI interface (via inheritance from PaddedPackerI  
  public Rectangle pack(GlyphI parent, ViewI view) {
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
    
    int slot_checks = 0;
    DoubleList slot_maxes = new DoubleList(1000);    
    double slot_height = parent.getChild(0).getCoordBox().height + (2 * spacing);
    for (int i=0; i<child_count; i++) {
      child = parent.getChild(i);
      cbox = child.getCoordBox();
      double child_min = cbox.x;
      double child_max = child_min + cbox.width;
      boolean child_placed = false;
      int slot_count = slot_maxes.size();
      for (int slot_index=0; slot_index < slot_count; slot_index++) {
	slot_checks++;
	double slot_max = slot_maxes.get(slot_index);
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
	  break;
	}
      }
      if (! child_placed) {
	double new_ycoord;
	if (this.getMoveType() == NeoConstants.UP) {
	  new_ycoord = - ((slot_maxes.size() * slot_height) + spacing);
	}
	else {
	  new_ycoord = (slot_maxes.size() * slot_height) + spacing;
	}
	child.moveAbsolute(child_min, new_ycoord);
	slot_maxes.add(child_max);
      }
      ymin = Math.min(cbox.y, ymin);
      ymax = Math.max(cbox.y + cbox.height, ymax);
    }

    System.out.println("slot checks: " + slot_checks);

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
