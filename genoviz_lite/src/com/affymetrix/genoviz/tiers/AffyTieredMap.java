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

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.bioviews.TransformI.Dimension;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.util.GeometryUtils;

import com.affymetrix.genoviz.util.NeoConstants.Orientation;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import java.util.*;
import javax.swing.*;

public class AffyTieredMap extends NeoMap {

  boolean TIME_PACKING = false;
  public String name; // for debugging only
  protected List<TierGlyph> tiers = new ArrayList<TierGlyph>();
  com.affymetrix.genoviz.util.Timer timecheck = new com.affymetrix.genoviz.util.Timer();

  // the total pixel height of visible fixed pixel tiers
  //    (recalculated with every packTiers() call)
  int fixed_pixel_height;

  // the total coord height of visible fixed coord tiers
  //    (any visible tier that is NOT a fixed pixel tier)
  //    (recalculated with every packTiers() call)
  double fixed_coord_height;

  static boolean show_plus = true;
  static boolean show_minus = true;
  static boolean show_mixed = true;
    
  public Action show_plus_action = new AbstractAction("Show (+) tiers") {
    @Override
    public void actionPerformed(ActionEvent e) {
      show_plus = ! show_plus;
      putValue(SELECTED_KEY, Boolean.valueOf(show_plus));
      repackTheTiers(false, true);
    }
  };
  public Action show_minus_action = new AbstractAction("Show (-) tiers") {
    @Override
    public void actionPerformed(ActionEvent e) {
      show_minus = ! show_minus;
      putValue(SELECTED_KEY, Boolean.valueOf(show_minus));
      repackTheTiers(false, true);
    }    
  };
  public Action show_mixed_action = new AbstractAction("Show (+/-) tiers") {
    @Override
    public void actionPerformed(ActionEvent e) {
      show_mixed = ! show_mixed;
      putValue(SELECTED_KEY, Boolean.valueOf(show_mixed));
      repackTheTiers(false, true);
    }    
  };

  public AffyTieredMap() {
    super();
  }

  public AffyTieredMap(AffyTieredMap rootmap) {
    super(rootmap);
    this.tiers = rootmap.tiers;
  }

  public AffyTieredMap(boolean hscroll, boolean vscroll) {
    this(hscroll, vscroll, Orientation.Horizontal);
  }

  public AffyTieredMap(boolean hscroll, boolean vscroll, Orientation orient) {
    super(hscroll, vscroll, orient, new LinearTransform());
    show_plus_action.putValue(Action.SELECTED_KEY, Boolean.valueOf(show_plus));
    show_minus_action.putValue(Action.SELECTED_KEY, Boolean.valueOf(show_minus));
    show_mixed_action.putValue(Action.SELECTED_KEY, Boolean.valueOf(show_mixed));
  }

  /** Add the given tier to the map, building top-down. */
  public void addTier(TierGlyph mtg) {
    addTier(mtg, false);
  }

  /**
   * Add the given tier to the map.
   * @param mtg the TierGlyph being added.
   * @param ontop determines whether tier goes above or below existing tiers
   */
  public void addTier(TierGlyph mtg, boolean ontop) {
    if (ontop) {
      addTier(mtg, 0); 
    }
    else {
      addTier(mtg, tiers.size()); 
    }
  }

  public void addTier(TierGlyph mtg, int tier_index) {
    if (mtg == null)  { return; }
    // return if tier_index is greater than size of current tier vec
    if (tier_index > tiers.size()) { return; }
    super.addItem(mtg);
    tiers.add(tier_index, mtg);
  }

  public TierGlyph getTier(String tier_name) {
    for (int i=0; i<tiers.size(); i++) {
      TierGlyph tgl = tiers.get(i);
      if (tier_name.equals(tgl.getLabel())) {
	return tgl;
      }
    }
    return null;
  }

  /** Returns the index of the requested TierGlyph in the map,
   *  or -1 if it isn't included. The test is based on "==", not equals().
   */
  public int getTierIndex(TierGlyph tg) {
    int tindex = -1;
    for (int i=0; i<tiers.size(); i++) {
      if (tiers.get(i) == tg) {
	tindex = i;
	break;
      }
    }
    return tindex;
  }

