package com.affymetrix.genoviz.swing;

import java.text.NumberFormat;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;


/**
 *  A TableCellRenderer for displaying formatted integers with commas.
 *  Use this only with non-editable table cells unless an appropriate
 *  TableCellEditor is also used.
 */
public final class IntegerTableCellRenderer extends DefaultTableCellRenderer {
  
  private NumberFormat nf;
  
  public IntegerTableCellRenderer() {
    super();
    nf = NumberFormat.getIntegerInstance();
    setHorizontalAlignment(SwingConstants.RIGHT);
  }

	@Override
  protected void setValue(Object value) {
    if (value instanceof Number) { // handles Integer, Double, etc.
      super.setValue(nf.format(value));
    } else {
      super.setValue(value);
    }
  }
    
}
