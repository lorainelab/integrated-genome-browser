package com.affymetrix.igb.view;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Hashtable;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * A table with two customizations:
 * 1.  An always-visible combo box. For a user, this differentiates the field from a text box, and thus indicates they have a choice.
 * 2.  Different combo box elements per row.  This allows different behavior per server type.
 */
final class TableWithVisibleComboBox {

	/**
	 * Set the columm to use the ComboBox editor and renderer
	 * @param items
	 */
	static void setComboBoxEditor(JTable table, int column, String[] items, boolean enabled) {
		JComboBox editor = new JComboBox(items);
		DefaultCellEditor dce = new DefaultCellEditor(editor);
		TableColumn c = table.getColumnModel().getColumn(column);
		c.setCellEditor(dce);
		c.setCellRenderer(new ComboBoxRenderer());
		((JComboBox) c.getCellRenderer()).setEnabled(enabled);
	}

	private static final class ComboBoxRenderer extends JComboBox implements TableCellRenderer {

		public Component getTableCellRendererComponent(
						JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
			setBorder(null);
			removeAllItems();
			addItem(value);

			return this;
		}
	}

	/**
	 * Allow each row to have a different editor.
	 */
	private class EachRowEditor implements TableCellEditor {
		protected Hashtable<Integer,TableCellEditor> editors;
		protected TableCellEditor editor,  defaultEditor;
		JTable table;

		/**
		 * Constructs EachRowEditor. Create default editor
		 *
		 * @see TableCellEditor
		 * @see DefaultCellEditor
		 */
		public EachRowEditor(JTable table) {
			this.table = table;
			editors = new Hashtable<Integer,TableCellEditor>();
			defaultEditor = new DefaultCellEditor(new JTextField());
		}

		public void setEditorAt(int row, TableCellEditor editor) {
			editors.put(new Integer(row), editor);
		}

		public Component getTableCellEditorComponent(JTable table, Object value,
						boolean isSelected, int row, int column) {
			return editor.getTableCellEditorComponent(table, value, isSelected,
							row, column);
		}

		public Object getCellEditorValue() {
			return editor.getCellEditorValue();
		}

		public boolean stopCellEditing() {
			return editor.stopCellEditing();
		}

		public void cancelCellEditing() {
			editor.cancelCellEditing();
		}

		public boolean isCellEditable(EventObject anEvent) {
			selectEditor((MouseEvent) anEvent);
			return editor.isCellEditable(anEvent);
		}

		public void addCellEditorListener(CellEditorListener l) {
			editor.addCellEditorListener(l);
		}

		public void removeCellEditorListener(CellEditorListener l) {
			editor.removeCellEditorListener(l);
		}

		public boolean shouldSelectCell(EventObject anEvent) {
			selectEditor((MouseEvent) anEvent);
			return editor.shouldSelectCell(anEvent);
		}

		protected void selectEditor(MouseEvent e) {
			int row;
			if (e == null) {
				row = table.getSelectionModel().getAnchorSelectionIndex();
			} else {
				row = table.rowAtPoint(e.getPoint());
			}
			editor = editors.get(new Integer(row));
			if (editor == null) {
				editor = defaultEditor;
			}
		}
	}
}