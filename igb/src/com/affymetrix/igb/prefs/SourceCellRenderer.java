package com.affymetrix.igb.prefs;

import com.affymetrix.igb.prefs.SourceTableModel.SourceColumn;
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
	private static final long serialVersionUID = -5433598077871623855l;
	
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
		        rowIndex, SourceColumn.Enabled.ordinal()));

		setText(value != null ? value.toString() : "");
		this.setForeground(enabled ? Color.BLACK : Color.GRAY);

		
		return this;
	}

    // The following methods override the defaults for performance reasons
	@Override
    public void validate() {}
	@Override
    public void revalidate() {}
	@Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
	@Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
}