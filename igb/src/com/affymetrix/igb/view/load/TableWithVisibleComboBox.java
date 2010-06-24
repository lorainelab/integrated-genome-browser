package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * A table with two customizations:
 * 1.  An always-visible combo box. For a user, this differentiates the field from a text box, and thus indicates they have a choice.
 * 2.  Different combo box elements per row.  This allows different behavior per server type.
 */
public final class TableWithVisibleComboBox {
	private static TableRowSorter<FeaturesTableModel> sorter;
	private static final JComboBoxToolTipRenderer comboRenderer = new JComboBoxToolTipRenderer();
  
	/**
	 * Set the columns to use the ComboBox DAScb and renderer (which also depends on the row/server type)
	 * @param table
	 * @param column
	 * @param enabled
	 */
	static void setComboBoxEditors(JTableX table, int column, boolean enabled) {
		comboRenderer.setToolTipEntry(LoadStrategy.NO_LOAD.toString(), IGBConstants.BUNDLE.getString("noLoadCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.VISIBLE.toString(), IGBConstants.BUNDLE.getString("visibleCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.CHROMOSOME.toString(), IGBConstants.BUNDLE.getString("chromosomeCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.GENOME.toString(), IGBConstants.BUNDLE.getString("genomeCBToolTip"));
		FeaturesTableModel ftm = (FeaturesTableModel) table.getModel();
		sorter = new TableRowSorter<FeaturesTableModel>(ftm);
		table.setRowSorter(sorter);

		int featureSize = ftm.getRowCount();
		RowEditorModel rm = new RowEditorModel(featureSize);
		// tell the JTableX which RowEditorModel we are using
		table.setRowEditorModel(rm);

		JComboBox DAScb = new JComboBox(FeaturesTableModel.standardLoadChoices);
		DAScb.setRenderer(comboRenderer);
		DAScb.setEnabled(true);
		DefaultCellEditor DASeditor = new DefaultCellEditor(DAScb);

		for (int row = 0; row < featureSize; row++) {
			GenericFeature gFeature = ftm.getFeature(row);
			SymLoader symL = gFeature.symL;
			if (symL != null) {
				JComboBox featureCB = new JComboBox(symL.getLoadChoices().toArray());
				featureCB.setRenderer(comboRenderer);
				featureCB.setEnabled(true);
				DefaultCellEditor featureEditor = new DefaultCellEditor(featureCB);
				rm.addEditorForRow(row, featureEditor);
				continue;
			}

			ServerType serverType = gFeature.gVersion.gServer.serverType;
			if (serverType == ServerType.DAS || serverType == ServerType.DAS2) {
				rm.addEditorForRow(row, DASeditor);
			} else {
				System.out.println("ERROR: Unexpected class " + serverType);
			}
		}

		TableColumn c = table.getColumnModel().getColumn(column);
		c.setCellRenderer(new ColumnRenderer());
		((JComponent) c.getCellRenderer()).setEnabled(enabled);
	}

  private static final class ColumnRenderer extends JComponent implements TableCellRenderer {

    private final JComboBox comboBox;
    private final JTextField textField;	// If an entire genome is loaded in, change the combo box to a text field.

    public ColumnRenderer() {
      comboBox = new JComboBox();
	  comboBox.setRenderer(comboRenderer);
      comboBox.setBorder(null);

	  textField = new JTextField(LoadStrategy.GENOME.toString());
	  textField.setToolTipText(IGBConstants.BUNDLE.getString("genomeCBToolTip"));	// only for whole genome
      textField.setBorder(null);
    }

    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

      if (((String) value).equals(textField.getText())) {
        return textField;
      } else {
        comboBox.removeAllItems();
        comboBox.addItem(value);
        return comboBox;
      }
    }
  }
}

/**
 * This maps a row to a specific editor.
 */
class RowEditorModel {

  private final Map<Integer, TableCellEditor> row2Editor;

  RowEditorModel(int size) {
    row2Editor = new HashMap<Integer, TableCellEditor>(size);
  }

  void addEditorForRow(int row, TableCellEditor e) {
    row2Editor.put(Integer.valueOf(row), e);
  }

  TableCellEditor getEditor(int row) {
    return row2Editor.get(Integer.valueOf(row));
  }
}

/**
 * A JTable with a RowEditorModel.
 */
class JTableX extends JTable {

  private RowEditorModel rm;

  public JTableX(TableModel tm) {
    super(tm);
    rm = null;
  }

  void setRowEditorModel(RowEditorModel rm) {
    this.rm = rm;
  }

  @Override
  public TableCellEditor getCellEditor(int row, int col) {
    if (rm != null) {
      TableCellEditor tmpEditor = rm.getEditor(row);
	  if (tmpEditor != null) {
		  return tmpEditor;
	  }
    }
    return super.getCellEditor(row, col);
  }
}
