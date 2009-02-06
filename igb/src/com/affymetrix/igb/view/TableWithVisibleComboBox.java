package com.affymetrix.igb.view;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * A table with an always-visible combo box.
 * For a user, this differentiates the field from a text box, and thus indicates they have a choice.
 */
final class TableWithVisibleComboBox {

    /**
     * Set the columm to use the ComboBox editor and renderer
     * @param items
     */
    static void setComboBoxEditor(JTable table, int column, String[] items) {
        JComboBox editor = new JComboBox(items);
        DefaultCellEditor dce = new DefaultCellEditor(editor);
        table.getColumnModel().getColumn(column).setCellEditor(dce);
        table.getColumnModel().getColumn(column).setCellRenderer(new ComboBoxRenderer());
    }

    private static final class ComboBoxRenderer extends JComboBox implements TableCellRenderer {

        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setBorder(null);
            removeAllItems();
            addItem(value);

            // TODO: Make row size of combo box larger.

            return this;
        }
    }
}