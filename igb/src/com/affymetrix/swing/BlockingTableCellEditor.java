package com.affymetrix.swing;

import java.util.EventObject;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import javax.swing.*;

/*
 * A table cell editor that will reject all attempts at editing.
 */
public final class BlockingTableCellEditor extends AbstractCellEditor implements TableCellEditor {
  
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
    return null;
  }

  public boolean isCellEditable(EventObject anEvent) {
    return true;
  }

  public boolean shouldSelectCell(EventObject anEvent) {
    return true;
  }
}

