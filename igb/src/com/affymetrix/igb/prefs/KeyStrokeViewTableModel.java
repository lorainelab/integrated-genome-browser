/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import javax.swing.table.AbstractTableModel;

/**
 *
 * @author dcnorris
 */
public class KeyStrokeViewTableModel extends AbstractTableModel {

	private final static String[] columnNames = {"Action", "Key Stroke (Double Click to Edit)"};
	private Object[][] rows;

	public int getRowCount() {
		return (rows == null) ? 0 : rows.length;
	}

	public int getColumnCount() {
		return (columnNames == null) ? 0 : columnNames.length;
	}

	public Object getValueAt(int i, int i1) {
		return (rows == null) ? "" : rows[i][i1];
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (column == KeyStrokesView.KeySrokeColumn) {
			return true;
		}
		return false;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		return String.class;
	}

	public void setRows(Object[][] rowData) {
		rows = rowData;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}
}
