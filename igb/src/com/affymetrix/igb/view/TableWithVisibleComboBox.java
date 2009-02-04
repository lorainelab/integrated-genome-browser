package com.affymetrix.igb.view;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * A table with an always-visible combo box.
 * For a user, this differentiates the field from a text box, and thus indicates they have a choice.
 */
final class TableWithVisibleComboBox extends JFrame
{
	JTable table;
	DefaultTableModel model;

    TableWithVisibleComboBox(String[] columnNames, String[]comboItems, int comboColumn, Object[][]data) {
        model = new DefaultTableModel(data, columnNames);
		table = new JTable(model);
		JScrollPane scrollPane = new JScrollPane( table );
		getContentPane().add( scrollPane );
		setComboBoxEditor(comboColumn, comboItems);
    }

    /**
     * Set the columm to use the ComboBox editor and renderer
     * @param items
     */
    private void setComboBoxEditor(int column, String[] items) {
        JComboBox editor = new JComboBox(items);
        DefaultCellEditor dce = new DefaultCellEditor(editor);
        table.getColumnModel().getColumn(column).setCellEditor(dce);
        table.getColumnModel().getColumn(column).setCellRenderer(new ComboBoxRenderer());
    }
	private static final class ComboBoxRenderer extends JComboBox implements TableCellRenderer
	{
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column)
		{
			setBorder(null);
			removeAllItems();
			addItem( value );
			return this;
		}
	}
 }