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

package com.affymetrix.genoviz.awt;

import java.awt.*;
import com.affymetrix.genoviz.util.NeoConstants;

/**
 * Separates the components in a NeoMultiSplitter.
 * The user can use this bar to change the relative sizes of the components on each side of it.
 * @see NeoMultiSplitter
 */
public class NeoSplitterBar extends Canvas implements NeoConstants  {

  public int orientation = VERTICAL;  // VERTICAL and HORIZONTAL inherited from NeoConstants

  /**
   * For one-touch expansion/contraction.
   *  toggle for whether one-touch expand/collapse controls should be 
   *    added on the splitter
   */
  protected boolean include_controls = true;

  Rectangle lowexpand = new Rectangle();
  Rectangle highexpand = new Rectangle();

  public NeoSplitterBar(int orientation) {
    this.orientation = orientation;
  }

  public void setOrientation(int orient) {
    orientation = orient;
  }

  public boolean mouseEnter(Event evt, int x, int y) {
    if (orientation == VERTICAL) {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
    }
    else {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
    }
    return super.mouseEnter(evt, x, y);
  }
    
  public boolean mouseExit(Event evt, int x, int y)  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return super.mouseExit(evt, x, y);
  }

  public boolean mouseDrag(Event evt, int x, int y) {
    return super.mouseDrag(evt, x, y);
  }

  protected Frame findFrame() {
    Component parent, child;
    child = this;
    parent = child.getParent();
    while (!(parent instanceof Frame || parent == null)) {
      child = parent;
      parent = child.getParent();
    }
    if (parent == null)  { return null; }   // just a precaution...
    return (Frame)parent;
  }

  /**
   *  overriding paint to add slight 3D look.
   */
  public void paint(Graphics g) {
    super.paint(g);
    Dimension d = getSize();
    g.setColor(getBackground());
    g.draw3DRect(1, 1, d.width-2, d.height-2, true);
    g.setColor(getForeground());
    // hardwiring preliminary expand/collapse controls
    if (include_controls) {
      if (orientation == VERTICAL) {
        lowexpand.setBounds(3, 3, 3, 10);
        highexpand.setBounds(3, 16, 3, 10);
      }
      else {
        lowexpand.setBounds(3, 1, 10, 5);
        highexpand.setBounds(16, 1, 10, 5);
      }
      g.fillRect(lowexpand.x, lowexpand.y, 
                 lowexpand.width, lowexpand.height);
      g.fillRect(highexpand.x, highexpand.y, 
                 highexpand.width, highexpand.height);
    }
  }

  public void setIncludeControls(boolean include) {
    include_controls = include;
  }

  public boolean getIncludeControls() {
    return include_controls;
  }

  public boolean hitControls(int x, int y) {
    return (hitHighExpand(x,y) || hitLowExpand(x,y));
  }

  public boolean hitHighExpand(int x, int y) {
    return (include_controls && highexpand.contains(x,y));
  }

  public boolean hitLowExpand(int x, int y) {
    return (include_controls && lowexpand.contains(x,y));
  }

}
