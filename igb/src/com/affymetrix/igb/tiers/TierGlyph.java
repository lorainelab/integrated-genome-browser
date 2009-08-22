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

import com.affymetrix.genometryImpl.style.*;
import com.affymetrix.igb.glyph.GraphGlyph;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.*;
import java.util.List;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.GeometryUtils;
import java.awt.geom.Rectangle2D;

/**
 *  TierGlyph is intended for use with AffyTieredMap.
 *  Each tier in the TieredNeoMap is implemented as a TierGlyph, which can have different
 *  states as indicated below.
 *  In a AffyTieredMap, TierGlyphs pack relative to each other but not to other glyphs added
 *  directly to the map.
 *
 */
public class TierGlyph extends SolidGlyph {
  // extending solid glyph to inherit hit methods (though end up setting as not hitable by default...)
  boolean DEBUG_SEARCH = false;
  boolean sorted = true;
  boolean ready_for_searching = false;
  static Comparator<GlyphI> child_sorter = new GlyphMinComparator();
  boolean isTimed = false;
  int direction = DIRECTION_NONE;

  protected com.affymetrix.genoviz.util.Timer timecheck = new com.affymetrix.genoviz.util.Timer();

  /** glyphs to be drawn in the "middleground" --
   *    in front of the solid background, but behind the child glyphs
   *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
   */
  List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();

  public static final int HIDDEN = 100;
  public static final int COLLAPSED = 101;
  public static final int EXPANDED = 102;
  public static final int FIXED_COORD_HEIGHT = 103;

  public static final int DIRECTION_FORWARD = +1;
  public static final int DIRECTION_NONE = 0;
  public static final int DIRECTION_REVERSE = -1;
  public static final int DIRECTION_BOTH = 2;

  /** Use this direction for axis tiers, so they can be recognized as a
   *  special case when sorting tiers.
   */
  public static final int DIRECTION_AXIS = -2;

  /** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
   *  Boolean.TRUE, the tier will draw a label next to where the handle
   *  would be.
   *  Note: You probably do NOT want the TierGlyph to draw a label and for the
   *  included GraphGlyph to also draw a label.
   */
  public static final String SHOW_TIER_LABELS_PROPERTY = "Show Tier Labels";

  /** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
   *  Boolean.TRUE, the tier will draw a handle on the left side.
   *  Note: You probably do NOT want the TierGlyph to draw a handle and for the
   *  included GraphGlyph to also draw a handle.
   */
  public static final String SHOW_TIER_HANDLES_PROPERTY = "Show Tier Handles";

  protected double spacer = 2;

  /*
   * other_fill_color is derived from fill_color whenever setFillColor() is called.
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

  protected List<GlyphI> max_child_sofar = null;

  IAnnotStyle style;

  public TierGlyph(IAnnotStyle style) {
    setHitable(false);
    setSpacer(spacer);
    setStyle(style);
  }

  /** Constructor that generates a default IAnnotStyle. */
  public TierGlyph() {
    this(new DefaultIAnnotStyle());
  }

  public void setStyle(IAnnotStyle style) {
    this.style = style;

    if (style != null) {
      // most tier glyphs ignore their foreground color, but AffyTieredLabelMap copies
      // the fg color to the TierLabel glyph, which does pay attention to that color.
      setForegroundColor(style.getColor());
      setFillColor(style.getBackground());

      if (style.getCollapsed()) {
        setPacker(collapse_packer);
      } else {
        setPacker(expand_packer);
      }
      setVisibility( ! style.getShow() );
      setMaxExpandDepth(style.getMaxDepth());
      setLabel(style.getHumanName());
    } else {
      throw new NullPointerException();
    }
  }

  public IAnnotStyle getAnnotStyle() {
    return style;
  }

/**
 *  Adds "middleground" glyphs, which are drawn in front of the background but
 *    behind all "real" child glyphs.
 *  These are generally not considered children of
 *    the glyph.  The TierGlyph will render these glyphs, but they can't be selected since they
 *    are not considered children in pickTraversal() method.
 *  The only way to remove these is via removeAllChildren() method,
 *    there is currently no external access to them.
 */
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
      max_child_sofar = new ArrayList<GlyphI>(child_count);
      GlyphI curMaxChild = getChild(0);
      Rectangle2D.Double curbox = curMaxChild.getCoordBox();
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
	GlyphI maxchild = max_child_sofar.get(i);
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
  public List getPriorOverlaps(int query_index) {
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
      GlyphI cur_max_glyph = max_child_sofar.get(cur_index);
      Rectangle2D.Double rect = cur_max_glyph.getCoordBox();
      double cur_max = rect.x + rect.width;
      if (cur_max < query_min) {
	cur_index++;
	break;
      }
    }
    if (cur_index == query_index) { return null; }

