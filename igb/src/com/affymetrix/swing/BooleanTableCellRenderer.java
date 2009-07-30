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
public final class BooleanTableCellRenderer extends JCheckBox 
implements TableCellRenderer {
    
  boolean indicateWhenEditable = true;
  
  public BooleanTableCellRenderer() {
    super();
    setBorder(null);
    setBorderPainted(true);
    setHorizontalAlignment(JLabel.CENTER);
  }
  
  /** Whether or not to visually indicate the differenct between editable
   *  and non-editable checkboxes.  Does this by calling JCheckBox.setEnabled().
   *  If your table contains a mix of editable and non-editable checkboxes, you
   *  should probably set this to true.  If the entire table is not editable,
   *  you might want to choose to set this to false, but I recommend keeping
   *  it true.
   *  Default is true.
   */
  public void setIndicateWhenEditable(boolean b) {
    indicateWhenEditable = b;
  }
  
  public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int column) {
    
    boolean editable = table.isCellEditable(row, column);
    
    if (indicateWhenEditable) {
      setEnabled(editable); // called for the side-effect on the icons:
        // typically, makes the checkbox be grayed-out when not editable.
        // See AbstractButton.setSelectedIcon(), setSelectedDisabledIcon(), etc.
    } else {
      setEnabled(true);
    }
      
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
      if (editable) {
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
