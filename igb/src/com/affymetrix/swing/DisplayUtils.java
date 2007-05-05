/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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

package com.affymetrix.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 *  General Swing display utilities.
 */
public class DisplayUtils {
  
  /** De-iconify a Frame and bring it to the front of the display, 
   *  without changing the minimized/maximized state.
   */
  public static void bringFrameToFront(Frame frame) {
    if ((frame.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
      // de-iconify it while leaving the maximized/minimized state flags alone
      frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
    }
    if (! frame.isShowing()) { frame.setVisible(true); }
    frame.toFront();
  }
  
  /**
   *  Scroll a JTable such that the given cell is visible.
   *  (Only works if the JTable is inside a scrollable parent.)
   *  From "The Java Developers Almanac 1.4."
   *  See http://javaalmanac.com/egs/javax.swing.table/Vis.html
   */
  public static void scrollToVisible(JTable table, int rowIndex, int vColIndex) {
    if (!(table.getParent() instanceof JViewport)) {
      return;
    }
    JViewport viewport = (JViewport)table.getParent();
    
    // This rectangle is relative to the table where the
    // northwest corner of cell (0,0) is always (0,0).
    Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);
    
    // The location of the viewport relative to the table
    Point pt = viewport.getViewPosition();
    
    // Translate the cell location so that it is relative
    // to the view, assuming the northwest corner of the
    // view is (0,0)
    rect.setLocation(rect.x-pt.x, rect.y-pt.y);
    
    // Scroll the area into view
    viewport.scrollRectToVisible(rect);
  }


  /**
   * adjusts JTable column preferred widths based on table contents
   * Adapted from "Swing Hacks" by Joshua Marinacci and Chris Adamson, 
   *    copyright 2005 O'Reilly Media, ISBN 0-596-00907-0
   */
  public static void adjustColumnPreferredWidths(JTable table, boolean include_headers) {
    // strategy - get max width for cells in column and
    // make that the preferred width
    TableColumnModel columnModel = table.getColumnModel();
    for (int col=0; col<table.getColumnCount(); col++) {
      int maxwidth = 0;
      for (int row=0; row<table.getRowCount(); row++) {
	TableCellRenderer rend = table.getCellRenderer (row, col);
	Object value = table.getValueAt (row, col);
	Component comp = rend.getTableCellRendererComponent (table,
							     value,
							     false,
							     false,
							     row,
							     col);
	maxwidth = Math.max (comp.getPreferredSize().width,
			     maxwidth);
      } // for row
      if (include_headers) {
	// include the column header's preferred width too
	TableColumn column = columnModel.getColumn (col);
	TableCellRenderer headerRenderer = column.getHeaderRenderer();
	if (headerRenderer == null) {
	  headerRenderer = table.getTableHeader().getDefaultRenderer();
	}
	Object headerValue = column.getHeaderValue();
	Component headerComp =
	  headerRenderer.getTableCellRendererComponent (table,
							headerValue,
							false,
							false,
							0,
							col);
	maxwidth = Math.max (maxwidth, headerComp.getPreferredSize().width);
	column.setPreferredWidth (maxwidth);
      }
      else {
	// don't include the column header's preferred width
	TableColumn column = columnModel.getColumn (col);
	column.setPreferredWidth (maxwidth);
      }
    } // for col
  }

}
