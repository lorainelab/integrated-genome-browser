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

package com.affymetrix.genoviz.glyph;

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import java.awt.image.ImageObserver;

/**
 *  A glyph that represents nodes in an outline tree.
 */

public class TreeNodeGlyph extends Glyph {  // or should it extend SolidGlyph???
  // need to generalize the notion of a TreeNodeGlyph, so _any_ glyph can 
  //    be added to a tree
  // or, TreeNodeGlyph needs to have a notion of containing two kinds of 
  //    children -- more TreeNodes, and a collection of other glyphs (labels, 
  //    icons, 
  public static int OUTLINE = 1000;
  public static int TREE = 1001;
  protected int style = OUTLINE;
  protected static Font DEFAULT_FONT = new Font("Courier", Font.BOLD, 12);
  //  protected static Color branch_color = Color.lightGray;
  protected static Color branch_color = Color.gray;
  protected static int icon_label_spacer = 5;

  protected String label;
  protected boolean expanded;
  protected boolean draw_outline = false;  // for debugging bounding rects
  protected boolean draw_branches = true;
  protected Font fnt;
  protected FontMetrics fm = 
     Toolkit.getDefaultToolkit().getFontMetrics(DEFAULT_FONT);
  protected Rectangle2D scratchbox = new Rectangle2D();
  protected Rectangle scratchpix = new Rectangle();
  protected Image icon = null;
  protected int icon_width, icon_height;

  public TreeNodeGlyph() {
    this("");
  }

  public TreeNodeGlyph(String label) {
    this.label = label;
    fnt = DEFAULT_FONT;
    setCoords(0, 0, 0, 0);
    setCoordBox(calcUnexpandedCoords(scratchbox));
    expanded = true;
  }

  public void setIcon(Image icon, ImageObserver observer) {
    this.icon = icon;
    ((Component)observer).prepareImage(icon, observer);
    icon_width = icon.getWidth(observer);
    icon_height = icon.getHeight(observer);
    setCoordBox(calcUnexpandedCoords(scratchbox));
  }

  public Image getIcon() {
    return icon;
  }
  
  public void addChild(GlyphI child) {
    super.addChild(child);
    child.setVisibility(expanded);
    if (child instanceof TreeNodeGlyph)  {
      ((TreeNodeGlyph)child).setNodeStyle(style);
    }
  }
  
  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
    Vector ch = this.getChildren();
    if (ch != null) {
      for (int i=0; i<ch.size(); i++) {
        ((GlyphI)ch.elementAt(i)).setVisibility(expanded);
      }
    }
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void setLabel(String label) {
    this.label = label;
    setCoordBox(calcUnexpandedCoords(scratchbox));
  }

  public void setFont(Font fnt) {
    this.fnt = fnt;
    fm = Toolkit.getDefaultToolkit().getFontMetrics(fnt);
    setCoordBox(calcUnexpandedCoords(scratchbox));
  }

  public Rectangle2D calcUnexpandedCoords(Rectangle2D rect_to_return) {
    // WARNING -- this assumes TreeNodeGlyph is being used on a non-scaling map, 
    // where 1 coord == 1 pixel
    // need to fix this if using TreeNodeGlyphs on scalable maps!
    //    FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(fnt);
    int node_width = fm.stringWidth(label);
    int node_height = fm.getAscent();
    if (icon != null) {
      if (icon_height > node_height) { node_height = icon_height; }
      node_width += icon_width;
      node_width += icon_label_spacer;
    }

    Rectangle2D cbox = getCoordBox();
    rect_to_return.x = cbox.x;
    rect_to_return.y = cbox.y;
    rect_to_return.width = node_width;
    rect_to_return.height = node_height;
    return rect_to_return;
  }

  public void calcBoundingBoxes(ViewI view) {
    // for now just leave coord bounding box alone
    calcPixels(view);
  }

  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    Graphics g = view.getGraphics();
    g.setFont(fnt);
    int text_width = fm.stringWidth(label);
    int text_height = fm.getAscent();
    int baseline = 0;

    calcUnexpandedCoords(scratchbox);
    view.transformToPixels(scratchbox, scratchpix);

