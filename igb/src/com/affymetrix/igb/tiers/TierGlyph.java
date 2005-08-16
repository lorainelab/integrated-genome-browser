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
import com.affymetrix.genoviz.util.GeometryUtils;
import com.affymetrix.genoviz.util.Timer;

/**
 *  TierGlyph is intended for use with AffyTieredMap.
 *  Each tier in the TieredNeoMap is implemented as a TierGlyph, which can have different
 *  states as indicated below.
 *  In a AffyTieredMap, TierGlyphs pack relative to each other but not to other glyphs added
 *  directly to the map.
 *
 *  Added ability to have "middleground" glyphs, which are generally not considered children of
 *    the glyph.  The TierGlyph will render these glyphs, but they can't be selected since they
 *    are not considered children in pickTraversal() method.
 *  Only way to add middleground glyphs is via addMiddleGlyph() method,
 *    only way to remove them is via removeAllChildren() method,
 *    no external access to them
 */
public class TierGlyph extends com.affymetrix.genoviz.glyph.SolidGlyph {
  // extending solid glyph to inherit hit methods (though end up setting as not hitable by default...)
  boolean DEBUG_SEARCH = false;
  boolean sorted = true;
  boolean ready_for_searching = false;
  static Comparator child_sorter = new GlyphMinComparator();
  boolean isTimed = false;
  protected com.affymetrix.genoviz.util.Timer timecheck = new com.affymetrix.genoviz.util.Timer();
  /** glyphs to be drawn in the "middleground" --
   *    in front of the solid background, but behind the child glyphs
   *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
   */
  java.util.List middle_glyphs = new ArrayList();

  public static final int HIDDEN = 100;
  public static final int COLLAPSED = 101;
  public static final int EXPANDED = 102;
  public static final int FIXED_COORD_HEIGHT = 103;
  //  public static final int SUMMARIZED = 104;

  protected int state = FIXED_COORD_HEIGHT;
  protected int stateBeforeHidden = FIXED_COORD_HEIGHT;
  protected double spacer = 2;

  // for now ignoring background color, foreground color
  //   inherited from Glyph (as a style)
  protected Color fill_color = null;
  /*
   * other_fill_color is derived from fill_color whenever setFillColor() is called
   * if there are any "middle" glyphs, then background is drawn with other_fill_color and 
   *    middle glyphs are drawn with fill_color
   * if no "middle" glyphs, then background is drawn with fill_color
   */
  protected Color other_fill_color = null;
  protected Color outline_color = null;
  protected boolean hideable = true;
  protected String label = null;

  /*
   *  Trying different packers for expand mode:
   *     ExpandPacker -- expand packer with no speed improvements
   *     ExpandPacker2  -- expand packer with xmax tracking to speed up packing when
   *                        no prior children overlap
   *     EfficientExpandPacker
   *        expand packer with optimizations for packing of sorted children
   *        in TierGlyph -- location-tuned scan
   *     FastExpandPacker
   *        expand packer with optimizations for packing of sorted children
   *        in TierGlyph when all children have same yheight
   *     FasterExpandPacker
   *        same as FastExpandPacker, but with additional optimizations for
   *        deep overlaps
   */
  //  protected PackerI expand_packer = null;
  //  protected PackerI expand_packer = new ExpandedTierPacker();
  //  protected PackerI expand_packer = new ExpandPacker();
  //  protected PackerI expand_packer = new ExpandPacker2();
  //  protected PackerI expand_packer = new EfficientExpandPacker();
  //  protected PackerI expand_packer = new FastExpandPacker();
  protected PackerI expand_packer = new FasterExpandPacker();
  protected PackerI collapse_packer = new CollapsePacker();
  //  protected PackerI summarize_packer = new SummarizePacker();

  protected java.util.List max_child_sofar = null;

  public TierGlyph() {
    state = 0; // do this so that setState() will work.
    setState(EXPANDED);
    setSpacer(spacer);
    setHitable(false);
  }

  public void addMiddleGlyph(GlyphI gl) {
    middle_glyphs.add(gl);
  }