  /** Returns an unmodifiable list of all tiers. */
  public List<TierGlyph> getAllTiers () { 
    return Collections.unmodifiableList(tiers); 
  }

  /**
   * Does a full repack of all tiers and the stretches the map.
   */
  @Override
  public void repack() {
    // WARNING
    // currently _ONLY_ tiers and glyphs placed in tiers will be repacked --
    // anything added directly to map other than tiers will need to
    // be dealt with manually
    packTiers(true, true);
  }



  static int packCount = 0;

  /**
   * Pack tiers in order with respect to each other.
   * Any tier with no children won't be considered in packing; it will
   * be treated as zero-height.
   * 
   * @param fullRepack if true, packs the contents of the tiers as well
   * as the tiers with respect to each other.
   * @param stretchMap reshapes the map to fit all of the tiers.
   *
   *
   */
  public void packTiers(boolean fullRepack, boolean stretchMap) {
    if (TIME_PACKING) {
      timecheck.start();
    }
    fixed_pixel_height = 0;
    fixed_coord_height = 0;

    if (fullRepack) {
      for (int i=0; i<tiers.size(); i++) {
        TierGlyph mtg = tiers.get(i);
        mtg.pack(getView());
      }
    }

    // Now hide or show tiers based on which checkboxes are selected
    for (int i=0; i<tiers.size(); i++) {
      TierGlyph mtg = tiers.get(i);
      
      if (mtg.getChildCount() <= 0) {
        mtg.setState(TierGlyph.HIDDEN);
      }
      else if ((! show_plus) && mtg.getDirection() == TierGlyph.DIRECTION_FORWARD) {
        mtg.setState(TierGlyph.HIDDEN);
      }
      else if ((! show_minus) && mtg.getDirection() == TierGlyph.DIRECTION_REVERSE) {
        mtg.setState(TierGlyph.HIDDEN);
      }
      else if ((! show_mixed) && (mtg.getDirection() == TierGlyph.DIRECTION_BOTH)) {
        mtg.setState(TierGlyph.HIDDEN);
      }
      else {
        if (mtg.getAnnotStyle().getShow()) {
          mtg.restoreState();
        } else {
          mtg.setState(TierGlyph.HIDDEN);
        }
      }
    }

    Rectangle2D.Double mbox = getCoordBounds();
    // assuming all tiers start anchored at 0 when being packed...
    double offset = 0;
    double height = mbox.height;
    //    System.out.println("in packTiers(), bounding coordbox: " + mbox);
    TierGlyph mtg;
    for (int i=0; i<tiers.size(); i++) {
      mtg = tiers.get(i);
      // don't make room if tier is'nt visible, or if it's hidden
      if ( (! mtg.isVisible()) ||
	   ((mtg.getState() == TierGlyph.HIDDEN))) {
	//	System.out.println("hiding tier: " + mtg.getLabel());
        continue;
      }
      if (mtg instanceof TransformTierGlyph &&
	  ((TransformTierGlyph)mtg).hasFixedPixelHeight()) {
	TransformTierGlyph transtier = (TransformTierGlyph)mtg;
	transtier.fitToPixelHeight(this.getView());
	fixed_pixel_height += transtier.getFixedPixHeight();
      }
      else {
	//	System.out.println(mtg.getCoordBox().height);
	fixed_coord_height += mtg.getCoordBox().height;
      }
      height = mtg.getCoordBox().height;
      // need to call moveAbsolute to trigger recursive move of
      //   all children
      //      System.out.println("moving tier absolute, yoffset = " + offset);
      //      mtg.moveAbsolute(mbox.x, offset);
      mtg.moveAbsolute(mtg.getCoordBox().x, offset);
      //      mtg.setCoords(mbox.x, offset, mbox.width, height);
      offset = offset + height;
    }

    if (stretchMap) {
      //      System.out.println("stretching");
      if (tiers.size() <= 0) { return; }
      Rectangle2D.Double pbox = getCoordBounds();
      Rectangle2D.Double newbox = null;
      mtg = null;

      for (int i=0; i<tiers.size(); i++) {
        mtg = tiers.get(i);
	if ((!mtg.isVisible()) || (mtg.getState() == TierGlyph.HIDDEN)) {
	  //	  System.out.println("still trying to hide tier: " + mtg.getLabel());
	  continue;
	}
	else if ( newbox == null ) {
	  newbox = new Rectangle2D.Double();
	  newbox.setRect(pbox.x, mtg.getCoordBox().y,
			 pbox.width, mtg.getCoordBox().height);
	}
	else {
	  GeometryUtils.union(newbox, mtg.getCoordBox(), newbox);
	}
      }

      if ( newbox != null ) {
	//	setMapOffset((int) newbox.y, (int) (newbox.y + newbox.height));
	//        setMapOffset(Math.floor(newbox.y), (int) (newbox.y + newbox.height));
        final double start = newbox.y;
        final double end = newbox.y + newbox.height;
        if (Double.isInfinite(end) || Double.isNaN(end) || Double.isInfinite(start) || Double.isNaN(start)) {
          //TODO: fix
          System.out.println("One or both of these values is bad! " + start + ", " + end);
          setFloatBounds(TransformI.Dimension.Y, 0, 100);
        } else {
          setFloatBounds(TransformI.Dimension.Y, newbox.y, newbox.y + newbox.height);          
        }
	//	System.out.println("new bounds: " + this.getCoordBounds());
      }
    }
    if (TIME_PACKING) {
      long tim = timecheck.read();
      System.out.println("time to pack: " + tim/1000f);
    }
  }

