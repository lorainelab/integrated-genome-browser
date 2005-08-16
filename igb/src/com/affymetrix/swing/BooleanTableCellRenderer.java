/**
*   Copyright (c) 2005 Affymetrix, Inc.
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

import java.awt.Component;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.TableCellRenderer;

/**
 *  A TableCellRenderer for showing a boolean value with a JCheckBox.
 *  This improves on the default Swing class JTable$BooleanCellRenderer
 *  by taking into account whether the cell does or does not have focus.
 *  (The default Swing class in JDK 1.4.2 does not take focus into account,
 *  but they may fix this in some future release.)
 */
public class BooleanTableCellRenderer extends JCheckBox 
implements TableCellRenderer {
    
  public BooleanTableCellRenderer() {
    super();
    setBorder(null);
    setBorderPainted(true);
    setHorizontalAlignment(JLabel.CENTER);
  }
  
  public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int column) {
        
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      setBackground(table.getSelectionBackground());
    }
    else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
    }

    if (hasFocus) {
      setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
      if (table.isCellEditable(row, column)) {
          setForeground( UIManager.getColor("Table.focusCellForeground") );
          setBackground( UIManager.getColor("Table.focusCellBackground") );
      }
    } else {
      setBorder(null);
    }

    setSelected((value != null && ((Boolean)value).booleanValue()));
    return this;
  }
}
