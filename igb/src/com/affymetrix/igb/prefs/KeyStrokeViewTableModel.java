/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import javax.swing.table.AbstractTableModel;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

/**
 *
 * @author dcnorris
 */
public class KeyStrokeViewTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private final static String[] columnNames = {"Action", "Key Stroke (Double Click to Edit)", "Toolbar ?"};
	private Object[][] rows;

	@Override
	public int getRowCount() {
		return (rows == null) ? 0 : rows.length;
	}

	@Override
	public int getColumnCount() {
		return (columnNames == null) ? 0 : columnNames.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (col == KeyStrokesView.ToolbarColumn) {
			return (rows == null) ? Boolean.FALSE : rows[row][col];
		}
		return (rows == null) ? "" : rows[row][col];
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (column == KeyStrokesView.KeyStrokeColumn ||
			column == KeyStrokesView.ToolbarColumn
			) {
			return true;
		}
		return false;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		return column == KeyStrokesView.ToolbarColumn ? Boolean.class : String.class;
	}

	public void setRows(Object[][] rowData) {
		rows = rowData;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == KeyStrokesView.ToolbarColumn && rows != null) {
			rows[rowIndex][columnIndex] = aValue;
			String pref_name = GenericAction.getCleanText((String)rows[rowIndex][0]);
			PreferenceUtils.getToolbarNode().putBoolean(pref_name, (Boolean)aValue);
		}
    }
}