  public void initForSearching() {
    int child_count = getChildCount();
    if (child_count > 0) {
      sortChildren(true);  // forcing sort
      //    sortChildren(false); // not forcing sort (relying on sorted field instead...)

      // now construct the max list, which is:
      //   for each entry in min sorted children list, the maximum max
      //     value up to (and including) that position
      // could do max list as int array or as symmetry list, for now doing symmetry list
      max_child_sofar = new ArrayList(child_count);
      GlyphI curMaxChild = getChild(0);
      Rectangle2D curbox = curMaxChild.getCoordBox();
      double max = curbox.x + curbox.width;
      for (int i=0; i<child_count; i++) {
	GlyphI child = this.getChild(i);
	curbox = child.getCoordBox();
	double newmax = curbox.x + curbox.width;
	if (newmax > max) {
	  curMaxChild = child;
	  max = newmax;
	}
	max_child_sofar.add(curMaxChild);
      }
    }
    else {
      max_child_sofar = null;
    }

    if (DEBUG_SEARCH && (label.startsWith("test"))) {
      System.out.println("***** called TierGlyph.initForSearch() on tier: " + getLabel());
      for (int i=0; i<child_count; i++) {
	GlyphI curchild = getChild(i);
	GlyphI maxchild = (GlyphI)max_child_sofar.get(i);
	double max = maxchild.getCoordBox().x + maxchild.getCoordBox().width;
	System.out.println("child " + i + ", min = " + getChild(i).getCoordBox().x +
			   ", max including child: " + max);
      }
    }

    ready_for_searching = true;
  }


  public void addChild(GlyphI glyph, int position) {
    throw new RuntimeException("TierGlyph.addChild(glyph, position) not allowed, " +
			       "use TierGlyph.addChild(glyph) instead");
  }

  // overriding addChild() to keep track of whether children are sorted in child vector
  //    by ascending min
  public void addChild(GlyphI glyph) {
    int count = this.getChildCount();
    if (count <= 0) {
      sorted = true;
    }
    else if (glyph.getCoordBox().x < this.getChild(count-1).getCoordBox().x) {
      sorted = false;
    }
    super.addChild(glyph);
  }

  /**
   *  return a list of all children _prior_ to query_index in child list that
   *    overlap (along x) the child at query_index.
   *  assumes that child list is already sorted by ascending child.getCoordBox().x
   *      and that max_child_sofar list is also populated
   *      (via TierGlyph.initForSearching() call)
   */
  public java.util.List getPriorOverlaps(int query_index) {
    if ((! ready_for_searching)  || (! sorted)) {
      throw new RuntimeException("must call TierGlyph.initForSearching() before " +
				 "calling TierGlyph.getPriorOverlaps");
    }
    int child_count = getChildCount();
    if (child_count <= 1) { return null; }

    double query_min = getChild(query_index).getCoordBox().x;
    int cur_index = query_index;

    while (cur_index > 0) {
      cur_index--;
      GlyphI cur_max_glyph = (GlyphI)max_child_sofar.get(cur_index);
      Rectangle2D rect = cur_max_glyph.getCoordBox();
      double cur_max = rect.x + rect.width;
      if (cur_max < query_min) {
	cur_index++;
	break;
      }
    }
    if (cur_index == query_index) { return null; }

    ArrayList result = new ArrayList();
    for (int i=cur_index; i<query_index; i++) {
      GlyphI child = getChild(i);
      Rectangle2D rect = child.getCoordBox();
      double max = rect.x + rect.width;
      if (max >= query_min) {
	result.add(child);
      }
    }
    return result;
  }


  public void sortChildren(boolean force) {
    int child_count = this.getChildCount();
    if (((! sorted) || force) && (child_count > 0)) {
      // make sure child symmetries are sorted by ascending min along search_seq
      // to avoid unecessary sort, first go through child list and see if it's
      //     already in ascending order -- if so, then no need to sort
      //     (not sure if this is necessary -- Collections.sort() may already
      //        be optimized to catch this case)
      sorted = true;
      //      int prev_min = Integer.MIN_VALUE;
      double prev_min = Double.NEGATIVE_INFINITY;
      for (int i=0; i<child_count; i++) {
	GlyphI child = (GlyphI)getChild(i);
	// int min = child.getCoordBox().x;
	double min = child.getCoordBox().x;
	if (prev_min > min) {
	  sorted = false;
	  break;
	}
	prev_min = min;
      }
      if (! sorted) {
	Collections.sort(children, child_sorter);
      }
    }
    sorted = true;
  }