  @Override
  public void clearWidget() {
    super.clearWidget();
    tiers.clear();
  }

  /**
   * Makes sure the tiers always stretch the full length of the map.
   */
  @Override
  public void setBounds(TransformI.Dimension axis, int start, int end) {
    super.setBounds(axis, start, end);
    Rectangle2D.Double mbox = getScene().getRootGlyph().getCoordBox();
    if ((axis != TransformI.Dimension.X) || (tiers == null)) {
      return;
    }
    for (int i=0; i<tiers.size(); i++) {
      TierGlyph tier = tiers.get(i);
      Rectangle2D.Double tbox = tier.getCoordBox();
      tier.setCoords(mbox.x, tbox.y, mbox.width, tbox.height);
    }
  }

  @Override
  public void removeItem(GlyphI gl) {
    super.removeItem(gl);
    if (gl.getChildren() != null) {
      List<GlyphI> children = gl.getChildren();
      int childCount = children.size();
      //TODO xxx
      /* remove from end of child List instead of beginning! -- that way, won't
       *   get issues with trying to access elements off end of List as
       *   List shrinks during removal...
       */
      for (int i=childCount-1; i>=0; i--) {
	GlyphI child = children.get(i);
	removeItem(child);
      }
    }
  }

  /**
   * Given a TierGlyph, remove it from ourself.
   */
  public void removeTier(TierGlyph toRemove) {
    this.removeItem(toRemove);
    tiers.remove(toRemove);
  }

