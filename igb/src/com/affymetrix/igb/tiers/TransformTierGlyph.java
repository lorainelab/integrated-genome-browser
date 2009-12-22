/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.style.IAnnotStyle;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.*;


/**
 *  TransformTierGlyph.
 *  only use transform for operations on children.
 *    coordinates of the tier itself are maintained in coordinate system
 *    of the incoming view...
 *
 *  currently assuming no modifications to tier_transform, etc. are made between
 *     call to modifyView(view) and call to restoreView(view);
 *
 *  Note that if the tier has any "middleground" glyphs, 
 *     these are _not_ considered children, so transform does not apply to them
 *
 */
public final class TransformTierGlyph extends TierGlyph {
  private static final boolean DEBUG_PICK_TRAVERSAL = false;

  /*
   *  if fixed_pixel_height == true,
   *    then adjust transform during pack, etc. to keep tier
   *    same height in pixels
   *  (assumes tier only appears in one map / scene)
   */
  private boolean fixed_pixel_height = false;
  private int fixedPixHeight = 1;

  private LinearTransform tier_transform = new LinearTransform();

  private LinearTransform modified_view_transform = new LinearTransform();
  private Rectangle2D.Double modified_view_coordbox = new Rectangle2D.Double();

  private LinearTransform incoming_view_transform;
  private Rectangle2D.Double incoming_view_coordbox;

  // for caching in pickTraversal() methods
  private Rectangle2D.Double internal_pickRect = new Rectangle2D.Double();
  // for caching in pickTraversal(pixbox, picks, view) method
  private Rectangle2D.Double pix_rect = new Rectangle2D.Double();
  
  public TransformTierGlyph(IAnnotStyle style)  {
    super(style);
  }

  public LinearTransform getTransform() {
    return tier_transform;
  }

  public void drawChildren(ViewI view) {

    // MODIFY VIEW
    incoming_view_transform = view.getTransform();
    incoming_view_coordbox = view.getCoordBox();

    // figure out draw transform by combining tier transform with view transform
    // should allow for arbitrarily deep nesting of transforms too, since cumulative
    //     transform is set to be view transform, and view transform is restored after draw...

    // should just copy values instead of creating new object every time,
    //    but for now just creating new object for convenience
    //    modified_view_transform = new LinearTransform(incoming_view_transform);

    //    modified_view_transform = new LinearTransform(incoming_view_transform);
    //    modified_view_transform.append(tier_transform);
    //    modified_view_transform.prepend(tier_transform);
    //    view.setTransform(modified_view_transform);
    //    view.setTransform(tier_transform);

    // should switch soon to doing this completely through
    //    LinearTransform calls, and eliminate new AffineTransform creation...
    AffineTransform trans2D = new AffineTransform();
    trans2D.translate(0.0, incoming_view_transform.getTranslateY());
    trans2D.scale(1.0, incoming_view_transform.getScaleY());

    //    trans2D.translate(1.0, this.getCoordBox().y);
    //    System.out.println("tier transform: offset = " + tier_transform.getOffsetY() +
    //    		       ", scale = " + tier_transform.getScaleY());

    trans2D.translate(1.0, tier_transform.getTranslateY());
    trans2D.scale(1.0, tier_transform.getScaleY());

    modified_view_transform = new LinearTransform();
    modified_view_transform.setScaleX(incoming_view_transform.getScaleX());
    modified_view_transform.setTranslateX(incoming_view_transform.getTranslateX());
    modified_view_transform.setScaleY(trans2D.getScaleY());
    modified_view_transform.setTranslateY(trans2D.getTranslateY());
    view.setTransform(modified_view_transform);

    // need to set view coordbox based on nested transformation
    //   (for methods like withinView(), etc.)
    view.transformToCoords(view.getPixelBox(), modified_view_coordbox);
    view.setCoordBox(modified_view_coordbox);

    // CALL NORMAL DRAWCHILDREN(), BUT WITH MODIFIED VIEW
    super.drawChildren(view);

    // RESTORE ORIGINAL VIEW
    view.setTransform(incoming_view_transform);
    view.setCoordBox(incoming_view_coordbox);

  }

  public void fitToPixelHeight(ViewI view) {
    // use view transform to determine how much "more" scaling must be
    //       done within tier to keep its
    LinearTransform view_transform = view.getTransform();
    double yscale = 0.0d;
    if ( 0.0d != coordbox.height ) {
      yscale = (double)fixedPixHeight / coordbox.height;
    }
    yscale = yscale / view_transform.getScaleY();
    tier_transform.setScaleY(tier_transform.getScaleY() * yscale );
 

    coordbox.height = coordbox.height * yscale;
  }


  //
  // need to redo pickTraversal, etc. to take account of transform also...
  //
	@Override
  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
                            ViewI view)  {

    // copied form first part of Glyph.pickTraversal()
    if (isVisible && intersects(pickRect, view))  {
      if (hit(pickRect, view))  {
        if (!pickList.contains(this)) {
          pickList.add(this);
        }
      }

      if (children != null)  {
	// modify pickRect on the way in
	//   (transform from view coords to local (tier) coords)
	//    [ an inverse transform? ]
	tier_transform.inverseTransform(pickRect, internal_pickRect);

	// copied from second part of Glyph.pickTraversal()
        GlyphI child;
        int childnum = children.size();
        for ( int i = 0; i < childnum; i++ ) {
          child = children.get( i );
          child.pickTraversal(internal_pickRect, pickList, view );
        }
      }
      if (DEBUG_PICK_TRAVERSAL)  { debugLocation(pickRect); }
    }

  }


  // NOT YET TESTED
	@Override
  public void pickTraversal(Rectangle pickRect, List<GlyphI> pickList,
                            ViewI view) {
    if (isVisible && intersects(pickRect, view))  {
      if (hit(pickRect, view))  {
        if (!pickList.contains(this)) {
          pickList.add(this);
        }
      }
      if (children != null)  {
	// recast to pickTraversal() with coord box rather than pixel box
	pix_rect.setRect(pickRect.x, pickRect.y, pickRect.width, pickRect.height);
	tier_transform.inverseTransform(pix_rect, internal_pickRect);
        GlyphI child;
        int childnum = children.size();
        for (int i=0; i<childnum; i++) {
          child = children.get(i);
          child.pickTraversal(internal_pickRect, pickList, view);
        }
      }
    }
  }


  // don't move children! just change tier's transform offset
	@Override
  public void moveRelative(double diffx, double diffy) {
    coordbox.x += diffx;
    coordbox.y += diffy;
    tier_transform.setTranslateY(tier_transform.getTranslateY() + diffy);
  }


  public void debugLocation(Rectangle2D.Double pickRect) {
    // just for debugging
    tier_transform.inverseTransform(pickRect, internal_pickRect);
    GlyphI pick_glyph = new FillRectGlyph();
    pick_glyph.setCoords(internal_pickRect.x, internal_pickRect.y,
			 internal_pickRect.width, internal_pickRect.height);
    System.out.println("pick at: ");
    System.out.println("view coords: " + pickRect);
    System.out.println("tier coords: " + internal_pickRect);

    pick_glyph.setColor(Color.black);
    this.addChild(pick_glyph);
  }

  public boolean hasFixedPixelHeight() {
    return fixed_pixel_height;
  }

  public void setFixedPixelHeight(boolean b) {
    fixed_pixel_height = b;
  }

  public void setFixedPixHeight(int pix_height) {
    fixedPixHeight = pix_height;
  }

  public int getFixedPixHeight() {
    return fixedPixHeight;
  }

}