    if (style == OUTLINE) {
      // -2 as a fudge factor to compensate for extra font ascent 
      //   above max caps on most platforms
      baseline = scratchpix.y + scratchpix.height/2 + text_height/2 - 2;
    }
    else if (style == TREE) {
      baseline = pixelbox.y + pixelbox.height/2 + text_height;
    }
    if (draw_outline) {
      g.setColor(Color.blue);
      g.drawRect(scratchpix.x, scratchpix.y,
          scratchpix.width, scratchpix.height);

      if (children != null && children.size() > 0) {
        g.setColor(Color.red);
        g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }

    g.setColor(getBackgroundColor());
    if (icon != null)  {
      ImageObserver observer = view.getComponent();
      g.drawImage(icon, pixelbox.x, pixelbox.y, observer);
      g.drawString(label,
          pixelbox.x + icon_width + icon_label_spacer,
          baseline);
    }
    else {
      g.drawString(label, pixelbox.x, baseline);
    }

    Vector ch = getChildren();
    if (draw_branches &&
        isExpanded() &&
        ch != null &&
        ch.size() > 0) {
      g.setColor(branch_color);
      TreeNodeGlyph child;
      Rectangle2D cbox;
      int ymin, ymax, ymid, xwidth;
      ymin = baseline+3;
      ymax = ymin;
      for (int i=ch.size()-1; i>=0; i--) {
        if (ch.elementAt(i) instanceof TreeNodeGlyph &&
            ((GlyphI)ch.elementAt(i)).isVisible()) {
          child = (TreeNodeGlyph)ch.elementAt(i);
          cbox = child.calcUnexpandedCoords(scratchbox);
          view.transformToPixels(cbox, scratchpix);
          ymax = scratchpix.y + scratchpix.height/2;
          break;
        }
      }
      // draw a vertical line from the node to the middle of the 
      // label of the last visible child
      g.fillRect(pixelbox.x+3, ymin, 1, ymax-ymin); 

      for (int i=0; i<ch.size(); i++) {
        // children should really be doing this!
        if (ch.elementAt(i) instanceof TreeNodeGlyph) {
          child = (TreeNodeGlyph)ch.elementAt(i);
          if (child.isVisible()) {
            cbox = child.calcUnexpandedCoords(scratchbox);
            view.transformToPixels(cbox, scratchpix);
            ymid = scratchpix.y + scratchpix.height/2;
            xwidth = scratchpix.x - pixelbox.x -6;
            g.fillRect(pixelbox.x+3, ymid, xwidth, 1);

            if (child.getChildren() != null &&
                child.getChildren().size() > 0 ) {
              g.fillRect(pixelbox.x-1, ymid-4, 9, 9);
              g.setColor(view.getComponent().getBackground());
              g.fillRect(pixelbox.x, ymid-3, 7, 7);
              g.setColor(branch_color);
              g.fillRect(pixelbox.x+1, ymid, 5, 1);
              if (!child.isExpanded()) {
                g.fillRect(pixelbox.x+3, ymid-2, 1, 5);
              }
            }
          }
        }
      }

    }
    super.draw(view);
  }

  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  isVisible() ? pixel_hitbox.intersects(this.getPixelBox()) : false;
  }

  public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
    if (!isVisible()) { return false; }
    Rectangle2D cbox = this.getCoordBox();
    if (!coord_hitbox.intersects(this.getCoordBox())) { return false; }
    scratchbox.x = cbox.x;
    scratchbox.y = cbox.y;
    Rectangle2D labelbox = calcUnexpandedCoords(scratchbox);
    return (coord_hitbox.intersects(labelbox));
  }

  /**
   * Overriding drawSelectedFill(view) to draw a filled background in 
   * selection color, and a normal draw of the TreeNodeGlyph on top.
   */
  public void drawSelectedFill(ViewI view) {
    Rectangle2D labelbox = calcUnexpandedCoords(scratchbox);
    Rectangle labelpix = view.transformToPixels(labelbox, scratchpix);
    Graphics g = view.getGraphics();
    g.setColor(view.getScene().getSelectionColor());
    g.fillRect(labelpix.x-2, labelpix.y-2,
        labelpix.width+4, labelpix.height+4);
    this.draw(view);
  }

  public boolean isLeafNode() {
    if (children == null) { return true; }
    for (int i=0; i<children.size(); i++) {
      if (children.elementAt(i) instanceof TreeNodeGlyph) {
        return false;
      }
    }
    return true;
  }

  public void setNodeStyle(int style) {
    this.style = style;
  }

  public int getNodeStyle() {
    return style;
  }

}