  public boolean isSorted() {
    return sorted;
  }

  public void setLabel(String str) {
    label = str;
  }

  public String getLabel() {
    return label;
  }

  // overriding pack to ensure that tier is always the full width of the scene
  public void pack(ViewI view) {
    if (isTimed) { timecheck.start(); }
    int cycles = 1;
    for (int i=0; i<cycles; i++) {
      initForSearching();
      super.pack(view);
      Rectangle2D mbox = scene.getCoordBox();
      Rectangle2D cbox = this.getCoordBox();
      this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
      //    if (isTimed && label.startsWith("whatever (+)")) {
    }
    if (isTimed) {
      long tim = timecheck.read();
      System.out.println("######## time to pack " + label + ": " + tim/cycles);
    }
  }

  /**
   *  Modifying draw method to allow background shading by a collection of non-child
   *    "middleground" glyphs.  These are rendered after the solid background but before
   *    all of the children (which could be considered the "foreground");
   */
  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    pixelbox.width = Math.max ( pixelbox.width, min_pixels_width );
    pixelbox.height = Math.max ( pixelbox.height, min_pixels_height );

    Graphics g = view.getGraphics();
    Rectangle vbox = view.getPixelBox();
    pixelbox = GeometryUtils.intersection(vbox, pixelbox, pixelbox);

