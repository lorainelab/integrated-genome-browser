/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.ExistentialTriad;
import com.affymetrix.igb.IGB;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author dcnorris
 */
public class KeyStrokeViewTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private final static String[] columnNames = new String[KeyStrokesView.ColumnCount];
	static {
		columnNames[KeyStrokesView.IconColumn] = "";
		columnNames[KeyStrokesView.ToolbarColumn] = "Toolbar ?";
		columnNames[KeyStrokesView.ActionColumn] = "Action";
		columnNames[KeyStrokesView.KeyStrokeColumn] = "Key Stroke (Double Click to Edit)";
	}
	private Object[][] rows;

	@Override
	public int getRowCount() {
		return (rows == null) ? 0 : rows.length;
	}

	@Override
	public int getColumnCount() {
		return KeyStrokesView.ColumnCount;
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
		if (column == KeyStrokesView.KeyStrokeColumn
				|| column == KeyStrokesView.ToolbarColumn) {
			return true;
		}
		return false;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if (column == KeyStrokesView.IconColumn) {
			return ImageIcon.class;
		}
		if (column == KeyStrokesView.ToolbarColumn) {
			return ExistentialTriad.class;
		}
		return String.class;
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
			String pref_name = (String) rows[rowIndex][KeyStrokesView.IdColumn];
			boolean bValue;
			if (aValue instanceof ExistentialTriad) {
				ExistentialTriad t = (ExistentialTriad) aValue;
				bValue = t.booleanValue();
			}
			else { // Vestigial; This used to be a boolean.
				bValue = (Boolean) aValue;
			}
			PreferenceUtils.getToolbarNode().putBoolean(pref_name, bValue);
			GenericAction genericAction = GenericActionHolder.getInstance().getGenericAction(pref_name);
			if (genericAction == null) {
				
			}
			else {
				if (bValue) {
					((IGB)IGB.getSingleton()).addToolbarAction(genericAction);
				}
				else {
					((IGB)IGB.getSingleton()).removeToolbarAction(genericAction);
				}
			}
		}
	}
}
