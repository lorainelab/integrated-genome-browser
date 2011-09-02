package com.affymetrix.igb.shared;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public abstract class SearchResultsTableModel extends AbstractTableModel {
	public abstract Object get(int i);
	public abstract void clear();
	public abstract int[] getColumnWidth();
	public abstract int[] getColumnAlign();
}
