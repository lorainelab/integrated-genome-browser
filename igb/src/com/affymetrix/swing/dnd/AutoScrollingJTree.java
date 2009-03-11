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

package com.affymetrix.swing.dnd;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;

/** A JTree that can autoscroll itself when used as a DropTarget and
 *  placed inside a JScrollPane.
 *
 * Original version derived from Kim Topley "Core Swing: Advanced Programming",
 * Prentice-Hall, 1999.
 *
 * @author  ed
 */
public final class AutoScrollingJTree extends JTree implements Autoscroll {
  public static final Insets defaultScrollInsets = new Insets(12, 12, 12, 12);
  protected Insets scrollInsets = defaultScrollInsets;

  public AutoScrollingJTree() {
  }
  
  public void setScrollInsets(Insets insets) {
    this.scrollInsets = insets;
  }

  public Insets getScrollInsets() {
    return scrollInsets;
  }

  // Implementation of Autoscroll interface
  public Insets getAutoscrollInsets() {
    Rectangle r = getVisibleRect();
    Dimension size = getSize();
    Insets i = new Insets(r.y + scrollInsets.top, r.x + scrollInsets.left, 
        size.height - r.y - r.height + scrollInsets.bottom, 
        size.width - r.x - r.width + scrollInsets.right);
    return i;
  }

  public void autoscroll(Point location) {
    JScrollPane scroller = 
        (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
    if (scroller != null) {
      JScrollBar hBar = scroller.getHorizontalScrollBar();
      JScrollBar vBar = scroller.getVerticalScrollBar();
      Rectangle r = getVisibleRect();
      if (location.x <= r.x + scrollInsets.left) {
        // Need to scroll left
        hBar.setValue(hBar.getValue() - hBar.getUnitIncrement(-1));
      }
      if (location.y <= r.y + scrollInsets.top) {
        // Need to scroll up
        vBar.setValue(vBar.getValue() - vBar.getUnitIncrement(-1));
      }
      if (location.x >= r.x + r.width - scrollInsets.right) {
        // Need to scroll right
        hBar.setValue(hBar.getValue() + hBar.getUnitIncrement(1));
      }
      if (location.y >= r.y + r.height - scrollInsets.bottom) {
        // Need to scroll down
        vBar.setValue(vBar.getValue() + vBar.getUnitIncrement(1));
      }
    }
  }
}
