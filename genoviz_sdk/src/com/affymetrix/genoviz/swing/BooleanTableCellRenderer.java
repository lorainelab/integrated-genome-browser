package com.affymetrix.genoviz.swing;

import java.awt.Component;
import javax.swing.*;
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

	public BooleanTableCellRenderer() {
		super();
		setBorder(null);
		setBorderPainted(true);
		setHorizontalAlignment(JLabel.CENTER);
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		boolean editable = table.isCellEditable(row, column);

		setEnabled(editable);

		if (isSelected || hasFocus) {
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(table.getBackground());
		}

		setSelected((value != null && ((Boolean) value).booleanValue()));
		return this;
	}
}
