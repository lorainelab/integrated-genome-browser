package com.affymetrix.genoviz.swing;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author hiralv
 */
public class LabelTableCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {

	private final JLabel label;

	public LabelTableCellRenderer(Icon icon, boolean enabled){
		label = new JLabel(icon);
		label.setEnabled(enabled);
	}

	@Override
	public void setEnabled(boolean enabled){
		label.setEnabled(enabled);
	}
	
	public LabelTableCellRenderer(){
		label = new JLabel();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		if(label.getIcon() == null && value instanceof String) {
			label.setText((String)value);
		}
		
		return label;
	}

}
