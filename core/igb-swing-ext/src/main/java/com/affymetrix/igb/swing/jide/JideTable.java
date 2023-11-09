package com.affymetrix.igb.swing.jide;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Vector;

public class JideTable extends JTable {
    public JideTable() {
        this.initTable();
        this.a();
    }

    public JideTable(int var1, int var2) {
        super(var1, var2);
        this.initTable();
        this.a();
    }

    public JideTable(TableModel var1) {
        super(var1);
        this.initTable();
        this.a();
    }

    public JideTable(Object[][] var1, Object[] var2) {
        super(var1, var2);
        this.initTable();
        this.a();
    }

    public JideTable(Vector<?> var1, Vector<?> var2) {
        super((Vector<? extends Vector>) var1, var2);
        this.initTable();
        this.a();
    }

    public JideTable(TableModel var1, TableColumnModel var2) {
        super(var1, var2);
        this.initTable();
        this.a();
    }

    public JideTable(TableModel var1, TableColumnModel var2, ListSelectionModel var3) {
        super(var1, var2, var3);
        this.initTable();
        this.a();
    }
    protected void initTable() {
    }
    private void a() {}
    public void setAutoResizeMode(Object fill){}
    public void setFillsGrids(boolean bool){}
    public void setCellSelectionEnabled(boolean bool){}
    public void setSelectionForeground(Color selectionBackgroundColor){}
    public void setIntercellSpacing(Dimension dimension){}
    public void setShowGrid(boolean b){}
    public void setGridColor(Color color){}
    public void setRowHeight(int i){}
    public JTableHeader getTableHeader(){
        return new JTableHeader();
    }

    public boolean isCellSelected(int r, int c){
        return false;
    }
    public boolean isCellEditable(int r, int c){
        return false;
    }
    public TableCellEditor getCellEditor() {
        return null;
    }
}
