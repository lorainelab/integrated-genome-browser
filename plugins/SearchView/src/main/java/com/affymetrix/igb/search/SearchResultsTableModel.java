package com.affymetrix.igb.search;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public abstract class SearchResultsTableModel extends AbstractTableModel {

    public abstract Object get(int i);

    public abstract void clear();

    public abstract int[] getColumnWidth();

    public abstract int[] getColumnAlign();

    public DefaultTableCellRenderer getColumnRenderer(int column) {
        return new DefaultTableCellRenderer();
    }
}
