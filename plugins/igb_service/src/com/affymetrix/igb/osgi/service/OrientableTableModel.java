package com.affymetrix.igb.osgi.service;

import java.io.Serializable;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class OrientableTableModel extends AbstractTableModel implements TableModel, Serializable {
	private static final long serialVersionUID = 1L;

	private final AbstractTableModel baseTableModel;
	private boolean reverse;

	public OrientableTableModel(AbstractTableModel baseTableModel) {
		super();
		this.baseTableModel = baseTableModel;
		reverse = false;
	}

	@Override
	public int getRowCount() {
		if (reverse) {
			return baseTableModel.getColumnCount();
		}
		else {
			return baseTableModel.getRowCount();
		}
	}

	@Override
	public int getColumnCount() {
		if (reverse) {
			return baseTableModel.getRowCount() + 1;
		}
		else {
			return baseTableModel.getColumnCount();
		}
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (reverse) {
			return null;
		}
		else {
			return baseTableModel.getColumnName(columnIndex);
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (reverse) {
			return Object.class;
		}
		else {
			return baseTableModel.getColumnClass(columnIndex);
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (reverse) {
			if (columnIndex == 0) {
				return false;
			}
			return baseTableModel.isCellEditable(columnIndex - 1, rowIndex);
		}
		else {
			return baseTableModel.isCellEditable(rowIndex, columnIndex);
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (reverse) {
			if (columnIndex == 0) {
				return baseTableModel.getColumnName(rowIndex);
			}
			else {
				return baseTableModel.getValueAt(columnIndex - 1, rowIndex);
			}
		}
		else {
			return baseTableModel.getValueAt(rowIndex, columnIndex);
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (reverse) {
			if (columnIndex == 0) {
				return;
			}
			baseTableModel.setValueAt(aValue, columnIndex - 1, rowIndex);
		}
		else {
			baseTableModel.setValueAt(aValue, rowIndex, columnIndex);
		}
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		baseTableModel.addTableModelListener(l);
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		baseTableModel.removeTableModelListener(l);
	}

    public void fireTableDataChanged() {
    	baseTableModel.fireTableDataChanged();
    }

    public AbstractTableModel getBaseTableModel() {
		return baseTableModel;
	}

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		if (this.reverse != reverse) {
			this.reverse = reverse;
			baseTableModel.fireTableStructureChanged();
	    	baseTableModel.fireTableDataChanged();
		}
	}
}