    ArrayList<GlyphI> result = new ArrayList<GlyphI>();
    for (int i=cur_index; i<query_index; i++) {
      GlyphI child = getChild(i);
      Rectangle2D.Double rect = child.getCoordBox();
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
	GlyphI child = getChild(i);
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

  boolean shouldDrawLabel() {
    if (! style.isGraphTier()) {
      // graph tiers take care of drawing their own handles and labels.
      if (Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_LABELS_PROPERTY))) {
        return true;
      }
    }
    return false;
  }

  // overriding pack to ensure that tier is always the full width of the scene
  public void pack(ViewI view) {
    if (isTimed) { timecheck.start(); }
    int cycles = 1;
    for (int i=0; i<cycles; i++) {
      initForSearching();
      super.pack(view);
      Rectangle2D.Double mbox = scene.getCoordBox();
      Rectangle2D.Double cbox = this.getCoordBox();

      if (shouldDrawLabel()) {
        // Add extra space to make room for the label.
//        FontMetrics fm = view.getGraphics().getFontMetrics();
//        int h_pix = fm.getAscent() + fm.getDescent();
//        com.affymetrix.genoviz.bioviews.Point2D p = new com.affymetrix.genoviz.bioviews.Point2D(0,0);
//        view.transformToCoords(new Point(0,h_pix), p);
//        this.setCoords(mbox.x, cbox.y - p.y, mbox.width, cbox.height + p.y);

        // Although the space SHOULD be computed based on font metrics, etc,
        // that doesn't really work any better than a fixed coord value
        this.setCoords(mbox.x, cbox.y - 6, mbox.width, cbox.height + 6);
      } else {
        this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
      }
      //    if (isTimed && label.startsWith("whatever (+)")) {
    }
    if (isTimed) {
      long tim = timecheck.read();
      System.out.println("######## time to pack " + label + ": " + tim/cycles);
    }
  }

  public void drawTraversal(ViewI view) {
    super.drawTraversal(view);
  }

  /**
   *  Overriden to allow background shading by a collection of non-child
   *    "middleground" glyphs.  These are rendered after the solid background but before
   *    all of the children (which could be considered the "foreground").
   */
  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    pixelbox.width = Math.max ( pixelbox.width, min_pixels_width );
    pixelbox.height = Math.max ( pixelbox.height, min_pixels_height );

    Graphics g = view.getGraphics();
    Rectangle vbox = view.getPixelBox();
	pixelbox = pixelbox.intersection(vbox);

    if (middle_glyphs.size() == 0) { // no middle glyphs, so use fill color to fill entire tier
      if (style.getBackground() != null) {
	g.setColor(style.getBackground());
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
      GlyphI mglyph = middle_glyphs.get(i);
      Rectangle2D.Double mbox = mglyph.getCoordBox();
      mbox.setRect(mbox.x, coordbox.y, mbox.width, coordbox.height);
      mglyph.setColor(style.getBackground());
      mglyph.drawTraversal(view);
    }
    if (outline_color != null) {
      g.setColor(outline_color);
      g.drawRect(pixelbox.x, pixelbox.y,
		 pixelbox.width-1, pixelbox.height);
    }
    // not messing with clipbox until need to
    //        g.setClip ( oldClipbox );

    if (! style.isGraphTier()) {
      // graph tiers take care of drawing their own handles and labels.
      if (shouldDrawLabel()) {
        drawLabel(view);
      }
      if (Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_HANDLES_PROPERTY))) {
        drawHandle(view);
      }
    }

    super.draw(view);
  }

  public void drawLabel(ViewI view) {
    drawLabelLeft(view);
  }

  static Font default_font = new Font("Monospaced", Font.PLAIN, 12);
  public static void setLabelFont(Font f) {
    default_font = f;
  }

  public void drawLabelLeft(ViewI view) {
    if (getLabel() == null) { return; }
    Rectangle hpix = calcHandlePix(view);
    if (hpix != null) {
      Graphics g = view.getGraphics();
      g.setFont(default_font);
      FontMetrics fm = g.getFontMetrics();

//      java.awt.geom.Rectangle2D rect2d = g.getFontMetrics().getStringBounds(getLabel(), g);
//      g.setColor(Color.CYAN);
//      g.fillRect((hpix.x + hpix.width + 1), (hpix.y  + 1),
//          (int) rect2d.getWidth(), (int) rect2d.getHeight());

      g.setColor(this.getColor());
      g.drawString(getLabel(), (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
      //g.drawString(getLabel(), (hpix.x + hpix.width + 1), (int) (hpix.y + rect2d.getHeight()));
    }
  }

  static final boolean LARGE_HANDLE = false;
  Rectangle handle_pixbox = new Rectangle(); // caching rect for handle pixel bounds
  Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection

  protected Rectangle calcHandlePix(ViewI view) {
    // could cache pixelbox of handle, but then will have problems if try to
    //    have multiple views on same scene / glyph hierarchy
    // therefore reconstructing handle pixel bounds here... (although reusing same object to
    //    cut down on object creation)
    //    System.out.println("comparing full view cbox.x: " + view.getFullView().getCoordBox().x +
    //		       ", view cbox.x: " + view.getCoordBox().x);

    // if full view differs from current view, and current view doesn't left align with full view,
    //   don't draw handle (only want handle at left side of full view)
    if (view.getFullView().getCoordBox().x != view.getCoordBox().x)  {
      return null;
    }
      view.transformToPixels(coordbox, pixelbox);
      Rectangle view_pixbox = view.getPixelBox();
      int xbeg = Math.max(view_pixbox.x, pixelbox.x);
      Graphics g = view.getGraphics();
      g.setFont(default_font);
      FontMetrics fm = g.getFontMetrics();
      int h = Math.min(fm.getMaxAscent(), pixelbox.height);
      if (LARGE_HANDLE) {
        handle_pixbox.setBounds(xbeg, pixelbox.y, GraphGlyph.handle_width, pixelbox.height);
      }
      else {
        handle_pixbox.setBounds(xbeg, pixelbox.y, GraphGlyph.handle_width, h);
      }
      return handle_pixbox;
  }

  public void drawHandle(ViewI view) {
    Rectangle hpix = calcHandlePix(view);
    if (hpix != null) {
      Graphics g = view.getGraphics();
      Color c = new Color(style.getColor().getRed(), style.getColor().getGreen(), style.getColor().getBlue(), 64);
      g.setColor(c);
      g.fillRect(hpix.x, hpix.y, hpix.width, hpix.height);
      g.drawRect(hpix.x, hpix.y, hpix.width, hpix.height);
    }
  }

  /**
   *  Remove all children of the glyph, including those added with
   *  addMiddleGlyph(GlyphI).
   */
  public void removeAllChildren() {
    super.removeAllChildren();
    // also remove all middleground glyphs
    // this is currently the only place where middleground glyphs are treated as if they were children
    //   maybe should rename this method clear() or something like that...
    // only reference to middle glyphs should be in this.middle_glyphs, so should be able to GC them by
    //     clearing middle_glyphs.  These glyphs never have setScene() called on them,
    //     so it is not necessary to call setScene(null) on them.
    middle_glyphs.clear();
  }

  public void setState(int newstate) {
    if (newstate == EXPANDED) {
      setPacker(expand_packer);
      setVisibility(true);
    }
    else if (newstate == COLLAPSED) {
      setPacker(collapse_packer);
      setVisibility(true);
    }
    else if (newstate == FIXED_COORD_HEIGHT)  {
      setPacker(null);
      setVisibility(true);
    }
    else if (newstate == HIDDEN) {
      setVisibility(false);
    }
    else {
      System.out.println("state not recognized: " + newstate);
      return;
    }
  }

  /** Equivalent to setVisibility(true). */
  public void restoreState() {
    setVisibility(true);
  }

  public int getState() {
    if (isVisible()) {
      if (packer == expand_packer) {
        return EXPANDED;
      } else if (packer == collapse_packer) {
        return COLLAPSED;
      } else if (packer == null) {
        return FIXED_COORD_HEIGHT;
      }
    } else {
      return HIDDEN;
    }
    return -1; // should never happen
  }

  public PackerI getExpandedPacker() {
    return expand_packer;
  }

  public PackerI getCollapsedPacker() {
    return collapse_packer;
  }

  /** Sets the expand packer.  Note that you are responsible for setting
   *  any properties of the packer, such as those based on the AnnotStyle.
   */
  public void setExpandedPacker(PackerI packer) {
    this.expand_packer = packer;
    setSpacer(getSpacer());
    setStyle(getAnnotStyle()); // make sure the correct packer is used, and that its properties are set
  }

  public void setCollapsedPacker(PackerI packer) {
    this.collapse_packer = packer;
    setSpacer(getSpacer());
    setStyle(getAnnotStyle()); // make sure the correct packer is used, and that its properties are set
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
    if (style.getBackground() != col) {
      style.setBackground(col);
    }

    // Now set the "middleground" color based on the fill color
    if (col == null) {
      other_fill_color = Color.DARK_GRAY;
    } else {
      int intensity = col.getRed() + col.getGreen() + col.getBlue();
      if (intensity == 0) { other_fill_color = Color.darkGray; }
      else if (intensity > (255+127)) { other_fill_color = col.darker(); }
      else { other_fill_color = col.brighter(); }
    }
  }


  // very, very deprecated
  public Color getColor() {
    return getForegroundColor();
  }

  // very, very deprecated
  public void setColor(Color c) {
    setForegroundColor(c);
  }


  /** Returns the color used to draw the tier background, or null
      if there is no background. */
  public Color getFillColor() {
    return style.getBackground();
  }

  public void setForegroundColor(Color color) {
    if (style.getColor() != color) {
      style.setColor(color);
    }
    //super.setForegroundColor(color);
    //super.setColor(color); // NO: super.setColor calls setBackgroundColor()
  }

  public Color getForegroundColor() {
    return style.getColor();
  }

  public void setBackgroundColor(Color color) {
    //super.setBackgroundColor(color);
    setFillColor(color);
  }

  public Color getBackgroundColor() {
    //return super.getBackgroundColor();
    return getFillColor();
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

  public int getDirection() {
    return direction;
  }

  /**
   *  Sets direction.  Must be one of DIRECTION_FORWARD, DIRECTION_REVERSE,
   *  DIRECTION_BOTH or DIRECTION_NONE.
   */
  public void setDirection(int d) {
    if ((d != DIRECTION_FORWARD) && (d != DIRECTION_NONE)
        && (d != DIRECTION_REVERSE) && (d != DIRECTION_BOTH) && (d != DIRECTION_AXIS)) {
      throw new IllegalArgumentException();
    }
    this.direction = d;
  }


  /** Changes the maximum depth of the expanded packer.
   *  This does not call pack() afterwards, and has no effect if the
   *  getExpandedPacker() is not of the correct type to allow for setting the max depth.
   *  @return true if the current expand packer allowed the max depth to be set.
   */
  public boolean setMaxExpandDepth(int max) {
    PackerI epacker = getExpandedPacker();
    if (epacker instanceof FasterExpandPacker) {
      FasterExpandPacker fpacker = (FasterExpandPacker) epacker;
      fpacker.setMaxSlots(max);
      return true;
    }
    return false;
  }


  /** Returns a string representing the state of this object.
      @see #setState */
  public String getStateString() {
    String str = (this.isVisible ? "VISIBLE" : "HIDDEN") + " | ";
    if (packer instanceof ExpandPacker) {
      str += "EXPANDED";
    } else if (packer instanceof CollapsePacker) {
      str += "COLLAPSED";
    } else if (packer == null) {
      str += "NULL PACKER";
    } else {
      str += "PACKER = " + packer.getClass().getName();
    }
    return str;
  }

  /** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
  protected void drawSelectedFill(ViewI view) {
    this.drawSelectedOutline(view);
  }

  /** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
  protected void drawSelectedReverse(ViewI view) {
    this.drawSelectedOutline(view);
  }

  /*
  public void moveAbsolute(double x, double y) {
    System.out.println("move absolute: " + label + ", " + x + ", " + y);
    super.moveAbsolute(x, y);
  }
  */

}
