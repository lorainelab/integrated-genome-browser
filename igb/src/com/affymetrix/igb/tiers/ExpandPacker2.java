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

public class ExpandPacker2 extends ExpandPacker {

  public Rectangle pack(GlyphI parent, ViewI view) {
    //    System.out.println("begin ExpandPacker2.pack(glyph, view)");
    Vector sibs = parent.getChildren();
    GlyphI child;
    Rectangle2D cbox;
    Rectangle2D pbox = parent.getCoordBox();

    // resetting height of parent to just spacers
    //    parent.setCoords(pbox.x, pbox.y, pbox.width, 2 * parent_spacer);
    parent.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);

    if (sibs == null || sibs.size() <= 0) { 
      //      System.out.println("end ExpandPacker2.pack(glyph, view)");
      return null; 
    }

    double ymin = Float.MAX_VALUE;
    double ymax = Float.MIN_VALUE;
    double prev_xmax = Float.MIN_VALUE;
    
    // trying synchronization to ensure this method is threadsafe
      synchronized(sibs) {  // testing synchronizing on sibs vector...
	GlyphI[] sibarray = new GlyphI[sibs.size()];
	sibs.copyInto(sibarray);
	sibs.removeAllElements(); // sets parent.getChildren() to empty Vector
	int sibs_size = sibarray.length;
	//	System.out.println("packing each child");
	for (int i=0; i<sibs_size; i++) {
	  child = sibarray[i];
	  cbox = child.getCoordBox();
	  // a quick hack to speed up packing when there are no (or few overlaps) -- 
	  // keep track of max x coord of previous sibs -- 
	  //   if prev_xmax < current glyph's min x, then there won't be any overlap, 
	  //   so can tell pack() to skip check against previous sibs
	  boolean prev_overlap = (prev_xmax >  cbox.x);
	  //	  if (i < 100) { System.out.println("overlap: " + prev_overlap + 
	  //					    ", parent = " + parent); }
	  sibs.addElement(child);  // add children back in one at a time
	  if (! (child instanceof LabelGlyph)) {
	    pack(parent, child, view, prev_overlap);
	  }
	  ymin = Math.min(cbox.y, ymin);
	  ymax = Math.max(cbox.y + cbox.height, ymax);
	  prev_xmax = Math.max(cbox.x + cbox.width, prev_xmax);
	  if (DEBUG_CHECKS)  { System.out.println(child); }
	}
	//	System.out.println("finished packing each child");
      }

      //    System.out.println("ymin for children = " + ymin);
      //    System.out.println("ymax for children = " + ymax);

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
    //    System.out.println("end ExpandPacker2.pack(glyph, view)");
    return null;
  }

}

