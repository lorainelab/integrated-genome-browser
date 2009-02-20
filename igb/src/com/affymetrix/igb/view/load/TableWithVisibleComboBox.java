package com.affymetrix.igb.view.load;

import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.general.GenericFeature;
import com.affymetrix.igb.general.GenericServer;
import java.awt.Component;
import java.util.Hashtable;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * A table with two customizations:
 * 1.  An always-visible combo box. For a user, this differentiates the field from a text box, and thus indicates they have a choice.
 * 2.  Different combo box elements per row.  This allows different behavior per server type.
 */
public final class TableWithVisibleComboBox {

	/**
	 * Set the columm to use the ComboBox DAScb and renderer (which also depends on the row/server type)
	 * @param items
	 */
	public static void setComboBoxEditors(JTableX table, int column, boolean enabled) {
		RowEditorModel rm = new RowEditorModel();
		// tell the JTableX which RowEditorModel we are using
		table.setRowEditorModel(rm);

		FeaturesTableModel ftm = (FeaturesTableModel) table.getModel();

		JComboBox DAScb = new JComboBox(FeaturesTableModel.standardLoadChoices);
		DAScb.setEnabled(enabled);
		DefaultCellEditor DASeditor = new DefaultCellEditor(DAScb);
		
		JComboBox QuickLoadcb = new JComboBox(FeaturesTableModel.quickloadLoadChoices);
		QuickLoadcb.setEnabled(enabled);
		DefaultCellEditor QuickLoadeditor = new DefaultCellEditor(QuickLoadcb);

		for (int row = 0; row < ftm.features.size(); row++) {
			GenericFeature gFeature = ftm.features.get(row);
			GenericServer.ServerType serverType = gFeature.gVersion.gServer.serverType;
			if (serverType == GenericServer.ServerType.DAS || serverType == GenericServer.ServerType.DAS2) {
				rm.addEditorForRow(row, DASeditor);
			} else if (serverType == GenericServer.ServerType.QuickLoad) {
				rm.addEditorForRow(row, QuickLoadeditor);
			} else {
				System.out.println("ERROR: Undefined class " + serverType);
			}
		}

		TableColumn c = table.getColumnModel().getColumn(column);
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
}

/**
 * This maps a row to a specific editor.
 */
	class RowEditorModel {

		private Hashtable<Integer, TableCellEditor> row2Editor;

		public RowEditorModel() {
			row2Editor = new Hashtable<Integer, TableCellEditor>();
		}

		public void addEditorForRow(int row, TableCellEditor e) {
			row2Editor.put(new Integer(row), e);
		}

		public void removeEditorForRow(int row) {
			row2Editor.remove(new Integer(row));
		}

		public TableCellEditor getEditor(int row) {
			return row2Editor.get(new Integer(row));
		}
	}

	/**
	 * A JTable with a RowEditorModel.
	 */
	class JTableX extends JTable {

		public RowEditorModel rm;

		public JTableX(TableModel tm) {
			super(tm);
			rm = null;
		}

		public void setRowEditorModel(RowEditorModel rm) {
			this.rm = rm;
		}

		public RowEditorModel getRowEditorModel() {
			return rm;
		}

		@Override
		public TableCellEditor getCellEditor(int row, int col) {
			TableCellEditor tmpEditor = null;
			if (rm != null) {
				tmpEditor = rm.getEditor(row);
			}
			if (tmpEditor != null) {
				return tmpEditor;
			}
			return super.getCellEditor(row, col);
		}
	}
