/**
*   Copyright (c) 2007 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/package com.affymetrix.swing;

import java.util.EventObject;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import javax.swing.*;

/*
 * A table cell editor that will reject all attempts at editing.
 */
public class BlockingTableCellEditor extends AbstractCellEditor implements TableCellEditor {
  
  JLabel comp = new JLabel("");
  Object original = null;
  
  public BlockingTableCellEditor() {
    super();
  }
  
  public Object getCellEditorValue() {
    return original;
  }
  
  public Component getTableCellEditorComponent(JTable table,
      Object value, boolean isSelected, int row, int column) {

    original = value;
    
    comp.setText(value == null ? "" : value.toString());    
    return comp;
  }

  public boolean isCellEditable(EventObject anEvent) {
    return true;
  }

  public boolean shouldSelectCell(EventObject anEvent) {
    return true;
  }
}

