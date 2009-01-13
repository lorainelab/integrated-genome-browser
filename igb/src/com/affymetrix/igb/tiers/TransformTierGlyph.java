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
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

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
public class TransformTierGlyph extends TierGlyph {
  boolean DEBUG_PICK_TRAVERSAL = false;

  /*
   *  if fixed_pixel_height == true,
   *    then adjust transform during pack, etc. to keep tier
   *    same height in pixels
   *  (assumes tier only appears in one map / scene)
   */
  boolean fixed_pixel_height = false;
  int fixedPixHeight = 1;

  LinearTransform tier_transform = new LinearTransform();

  LinearTransform modified_view_transform = new LinearTransform();
  Rectangle2D modified_view_coordbox = new Rectangle2D();

  LinearTransform incoming_view_transform;
  Rectangle2D incoming_view_coordbox;

  // for caching in pickTraversal() methods
  Rectangle2D internal_pickRect = new Rectangle2D();
  // for caching in pickTraversal(pixbox, picks, view) method
  Rectangle2D pix_rect = new Rectangle2D();

  public TransformTierGlyph() {
    super();
  }
  
  public TransformTierGlyph(IAnnotStyle style)  {
    super(style);
  }

  public void setTransform(LinearTransform trans) {
    tier_transform = trans;
  }

  public LinearTransform getTransform() {
    return tier_transform;
  }

  public void drawChildren(ViewI view) {

    // MODIFY VIEW
    incoming_view_transform = (LinearTransform)view.getTransform();
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
    trans2D.translate(0.0, incoming_view_transform.getOffsetY());
    trans2D.scale(1.0, incoming_view_transform.getScaleY());

    //    trans2D.translate(1.0, this.getCoordBox().y);
    //    System.out.println("tier transform: offset = " + tier_transform.getOffsetY() +
    //    		       ", scale = " + tier_transform.getScaleY());

    trans2D.translate(1.0, tier_transform.getOffsetY());
    trans2D.scale(1.0, tier_transform.getScaleY());

    modified_view_transform = new LinearTransform();
    modified_view_transform.setScaleX(incoming_view_transform.getScaleX());
    modified_view_transform.setOffsetX(incoming_view_transform.getOffsetX());
    modified_view_transform.setScaleY(trans2D.getScaleY());
    modified_view_transform.setOffsetY(trans2D.getTranslateY());
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
    LinearTransform view_transform = (LinearTransform)view.getTransform();
    double yscale = 0.0d;
    if ( 0.0d != coordbox.height ) {
      yscale = (double)fixedPixHeight / coordbox.height;
    }
    //    System.out.println("yscale: " + yscale);
    yscale = yscale / view_transform.getScaleY();
    //    System.out.println("yscale2: " + yscale);
    tier_transform.setScaleY(tier_transform.getScaleY() * yscale );
    //    tier_transform.setOffsetY(tier_transform.getOffsetY() * yscale);
    /*
    tier_transform.setOffsetY(tier_transform.getOffsetY()
			      - (tier_transform.getOffsetY() * yscale) );
    */

    coordbox.height = coordbox.height * yscale;
  }


  //
  // need to redo pickTraversal, etc. to take account of transform also...
  //
  public void pickTraversal(Rectangle2D pickRect, Vector pickVector,
                            ViewI view)  {

    // copied form first part of Glyph.pickTraversal()
    if (isVisible && intersects(pickRect, view))  {
      if (hit(pickRect, view))  {
        if (!pickVector.contains(this)) {
          pickVector.addElement(this);
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
          child = children.elementAt( i );
          child.pickTraversal(internal_pickRect, pickVector, view );
        }
      }
      if (DEBUG_PICK_TRAVERSAL)  { debugLocation(pickRect); }
    }

  }


  // NOT YET TESTED
  public void pickTraversal(Rectangle pickRect, Vector pickVector,
                            ViewI view) {
    if (isVisible && intersects(pickRect, view))  {
      if (hit(pickRect, view))  {
        if (!pickVector.contains(this)) {
          pickVector.addElement(this);
        }
      }
      if (children != null)  {
	// recast to pickTraversal() with coord box rather than pixel box
	pix_rect.reshape(pickRect.x, pickRect.y, pickRect.width, pickRect.height);
	tier_transform.inverseTransform(pix_rect, internal_pickRect);
        GlyphI child;
        int childnum = children.size();
        for (int i=0; i<childnum; i++) {
          child = children.elementAt(i);
          child.pickTraversal(internal_pickRect, pickVector, view);
        }
      }
    }
  }


  // don't move children! just change tier's transform offset
  public void moveRelative(double diffx, double diffy) {
    coordbox.x += diffx;
    coordbox.y += diffy;
    //    tier_transform.setOffsetY(coordbox.y);
    //    tier_transform.setOffsetY(diffy);
    tier_transform.setOffsetY(tier_transform.getOffsetY() + diffy);
    //    System.out.println("Hmm: called moveRelative: diffx = " + diffx + ", diffy = " + diffy);
  }


  public void debugLocation(Rectangle2D pickRect) {
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

  /**
   *  WARNING - NOT YET TESTED
   *  This may very well not work at all!
   */
  public void getChildTransform(LinearTransform trans) {
    //    LinearTransform vt = (LinearTransform)view.getTransform();
    // mostly copied from drawChildren() ...
    // keep same X scale and offset, but concatenate internal Y transform
    AffineTransform trans2D = new AffineTransform();
    trans2D.translate(0.0, trans.getOffsetY());
    trans2D.scale(1.0, trans.getScaleY());
    trans2D.translate(1.0, tier_transform.getOffsetY());
    trans2D.scale(1.0, tier_transform.getScaleY());

    trans.setScaleY(trans2D.getScaleY());
    trans.setOffsetY(trans2D.getTranslateY());
  }


}

