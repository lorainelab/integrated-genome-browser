package com.affymetrix.igb.prefs;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * 
 * @author Tony Di Sera
 * 
 * Custom renderer for sources JTable so that row is greyed out
 * when server is disabled.
 *
 */
public class SourceCellRenderer extends JLabel implements TableCellRenderer {
	
	public SourceCellRenderer() {
		setOpaque(true);
		
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
	        boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {

		if (table.getSelectedRow() == rowIndex) {
			this.setBackground(table.getSelectionBackground());
		} else {
			this.setBackground(table.getBackground());
		}

		Boolean enabled = Boolean.class.cast(table.getModel().getValueAt(
		        rowIndex, SourceTableModel.ENABLED)); 

		setText(value != null ? value.toString() : "");
		this.setForeground(enabled ? Color.BLACK : Color.GRAY);

		
		return this;
	}

    // The following methods override the defaults for performance reasons
    public void validate() {}
    public void revalidate() {}
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
}