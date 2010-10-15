package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.ButtonTableCellRenderer;
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
	static void setComboBoxEditors(JTableX table, boolean enabled) {
		comboRenderer.setToolTipEntry(LoadStrategy.NO_LOAD.toString(), IGBConstants.BUNDLE.getString("noLoadCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.VISIBLE.toString(), IGBConstants.BUNDLE.getString("visibleCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.CHROMOSOME.toString(), IGBConstants.BUNDLE.getString("chromosomeCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.GENOME.toString(), IGBConstants.BUNDLE.getString("genomeCBToolTip"));
		FeaturesTableModel ftm = (FeaturesTableModel) table.getModel();
		sorter = new TableRowSorter<FeaturesTableModel>(ftm);
		table.setRowSorter(sorter);

		int featureSize = ftm.getRowCount();
		RowEditorModel choices = new RowEditorModel(featureSize);
		RowEditorModel delete = new RowEditorModel(featureSize);
		// tell the JTableX which RowEditorModel we are using
		table.setRowEditorModel(FeaturesTableModel.LOAD_STRATEGY_COLUMN, choices);
		table.setRowEditorModel(FeaturesTableModel.DELETE_FEATURE_COLUMN, delete);

		for (int row = 0; row < featureSize; row++) {
			GenericFeature gFeature = ftm.getFeature(row);
			JComboBox featureCB = new JComboBox(gFeature.getLoadChoices().toArray());
			featureCB.setRenderer(comboRenderer);
			featureCB.setEnabled(true);
			DefaultCellEditor featureEditor = new DefaultCellEditor(featureCB);
			choices.addEditorForRow(row, featureEditor);

			ButtonTableCellEditor buttonEditor = new ButtonTableCellEditor(gFeature);
			delete.addEditorForRow(row, buttonEditor);
			buttonEditor.addActionListener(ftm);
		}

		TableColumn c = table.getColumnModel().getColumn(FeaturesTableModel.LOAD_STRATEGY_COLUMN);
		c.setCellRenderer(new ColumnRenderer());
		((JComponent) c.getCellRenderer()).setEnabled(enabled);

		c = table.getColumnModel().getColumn(FeaturesTableModel.DELETE_FEATURE_COLUMN);
		c.setCellRenderer(new ButtonTableCellRenderer());
	}

  static final class ColumnRenderer extends JComponent implements TableCellRenderer {

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

  private final Map<Integer, RowEditorModel> rmMap;

  public JTableX(TableModel tm) {
    super(tm);
    rmMap = new HashMap<Integer, RowEditorModel>();
  }

  void setRowEditorModel(int column, RowEditorModel rm) {
    this.rmMap.put(column, rm);
  }

  @Override
  public TableCellEditor getCellEditor(int row, int col) {
    if (rmMap != null) {
      TableCellEditor tmpEditor = rmMap.get(col).getEditor(row);
	  if (tmpEditor != null) {
		  return tmpEditor;
	  }
    }
    return super.getCellEditor(row, col);
  }

   @Override
   public TableCellRenderer getCellRenderer(int row, int column) {
	   if(column == FeaturesTableModel.LOAD_STRATEGY_COLUMN){
		   return new TableWithVisibleComboBox.ColumnRenderer();
	   }else if(column == FeaturesTableModel.DELETE_FEATURE_COLUMN){
		   return new ButtonTableCellRenderer();
	   }

	   return super.getCellRenderer(row,column);
   }
}
