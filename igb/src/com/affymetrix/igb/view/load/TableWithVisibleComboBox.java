package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.general.GenericFeature;
import java.awt.Component;
import java.util.Hashtable;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
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
      ServerType serverType = gFeature.gVersion.gServer.serverType;

      if (serverType == ServerType.DAS || serverType == ServerType.DAS2) {
        rm.addEditorForRow(row, DASeditor);
      } else if (serverType == ServerType.QuickLoad) {
        rm.addEditorForRow(row, QuickLoadeditor);
      } else {
        System.out.println("ERROR: Undefined class " + serverType);
      }
    }

    TableColumn c = table.getColumnModel().getColumn(column);
    c.setCellRenderer(new ColumnRenderer());
    ((JComponent) c.getCellRenderer()).setEnabled(enabled);
  }

  private static final class ColumnRenderer extends JComponent implements TableCellRenderer {

    private JComboBox comboBoxRender;
    private JTextField textFieldRender;	// If an entire genome is loaded in, change the combo box to a text field.

    public ColumnRenderer() {
      comboBoxRender = new JComboBox();
      textFieldRender = new JTextField(LoadStrategy.WHOLE.toString());
      comboBoxRender.setBorder(null);
      textFieldRender.setBorder(null);
    }

    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

      if (((String) value).equals(textFieldRender.getText())) {
        return textFieldRender;
      } else {
        comboBoxRender.removeAllItems();
        comboBoxRender.addItem(value);
        return comboBoxRender;
      }
    }
  }
}

/**
 * This maps a row to a specific editor.
 */
class RowEditorModel {

  private final Hashtable<Integer, TableCellEditor> row2Editor;

  public RowEditorModel() {
    row2Editor = new Hashtable<Integer, TableCellEditor>();
  }

  public void addEditorForRow(int row, TableCellEditor e) {
    row2Editor.put(Integer.valueOf(row), e);
  }

  public void removeEditorForRow(int row) {
    row2Editor.remove(Integer.valueOf(row));
  }

  public TableCellEditor getEditor(int row) {
    return row2Editor.get(Integer.valueOf(row));
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
