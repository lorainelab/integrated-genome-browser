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

public class CollapsePacker implements PaddedPackerI {
  public static int ALIGN_TOP = 1000;
  public static int ALIGN_BOTTOM = 1001;
  public static int ALIGN_CENTER = 1002;
  
  int alignment = ALIGN_CENTER;
  double maxHeight = 0;
  protected double parent_spacer = 2;
  protected double spacing = 2;

  public Rectangle pack(GlyphI parent, ViewI view) {
    Rectangle2D pbox = parent.getCoordBox();
    parent.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);
    Vector children = parent.getChildren();
    if (children == null) { maxHeight = 0;  }
    else  { 
      GlyphI child;
      double height;
    //        double maxHeight = 0;
      for (int i=0; i<children.size(); i++) {
	child = (GlyphI)children.elementAt(i);
	height = child.getCoordBox().height;
	maxHeight = (height > maxHeight) ? height : maxHeight;
      }
    }
    //    System.out.println(maxHeight);
    adjustHeight(parent);
    moveAllChildren(parent);

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

  protected void adjustHeight(GlyphI parent) {
    Rectangle2D pbox = parent.getCoordBox();
    //    parent.getCoordBox().height = maxHeight + (2 * parent_spacer);
    parent.setCoords(pbox.x, pbox.y, pbox.width, maxHeight + (2 * parent_spacer));
  }

  
  protected void moveAllChildren(GlyphI parent) {
    Rectangle2D pbox = parent.getCoordBox();
    Vector children = parent.getChildren();
    if (children == null) { return; }
    //	double parent_height = adjustHeight();
    //	double parent_height = maxHeight + (2 * parent_spacer);
    //	parent.setCoords(pbox.x, pbox.y, pbox.width, parent_height);
    double parent_height = parent.getCoordBox().height;

    Rectangle2D cbox;
    double center = pbox.y + parent_height / 2;
    for (int i=0; i<children.size(); i++) {
      GlyphI child = (GlyphI)children.elementAt(i);
      cbox = child.getCoordBox();
      child.moveAbsolute(cbox.x, center - cbox.height/2);
    }
  }

  public void setParentSpacer(double spacer) {
    this.parent_spacer = spacer;
  }
    
  public double getParentSpacer() {
    return parent_spacer;
  }

  public void setSpacing(double sp) {
    this.spacing = sp;
  }

  public double getSpacing() {
    return spacing;
  }

  public void setAlignment(int val) {
    alignment = val;
  }

  public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) { return null; }

}


