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

import java.awt.Rectangle;
import java.util.*;
import java.util.List;
import com.affymetrix.genoviz.bioviews.*;

public final class CollapsePacker implements PaddedPackerI {
  public static final int ALIGN_TOP = 1000;
  public static final int ALIGN_BOTTOM = 1001;
  public static final int ALIGN_CENTER = 1002;
  public static final int ALIGN_STRETCH = 1003;
  
  int alignment = ALIGN_CENTER;
  protected double parent_spacer = 10;
  protected double spacing = 0;

  public Rectangle pack(GlyphI parent, ViewI view) {
    Rectangle2D pbox = parent.getCoordBox();
    Vector children = parent.getChildren();
    double maxHeight = 0;

    if (children != null) { 
      GlyphI child;
      double height;
      for (int i=0; i<children.size(); i++) {
	child = (GlyphI)children.elementAt(i);
	height = child.getCoordBox().height;
	maxHeight = (height > maxHeight) ? height : maxHeight;
      }
    }


    parent.setCoords(pbox.x, pbox.y, pbox.width, maxHeight + (2 * parent_spacer));
    moveAllChildren(parent, view);

    Rectangle2D newbox = new Rectangle2D();
    newbox.reshape(parent.getCoordBox());
    // trying to transform according to tier's internal transform  
    //   (since packing is done base on tier's children)
    if (parent instanceof TransformTierGlyph)  {
      TransformTierGlyph transtier = (TransformTierGlyph)parent;
      LinearTransform tier_transform = transtier.getTransform();
      tier_transform.transform(newbox, newbox);
    }
    parent.setCoords(newbox.x, newbox.y, newbox.width, newbox.height);
    //    System.out.println("packed tier, coords are: " + parent.getCoordBox());
    return null;
  }

  protected void moveAllChildren(GlyphI parent, ViewI view) {
    List children = parent.getChildren();
    if (children == null) { return; }

    for (int i=0; i<children.size(); i++) {
      GlyphI child = (GlyphI) children.get(i);
      pack(parent, child, view);
    }
  }

  public void setAlignment(int val) {
    alignment = val;
  }

  public void setParentSpacer(double spacer) {
    this.parent_spacer = spacer;
  }
    
  public double getParentSpacer() {
    return parent_spacer;
  }

  /** Spacing has no effect in a CollapsePacker. */
  public void setSpacing(double sp) {
    this.spacing = sp;
  }

  /** Spacing has no effect in a CollapsePacker. */
  public double getSpacing() {
    return spacing;
  }

  public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) {
    Rectangle2D pbox = parent.getCoordBox();
    Rectangle2D cbox = child.getCoordBox();
    
    if (alignment == ALIGN_CENTER) {
      double center = pbox.y + pbox.height / 2;
      child.moveAbsolute(cbox.x, center - cbox.height/2);
    } else if (alignment == ALIGN_TOP) {
      child.moveAbsolute(cbox.x, pbox.y + parent_spacer);
    } else if (alignment == ALIGN_BOTTOM) {
      child.moveAbsolute(cbox.x, pbox.y + parent_spacer + pbox.height - cbox.height );
    } else if (alignment == ALIGN_STRETCH) {
      child.getCoordBox().reshape(cbox.x, pbox.y + parent_spacer, cbox.width, pbox.height);
    }
    return null;
  }

}


