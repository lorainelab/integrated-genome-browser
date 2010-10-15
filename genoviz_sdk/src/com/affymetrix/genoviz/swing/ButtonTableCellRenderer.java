package com.affymetrix.genoviz.swing;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author hiralv
 */
public final class ButtonTableCellRenderer extends JComponent implements TableCellRenderer {

	private final JButton button;

	public ButtonTableCellRenderer(Icon icon){
		button = new JButton(icon);
		button.setBorderPainted(false);
	}

	public ButtonTableCellRenderer(){
		button = new JButton();
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		if(button.getIcon() == null && value instanceof String)
			button.setText((String)value);
		
		return button;
	}

}
