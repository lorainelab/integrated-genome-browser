/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.bioviews;

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.glyph.TreeNodeGlyph;

public class TreePacker implements PackerI {
  // public class TreePacker {

  public static int OUTLINE = TreeNodeGlyph.OUTLINE;
  public static int TREE = TreeNodeGlyph.TREE;
  protected int style = OUTLINE;
  int xindent = 15;
  int yspacing = 5;

  public Rectangle pack(GlyphI parent_glyph, GlyphI child_glyph, ViewI view) {
    throw new IllegalArgumentException("TreePacker.pack(parent, child, view)" +
        " not implemented yet");
  }

  public Rectangle packNode(TreeNodeGlyph parent, TreeNodeGlyph child,
      ViewI view) {
    Rectangle2D cbox = child.getCoordBox();
    cbox = child.calcUnexpandedCoords(cbox);
    Rectangle2D pbox = parent.getCoordBox();

    cbox.x = pbox.x + xindent;
    cbox.y = pbox.y + pbox.height + yspacing;

    if (style == TREE) {
      // y position will depend not on the number of nodes above, but rather
      //   the number of visible _leaf_ nodes
     Rectangle2D parent_lbox = parent.calcUnexpandedCoords(new Rectangle2D());
     cbox.x += parent_lbox.width;
    }

    child.setCoordBox(cbox);

    // call TreeNodeGlyph.calcBoundingBoxes() to calculate both pixel and coord
    //   bounding boxes based on coord positioning of top left corner
    //   (cbox.x, cbox.y)
    child.calcBoundingBoxes(view);
    return child.getPixelBox();
  }

  public Rectangle pack(GlyphI parent, ViewI view) {
    if (! (parent instanceof TreeNodeGlyph)) { return null; }
    TreeNodeGlyph parent_node = (TreeNodeGlyph)parent;
    Rectangle2D cbox = parent_node.getCoordBox();
    cbox = parent_node.calcUnexpandedCoords(cbox);

    parent_node.setCoordBox(cbox);


    // parent glyph's left corner stays where it has been positioned:
    //    it's bounding box extends to encompass all its children?

    // the packing is based on the glyph's visibility

    // packing a hierarchy of TreeNodeGlyphs should be a depth-first recursion
    //   recursion terminates at leaf nodes

    // use parent_glyph's bounding box to keep track of
    //    yoffset and xoffset (indentation)

    Vector children = parent_node.getChildren();

    // if no children then it's a leaf node, terminate the recursion
   if (children == null || children.size() <= 0 || !parent_node.isExpanded()) {
      parent_node.calcPixels(view);
      return parent_node.getPixelBox();
    }

    // otherwise traverse & recurse over children
    GlyphI child;
    TreeNodeGlyph child_node;
    Rectangle child_pix;
    Rectangle2D child_coords;
    for (int i=0; i<children.size(); i++) {
      child = (GlyphI)children.elementAt(i);
      if (!child.isVisible()) { continue; }
      else if (child instanceof TreeNodeGlyph)  {
        child_node = (TreeNodeGlyph)child;

        // position child relative to parent...
        packNode(parent_node, child_node, view);

        // then recurse
        child_pix = pack(child, view);
        // need to convert from Rectangle to Rectangle2D here
        child_coords = view.transformToCoords(child_pix, new Rectangle2D());

        // then extend parent's boundaries
        // (parent's boundaries are keeping track of xoffset and yoffset)
        parent_node.setCoordBox(parent_node.getCoordBox().union(child_coords));
      }
      else {  // not a TreeNodeGlyph, pack in a row towards the right???
        // do nothing for now
        continue;
      }
    }
    parent_node.calcPixels(view);
    return parent_node.getPixelBox();
  }

  public Vector<TreeNodeGlyph> getVisibleLeafNodes(TreeNodeGlyph root, Vector<TreeNodeGlyph> result_vec) {
    if (!root.isVisible()) { return result_vec; }
    Vector children = root.getChildren();
    if (children == null || (!root.isExpanded()))   {
      result_vec.addElement(root);
      return result_vec;
    }
    TreeNodeGlyph child;
    for (int i=0; i<children.size(); i++) {
      if (children.elementAt(i) instanceof TreeNodeGlyph) {
        child = (TreeNodeGlyph) children.elementAt(i);
        getVisibleLeafNodes(child, result_vec);
      }
    }
    return result_vec;
  }

  public Rectangle leftRightTreePack(TreeNodeGlyph root, ViewI view) {
    // 1. depth-first calculation of all visible nodes width/height
    //    based on fonts, and assume they all have x,y at root's x,y
    //
    // 2. depth-first calc x for all nodes based on x/width of parent
    //    nodes (and xspacing)
    //
    // 3. depth-first calc y for leaf nodes based on y/height of
    //    previous leaf node
    //
    // 4. calc y/height for all non-leaf nodes based on their contained
    //    descendants' y/height
    //
    // 5. calc width for all non-leaf nodes based on their contained
    //    descendants' width
    return null;

  }

  public Rectangle leftRightTreePack(TreeNodeGlyph leaf_node,
      TreeNodeGlyph prev_leaf_node, ViewI view) {
    // pack leaf_node just beneath prev_leaf_node
    return null;
  }

  public void setNodeStyle(int style) {
    this.style = style;
  }

  public int getNodeStyle() {
    return style;
  }


}
