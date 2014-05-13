package com.affymetrix.igb.property;

import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nick
 */
public class PropertyTableModel extends DefaultTableModel {

    public PropertyTableModel(Object[][] os, Object[] os1) {
        this.setDataVector(os, os1);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col != 0;
    }
}