  protected boolean hasFixedTier() {
    int tierCount = tiers.size();
    for (int i=0; i<tierCount; i++) {
      TierGlyph mtg = tiers.get(i);
      if (mtg instanceof TransformTierGlyph) {
	TransformTierGlyph transtier = (TransformTierGlyph)mtg;
	if (transtier.hasFixedPixelHeight()) {
	  return true;
	}
      }
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public void stretchToFit(boolean fitx, boolean fity) {
    this.stretchToFit(fitx, fity, true);
  }

  /**
   * Calls {@link #stretchToFit(boolean, boolean)} and then optionally
   * packs the tiers with respect to each other.  Does <em>NOT</em>
   * pack the contents of each tier.
   * @param fitx Whether to stretch horizontally
   * @param fity Whether to stretch vertically
   * @param packTiers  Whether to pack tiers with respect to one another
   */
  public void stretchToFit(boolean fitx, boolean fity, boolean packTiers) {
    super.stretchToFit(fitx, fity);
    if (packTiers) {
      packTiers(false, true);
    }
    if (!fity) {
      doZoomFix(Dimension.Y);
    }
    if (!fitx) {
      doZoomFix(Dimension.X);
    }
  }

  /**
   *  A hack.  Sometimes after a stretchToFit() where fity is false it can happen
   *  that a portion of the area that should be filled by map tiers is empty.
   *  (Perhaps because some tiers were just hidden and the remaining ones don't
   *  fill up all the space.)  As soon as the user touches the Y-zoomer, 
   *  the map snaps to fill the given space.  This hack makes that happen automatically
   *  without the user having to touch the zoomer.
   */
  void doZoomFix(TransformI.Dimension dim) {
    final int ordinal = dim.ordinal();
    if (zoomtrans[ordinal] == null) {
      return;
    }
    zoomer_scale[ordinal] = zoomtrans[ordinal].transform(dim, zoomer_value[ordinal]);
    if (scale_constraint[ordinal] == ScaleConstraint.INTEGRAL_PIXELS ||
        scale_constraint[ordinal] == ScaleConstraint.INTEGRAL_ALL) {
      if (zoomer_scale[ordinal] >= 1)  {
        zoomer_scale[ordinal] = (int)(zoomer_scale[ordinal] +.0001);
      }
    }
    zoom(dim, zoomer_scale[ordinal]);
    // updateWidget(); // usually something else will call updateWidget() later
  }

  /**  Called within NeoMap.stretchToFit(), subclassing here to customize calculation
   *     to take into account fixed pixel tiers.
   */
  @Override
  public LinearTransform calcFittedTransform() {
    /*
       // hmm -- really would be best to calculate min and max zoom based on
       // coord sizes of non-fixed-pixel tiers compared against pixel size of canvas MINUS
       // the pixel size of all the fixed tiers:
       //
       //             (canvas_height - sum(fixed_pixel_tiers.pixel_height))
       // min_zoom = --------------------------------------------------------
       //             (viewbox_height - sum(fixed_pixel_tiers.coord_height))
       //
       // and since viewbox_height =
       //            sum(fixed_coord_tiers.coord_height) + sum(fixed_pixel_tiers.coord_height)
       //
       // substituting in for viewbox height gives:
       //
       //             (canvas_height - sum(fixed_pixel_tiers.pixel_height))
       // min_zoom = --------------------------------------------------------
       //                     sum(fixed_coord_tiers.coord_height)
       //
       // and since all terms on right side DO NOT CHANGE based on scale of zoom,
       // min_zoom stays CONSTANT
       // (assuming no other state changes / glyph manipulation occur in tiers)
       // hopefully a constant min_zoom will make zooming less jumpy...
       //

       //  "actual" max_zoom = max_zoom desired for fixed coord tiers , but then with
       //                      adjustment for fixed pixel tiers somehow factored in???
       //    (not worrying about max zoom yet...)
    */
    LinearTransform new_trans = super.calcFittedTransform();
    if (fixed_coord_height == 0)  { return new_trans; }
    //    if (fixed_pixel_height == 0)  { return new_trans; }
    //    double prevmin = getMinZoom(this.Y);
    int mod_pixel_height = canvas.getSize().height - fixed_pixel_height;
    double mod_coord_height = fixed_coord_height;
    double minzoom = mod_pixel_height / mod_coord_height;
    //    setMinZoom(this.Y, minzoom);
    //    System.out.println("standard fit: " + new_trans);
    new_trans.setScaleY(minzoom);
    /*
    System.out.println("new transform: " + new_trans);
    System.out.println("canvas size: " + canvas.getSize().height);
    System.out.println("constant-pix tiers total pixel height = " + fixed_pixel_height);
    System.out.println("constant-coord tiers tot coord height = " + fixed_coord_height);
    //    System.out.println("prev minzoom = " + prevmin + ", new minzoom = " + getMinZoom(this.Y));
    System.out.println("prev minzoom = " + prevmin + ", new minzoom = " + minzoom);
    System.out.println("calculated scene / view size at min zoom: " +
		       canvas.size().height / minzoom );
    */
    return new_trans;
  }


  //TODO: use Dimension instead of integer
  @Override
  public void zoom(TransformI.Dimension dim, double zoom_scale) {
    final int ordinal = dim.ordinal();
    if (dim == TransformI.Dimension.X) { 
      super.zoom(dim, zoom_scale); 
      return; 
    }
    //    System.out.println("***** zoom_scale = " + zoom_scale + " *****");
    if (zoom_scale == Float.NEGATIVE_INFINITY || zoom_scale == Float.POSITIVE_INFINITY ||
	Double.isNaN(zoom_scale)) {
      return;
    }
    // should be able to replace many variables calculation here with
    //    access to view coordbox fields...
    Rectangle2D.Double prev_view_coords = view.calcCoordBox();
    double prev_pixels_per_coord = pixels_per_coord[ordinal]; // should be same as trans.getScale()
    //double prev_coords_per_pixel = 1/prev_pixels_per_coord;
    pixels_per_coord[ordinal] = zoom_scale;
    coords_per_pixel[ordinal] = 1/zoom_scale;
    if (pixels_per_coord[ordinal] == prev_pixels_per_coord) {
      return;
    }

    double coord_beg, coord_end, coord_size;
    double fixed_coord, fixed_pixel;

    // assuming modifying Y
    // assume zoom constraint is always to hold middle of previous view constant...
    if (zoom_behavior[ordinal] == ZoomConstraint.CONSTRAIN_MIDDLE) {
      fixed_coord = prev_view_coords.y + (prev_view_coords.height / 2.0f);
      //      fixed_coord = prev_coord_offset + (prev_visible_coords / 2.0f);
    }
    // because bounds of map may change with every zoom (due to fixed-pixel tiers), the desired
    //   _coord_ of a glyph that needs to stay fixed in pixel-space may change.
    //   therefore need a better way of dealing with this...
    else if (zoom_behavior[ordinal] == ZoomConstraint.CONSTRAIN_COORD) {
      fixed_coord = zoom_coord[ordinal];
    }
    else {  // if unknown zoom behavior, hold middle constant
      fixed_coord = prev_view_coords.y + (prev_view_coords.height / 2.0f);
    }
    /*
    else if (zoom_behavior[id] == CONSTRAIN_START)  {
      fixed_coord = prev_coord_offset;
    }
    else if (zoom_behavior[id] == CONSTRAIN_END) {
      fixed_coord = prev_coord_offset + prev_visible_coords;
    }
    */

    // transforming fixed coord to find fixed pixel
    fixed_pixel = trans.transform(TransformI.Dimension.Y, fixed_coord);
    //    System.out.println("fixed coord = " + fixed_coord + ", fixed pixel = " + fixed_pixel);

    // calculate transform offset needed to hold coords at same fixed pixel
    double pix_offset = fixed_pixel - pixels_per_coord[ordinal] * fixed_coord;
    // convert transform offset (which is in pixels) to coords
    double coord_offset = pix_offset * coords_per_pixel[ordinal];

    //    System.out.println("real pix offset = " + pix_offset +
    //		       ", real coord offset = " + coord_offset);

    double visible_coords = coords_per_pixel[ordinal] * pixel_size[ordinal];
    double first_coord_displayed = -coord_offset;
    double last_coord_displayed = first_coord_displayed + visible_coords;

    // modifying view's transform so that scale-dependent packing occurs
    // AS IF zoom had already occurred
    // (not sure what this will mean for offset-dependent packing [though
    //   offset is set here, it may change later to trim view to scene] --
    //   will have to deal with that later when there's a reason to, right
    //   now packing does not depend on offset)
    // [later, may be able to leverage off the fact that tier maps always
    //   pack down from 0, so that scene starts at 0 and ends at (y+height)
    //   of last packed tier]
    trans.setOffsetY(pix_offset);
    trans.setScaleY(pixels_per_coord[ordinal]);
    // pack tiers (which may modify scene bounds) based on view with transform
    //    modified to take into account zoom_scale and "proposed" offset
    packTiers(false, true);

    // BEGIN only section that relies on scene coords
    Rectangle2D.Double scenebox = scene.getCoordBox();
    coord_beg = scenebox.y;
    coord_size = scenebox.height;
    coord_end = coord_beg + coord_size;
    // adjusting so that view doesn't extend beyond scene (unless total size
    //    of view is bigger than scene...)
    if (first_coord_displayed < coord_beg) {
      first_coord_displayed = coord_beg;
    }
    if (last_coord_displayed > coord_end) {
      first_coord_displayed = coord_end - visible_coords;
    }
    // END only section that relies on scene coords

    // recalculating offset transform after view vs. scene adjustment...
    coord_offset = -first_coord_displayed;
    pixel_offset[ordinal] = coord_offset / coords_per_pixel[ordinal];

    // redoing setting of transform, in case there were any adjusments to trim to scene...
    // assuming modifying Y
    trans.setOffsetY(pixel_offset[ordinal]);
    //    trans.setOffsetY(pix_offset);
    trans.setScaleY(pixels_per_coord[ordinal]);

    if (zoom_scale != zoomer_scale[ordinal]) { 
      adjustZoomer(Dimension.values()[ordinal]); 
    }
    adjustScroller(dim);

    view.calcCoordBox();
    //    System.out.println("zooming to: " + zoom_scale + ", coord offset = " + coord_offset);
    //	System.out.println("SceneCoordBox: " + scene.getCoordBox());
    //	System.out.println("ViewCoordBox:  " + view.getCoordBox());
  }

  /**
   *  Repacks tiers.  Should be called after hiding or showing tiers or
   *  changing their heights.
   */
  public void repackTheTiers(boolean full_repack, boolean stretch_vertically) {
    packTiers(full_repack, true);
    stretchToFit(false, stretch_vertically, false);
    // apply a hack to make sure strechToFit worked
    if ((getZoom(Dimension.Y) < getMinZoom(Dimension.Y)) || (getZoom(Dimension.Y) > getMaxZoom(Dimension.Y))) {
      stretchToFit(false, true, false);
    }
    updateWidget();
    
    // pack them again!  This clears-up problems with the packing of the axis
    // tier and getting the labelmap lined-up with the main tier map.
    packTiers(false,true); 
  }

  /** Prints this component. */
  public void print() throws java.awt.print.PrinterException {
    ComponentPagePrinter cpp = new ComponentPagePrinter(this);
    cpp.print();
    cpp = null; // for garbage collection
  }
  
    // if fixed tiers, then pack first
    //    (responsibility for packing tier itself to a fixed pixel height
    //    is left to the tier (which may well delegate to it's packer...)
    // supressing call to updateWidget() in packTiers()
    //    (de facto standard with GenoViz is for anything that calls zoom() or scroll(),
    //    the caller is responsible for deciding whether and when to call updateWidget())
    //    packTiers(false, true, false);
    // CALL NORMAL ZOOM

    // GAH 4-10-2002
    // MESSY PROBLEM WITH GETTING PACKING OF FIXED-PIXEL TIERS AND ZOOMING PLAYING NICE TOGETHER
    //
    // this seems to be the core of the problem:
    // packTiers packs based on a particular transform (which includes zoom scale), and
    // this ends up changing the bounds of the scene when there are fixed-pixel tiers.
    // But if the zoom is done _after_ the packTiers, then:
    //    A) the fixed-pixel tiers often end up larger or smaller than they are supposed
    //       to be (since there internal transforms weren't calculated for the
    //       appropriate scaling factor / zoom level),
    // and if the zoom is done _before_ the packTiers then
    //    B) the zoom does not take into account the new packed height of the map/scene,
    //       so things like scrollbar adjustments get screwed up
    // neither of which is what we really want
    // probably need to reimplement more of zoom rather than call super.zoom(), so
    //     can interleave manipulations happening in packTiers() and zoom(),
    //     so that zoom is done with the right scene size, and pack is done with the
    //     appropriate scaling factor
}









