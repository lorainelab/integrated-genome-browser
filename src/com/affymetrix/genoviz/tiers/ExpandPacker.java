/**
*   Copyright (c) 2001-2008 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genoviz.tiers;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LabelGlyph;
import com.affymetrix.genoviz.util.GeometryUtils;
import com.affymetrix.genoviz.util.NeoConstants.Direction;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;


public class ExpandPacker implements PaddedPackerI {

  // BEGIN from AbstractCoordPacker
  protected boolean DEBUG = true;
  protected boolean DEBUG_CHECKS = false;
  protected double coord_fuzziness = 1;
  protected double spacing = 2;
  protected Direction movetype;
  protected Rectangle2D.Double before = new Rectangle2D.Double(); 
  // END from AbstractCoordPacker

  boolean STRETCH_HORIZONTAL = true;
  boolean STRETCH_VERTICAL = true;
  boolean USE_NEW_PACK = true;
  boolean use_search_nodes = false;

  /*
   *  parent_spacer is NOT the same as AbstractCoordPacker.spacing
   *  spacing is between each child
   *  parent_spacer is padding added to parent above and below the 
   *  extent of all the children
   */
    protected double parent_spacer;


  // BEGIN from AbstractCoordPacker
  /**
   * constructs a packer that moves glyphs away from the horizontal axis.
   */
  public ExpandPacker() {
    this(Direction.DOWN);
  }

  /**
   * constructs a packer with a given direction to move glyphs.
   *
   * @param movetype indicates which direction the glyph_to_move should move.
   * @see #setMoveType
   */
  public ExpandPacker(Direction movetype) {
    setMoveType(movetype);
  }

  /**
   * sets the direction this packer should move glyphs.
   *
   * @param movetype indicates which direction the glyph_to_move should move.
   *                 It must be one of UP, DOWN, LEFT, RIGHT,
   *                 MIRROR_VERTICAL, or MIRROR_HORIZONTAL.
   *                 The last two mean "away from the orthoganal axis".
   */
  public void setMoveType(Direction movetype) {
    this.movetype = movetype;
  }

  public Direction getMoveType() {
    return movetype;
  }

  /**
   *     Sets the fuzziness of hit detection in layout.
   *     This is the minimal distance glyph coordboxes need to be separated by 
   *     in order to be considered not overlapping.
   * <p> <em>WARNING: better not make this greater than spacing.</em>
   * <p> Note that since Rectangle2D does not consider two rects
   *     that only share an edge to be intersecting,
   *     will need to have a coord_fuzziness > 0
   *     in order to consider these to be overlapping.
   */
  public void setCoordFuzziness(double fuzz) {
    if (fuzz > spacing) {
      throw new IllegalArgumentException("Can't set packer fuzziness greater than spacing");
    } else {
      coord_fuzziness = fuzz;
    }
  }

  public double getCoordFuzziness() {
    return coord_fuzziness;
  }

  /**
   * Sets the spacing desired between glyphs.
   * If glyphB is found to hit glyphA,
   * this is the distance away from glyphA's coordbox
   * that glyphB's coord box will be moved.
   */
  @Override
  public void setSpacing(double sp) {
    if (sp < coord_fuzziness) {
      throw new IllegalArgumentException("Can't set packer spacing less than fuzziness");
    } else {
      spacing = sp;
    }
  }

  @Override
  public double getSpacing() {
    return spacing;
  }

  /**
   * Moves one glyph to avoid another.
   * This is called from subclasses
   * in their {@link #pack(com.affymetrix.genoviz.bioviews.GlyphI, com.affymetrix.genoviz.bioviews.GlyphI, com.affymetrix.genoviz.bioviews.ViewI)} method.
   *
   * @param glyph_to_move
   * @param glyph_to_avoid
   * @param movetype indicates which direction the glyph_to_move should move.
   * @see #setMoveType
   */
  public void moveToAvoid(GlyphI glyph_to_move, 
			  GlyphI glyph_to_avoid, Direction movetype)  {
    Rectangle2D.Double movebox = glyph_to_move.getCoordBox();
    Rectangle2D.Double avoidbox = glyph_to_avoid.getCoordBox();
    if (!movebox.intersects(avoidbox)) {
      return;
    }
    if (movetype == Direction.MIRROR_VERTICAL) {
      if (movebox.y < 0) {
        glyph_to_move.moveAbsolute(movebox.x,
          avoidbox.y - movebox.height - spacing);
      } else {
        glyph_to_move.moveAbsolute(movebox.x,
          avoidbox.y + avoidbox.height + spacing);
      }
    } else if (movetype == Direction.MIRROR_HORIZONTAL) {
      if (movebox.x < 0) {
        glyph_to_move.moveAbsolute(avoidbox.x - movebox.width - spacing,
          movebox.y);
      } else {
        glyph_to_move.moveAbsolute(avoidbox.x + avoidbox.width + spacing,
          movebox.y);
      }
    } else if (movetype == Direction.DOWN) {
      glyph_to_move.moveAbsolute(movebox.x,
        avoidbox.y + avoidbox.height + spacing);
    } else if (movetype == Direction.UP) {
      glyph_to_move.moveAbsolute(movebox.x,
        avoidbox.y - movebox.height - spacing);
    } else if (movetype == Direction.RIGHT) {
      glyph_to_move.moveAbsolute(avoidbox.x + avoidbox.width + spacing,
        movebox.y);
    } else if (movetype == Direction.LEFT) {
      glyph_to_move.moveAbsolute(avoidbox.x - movebox.width - spacing,
        movebox.y);
    } else {
      throw new IllegalArgumentException("movetype must be one of UP, DOWN, LEFT, RIGHT, MIRROR_HORIZONTAL, or MIRROR_VERTICAL");
    }
  }

  @Override
  public void setParentSpacer(double spacer) {
    this.parent_spacer = spacer;
  }

  @Override
  public double getParentSpacer() {
    return parent_spacer;
  }

  public void setStretchHorizontal(boolean b) {
    STRETCH_HORIZONTAL = b;
  }
  
  public boolean getStretchHorizontal(boolean b) {
    return STRETCH_HORIZONTAL;
  }

  @Override
  public Rectangle pack(GlyphI parent, ViewI view) {
    List<GlyphI> sibs = parent.getChildren();
    GlyphI child;
    Rectangle2D.Double cbox;
    Rectangle2D.Double pbox = parent.getCoordBox();

    // resetting height of parent to just spacers
    //    parent.setCoords(pbox.x, pbox.y, pbox.width, 2 * parent_spacer);
    parent.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);

    if (sibs == null || sibs.size() <= 0) { 
      return null; 
    }

    double ymin = Float.MAX_VALUE;
    double ymax = Float.MIN_VALUE;
    double prev_xmax = Float.MIN_VALUE;
    
    // trying synchronization to ensure this method is threadsafe
      synchronized(sibs) {  // testing synchronizing on sibs list...
	int sibs_size = sibs.size();
	GlyphI[] sibarray = new GlyphI[sibs_size];
        sibarray = sibs.toArray(sibarray);
	sibs.clear();
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
	  sibs.add(child);  // add children back in one at a time
	  if (! (child instanceof LabelGlyph)) {
	    pack(parent, child, view, true);
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
      child = sibs.get(i);
      child.moveRelative(0, parent_spacer - ymin);
      //      System.out.println(child.getCoordBox());
    }

    // old implementation
    Rectangle2D.Double newbox = new Rectangle2D.Double();
    Rectangle2D.Double tempbox = new Rectangle2D.Double();  
    child = sibs.get(0);
    newbox.setRect(pbox.x, child.getCoordBox().y, 
                   pbox.width, child.getCoordBox().height);
    int sibs_size = sibs.size();
    if (STRETCH_HORIZONTAL && STRETCH_VERTICAL) {
      for (int i=1; i<sibs_size; i++) {
	child = sibs.get(i);
	GeometryUtils.union(newbox, child.getCoordBox(), newbox);
      }
    }
    else if (STRETCH_VERTICAL) {
      for (int i=1; i<sibs_size; i++) {
	child = sibs.get(i);
	final Rectangle2D.Double childbox = child.getCoordBox();
	tempbox.setRect(newbox.x, childbox.y, newbox.width, childbox.height);
	GeometryUtils.union(newbox, tempbox, newbox);
      }
    }
    else if (STRETCH_HORIZONTAL) {  // NOT YET TESTED
      for (int i=1; i<sibs_size; i++) {
	child = sibs.get(i);
	final Rectangle2D.Double childbox = child.getCoordBox();
	tempbox.setRect(childbox.x, newbox.y, childbox.width, newbox.height);
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

  
  @Override
  public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) {
    return pack(parent, child, view, true);
  }

  /**
   * packs a child.
   * This adjusts the child's offset
   * until it no longer reports hitting any of it's siblings.
   */
  public Rectangle pack(GlyphI parent, GlyphI child, 
			ViewI view, boolean avoid_sibs) {
    //    System.out.println("packing child: " + child);
    Rectangle2D.Double childbox, siblingbox;
    Rectangle2D.Double pbox = parent.getCoordBox();
    childbox = child.getCoordBox();
    if (movetype == Direction.UP) {
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
    java.util.List<GlyphI> sibs = parent.getChildren();
    if (sibs == null) { return null; }
    java.util.List<GlyphI> sibsinrange = null;
    boolean childMoved = true;
    if (avoid_sibs) {
      sibsinrange = new ArrayList<GlyphI>();
      int sibs_size = sibs.size();
      for (int i=0; i<sibs_size; i++) {
	GlyphI sibling = sibs.get(i);
	siblingbox = sibling.getCoordBox();
	if (!(siblingbox.x > (childbox.x+childbox.width) ||
	      ((siblingbox.x+siblingbox.width) < childbox.x)) ) {
	  sibsinrange.add(sibling);
	}
      }
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
        GlyphI sibling = sibsinrange.get(j);
        if (sibling == child) { continue; }
        siblingbox = sibling.getCoordBox();
	if (DEBUG_CHECKS)  { System.out.println("checking against: " + sibling); }
        if (child.hit(siblingbox, view) ) {
	  if (DEBUG_CHECKS)  { System.out.println("hit sib"); }
            this.before.setRect(child.getCoordBox());
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