    if (middle_glyphs.size() == 0) { // no middle glyphs, so use fill color to fill entire tier
      if (fill_color != null) {
	g.setColor(fill_color);      
	g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }
    else {  
      // there are middle glyphs, so use other_fill_color to fill entire tier, 
      //   and fill_color to color middle glyphs
      if (other_fill_color != null) {
	g.setColor(other_fill_color);      
	g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }

    // cycle through "middleground" glyphs,
    //   make sure their coord box y and height are set to same as TierGlyph,
    //   then call mglyph.draw(view)
    int mcount = middle_glyphs.size();
    for (int i=0; i<mcount; i++) {
      GlyphI mglyph = (GlyphI)middle_glyphs.get(i);
      Rectangle2D mbox = mglyph.getCoordBox();
      mbox.reshape(mbox.x, coordbox.y, mbox.width, coordbox.height);
      mglyph.setColor(fill_color);
      mglyph.drawTraversal(view);
    }
    if (outline_color != null) {
      g.setColor(outline_color);
      g.drawRect(pixelbox.x, pixelbox.y,
		 pixelbox.width-1, pixelbox.height);
    }
    // not messing with clipbox until need to
    //        g.setClip ( oldClipbox );

    super.draw(view);
  }

  /**
   *  Remove all children of the glyph
   *
   */
  public void removeAllChildren() {
    super.removeAllChildren();
    // also remove all middleground glyphs
    // this is currently the only place where middleground glyphs are treated as if they were children
    //   maybe should rename this method clear() or something like that...
    // only reference to middle glyphs should be in this.middle_glyphs, so should be able to GC them by
    //     clearing middle_glyphs
    middle_glyphs.clear();
  }

  /* GAH removed this method, should be able to call removeAllChildren() (inherited from Glyph/GlyphI)
  public void removeChildren() {
    Vector kids = this.getChildren();
    if (kids != null) {
      for (int i=0; i < kids.size(); i++)
        this.removeChild((GlyphI)kids.elementAt(i));
    }
  }
  */

  public void setState(int newstate) {
    // terminate any pingponging if state is already same
    if (state == newstate) {
      return;
    }
    // if state is unrecognized, do not change state
    if (! (newstate == COLLAPSED || newstate == EXPANDED ||
	   newstate == HIDDEN || newstate == FIXED_COORD_HEIGHT  // ||
	   // 	   newstate == SUMMARIZED
	   ) ) {
      System.out.println("state not recognized: " + newstate);
      return;
    }
    if (newstate == HIDDEN)  {
      stateBeforeHidden = state; // used by restoreState();
    }
    state = newstate;
    if (state == EXPANDED) {
      setPacker(expand_packer);
      setVisibility(true);
    }
    else if (state == COLLAPSED) {
      setPacker(collapse_packer);
      setVisibility(true);
    }
    else if (state == HIDDEN) {
      setPacker(null);
      setVisibility(false);
    }
    /*
    else if (state == SUMMARIZED) {
      System.out.println("setting state to summarize");
      setPacker(summarize_packer);
      setVisibility(true);
    }
    */
    else if (state == FIXED_COORD_HEIGHT)  {
      setPacker(null);
      setVisibility(true);
    }
  }


  /** If the state==HIDDEN, restore the glyph to the state it was in before
      it was hidden. Else do nothing. */
  public void restoreState() {
    if (state==HIDDEN) setState(stateBeforeHidden);
  }

  public int getState() {
    return state;
  }

  public PackerI getExpandedPacker() {
    return expand_packer;
  }

  public PackerI getCollapsedPacker() {
    return collapse_packer;
  }

  public void setExpandedPacker(PackerI packer) {
    this.expand_packer = packer;
    setSpacer(getSpacer());
  }

  public void setCollapsedPacker(PackerI packer) {
    this.collapse_packer = packer;
    setSpacer(getSpacer());
  }

  public void setSpacer(double spacer) {
    this.spacer = spacer;
    if (collapse_packer instanceof PaddedPackerI) {
      ((PaddedPackerI)collapse_packer).setParentSpacer(spacer);
    }
    if (expand_packer instanceof PaddedPackerI) {
      ((PaddedPackerI)expand_packer).setParentSpacer(spacer);
    }
  }

  public double getSpacer() {
    return spacer;
  }

  /** Sets the color used to draw the outline.
   *  @param col A color, or null if no outline is desired.
   */
  public void setOutlineColor(Color col) {
    outline_color = col;
  }

  /** Returns the color used to draw the outline, or null
      if there is no outline. */
  public Color getOutlineColor() {
    return outline_color;
  }


  /** Sets the color used to fill the tier background, or null if no color
   *  @param col  A color, or null if no background color is desired.
   */
  public void setFillColor(Color col) {
    fill_color = col;
    // for now, assume "middleground" color is based on fill color
    int intensity = col.getRed() + col.getGreen() + col.getBlue();
    if (intensity == 0) { other_fill_color = Color.darkGray; }
    else if (intensity > (255+127)) { other_fill_color = col.darker(); }
    else { other_fill_color = col.brighter(); }
    //    other_fill_color = Color.lightGray;
    //    other_fill_color = fill_color.brighter().brighter().brighter();
  }

  /** Returns the color used to draw the tier background, or null
      if there is no background. */
  public Color getFillColor() {
    return fill_color; 
  }

  /** Set whether or not the tier wants to allow itself to be hidden;
   *  The state of this flag has no effect on whether setState(HIDDEN)
   *  will work or not.
   */
  public void setHideable(boolean h) {
    this.hideable = h;
  }

  /** Get whether or not the tier wants to allow itself to be hidden;
   *  The state of this flag has no effect on whether setState(HIDDEN)
   *  will work or not.
   */
  public boolean isHideable() {
    return this.hideable;
  }

  /** Returns a string representing the state of this object.
      @see #setState */
  public String getStateString() {
    return getStateString(getState());
  }

  /** Converts the given state constant into a human-readable string.
      @see #setState */
  public static String getStateString(int astate) {
    if (astate == HIDDEN) { return "HIDDEN"; }
    else if (astate == COLLAPSED) { return "COLLAPSED"; }
    else if (astate == EXPANDED) { return "EXPANDED"; }
    else if (astate == FIXED_COORD_HEIGHT) { return "FIXED_COORD_HEIGHT"; }
    else { return "UNKNOWN"; }
  }

}
