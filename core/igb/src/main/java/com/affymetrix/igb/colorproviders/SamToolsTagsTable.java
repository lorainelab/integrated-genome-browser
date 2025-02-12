package com.affymetrix.igb.colorproviders;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.general.IParameters;
import com.affymetrix.igb.swing.jide.JRPStyledTable;
import com.jidesoft.combobox.ColorComboBox;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public final class SamToolsTagsTable extends JRPStyledTable {
    public ColorComboBox colorComboBox;
    public Map<String,Object> samtoolsData;
    private IParameters iParameters;
    public SamToolsTagsTable(IParameters iParameters) {
        super("SamToolsTagsTable");

        this.setRowHeight(this.getRowHeight()+10);
        samtoolsData = new HashMap<>();
        colorComboBox = new ColorComboBox();
        this.iParameters = iParameters;
//        SamToolsTagsTableModel tableModel = new SamToolsTagsTableModel(samtoolsData);
        DefaultTableModel dm = new DefaultTableModel();
        this.setModel(dm);
        dm.setDataVector(new Object[20][3],new Object[]{"Tag Value","Color",""});


        //Color
        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_COLOR).setCellRenderer(new ColorComboBoxCellRenderer());
        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_COLOR).setCellEditor(new ColorComboBoxCellEditor());
        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_COLOR).setMaxWidth(60);

        // Delete

        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_DELETE).setCellRenderer(new DeleteButtonCellRenderer());
        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_DELETE).setCellEditor(new DeleteButtonCellEditor());
        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_DELETE).setMaxWidth(20);


        //Value
//        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_VALUE).setCellRenderer(new DefaultTableCellRenderer(){
//            @Override
//            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//            }
//        });
//        this.getColumnModel().getColumn(SamToolsTagsTableModel.COL_VALUE).setCellEditor(TagValueCellEditor.make());
    }
    public Map<String, Object> saveAndContinue(){
        for(int i = 0;i<this.getRowCount();i++){

            if(this.getValueAt(i,0) != null && this.getValueAt(i,1) != null){
                samtoolsData.put(this.getValueAt(i,0).toString().toUpperCase(),this.getValueAt(i,1));
            }
        }
        return samtoolsData;
//        JOptionPane.showMessageDialog(this,samtoolsData);
    }
}
//TCGGTAATCTGCGGCA;TTTACTGGTACTCTCC;GCCAAATGTCTGATTG;TACACGAAGACTAAGT;GTACTTTAGGTAGCTC;TAAGCGTAGGAGTACC
class ColorComboBoxCellRenderer extends DefaultTableCellRenderer {
    private final ColorComboBox ccb;
    public ColorComboBoxCellRenderer() {
        setOpaque(true);
        ccb = new ColorComboBox();
        ccb.setSelectedColor(Color.LIGHT_GRAY);

    }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            ccb.setBackground(table.getSelectionBackground());
        } else {
            ccb.setBackground(table.getBackground());
        }
        ccb.setSelectedItem(value);
        return ccb;
    }
}
class ColorComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final ColorComboBox ccb;
    public ColorComboBoxCellEditor() {
        ccb = new ColorComboBox();
        ccb.setSelectedColor(Color.LIGHT_GRAY);
        ccb.addActionListener((ActionListener) e -> {
            fireEditingStopped();
        });
    }

    @Override
    public Object getCellEditorValue() {
        return ccb.getSelectedItem();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        ccb.setSelectedColor((Color)value);
        ccb.setSelectedItem(value);
        return ccb;
    }
}
class DeleteButtonCellRenderer extends DefaultTableCellRenderer {
    public DeleteButtonCellRenderer() {
        setOpaque(true);
    }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = new JLabel();
        Icon delete_icon = CommonUtils.getInstance().getIcon("16x16/actions/delete.gif");
        this.setHorizontalAlignment(JLabel.CENTER);
        this.setVerticalAlignment(JLabel.CENTER);

        label.setIcon(delete_icon);
        return label;
    }
}
class DeleteButtonCellEditor extends DefaultCellEditor {
    DeleteButton deleteBtn;
    private String label;
    private boolean isPushed;
    public DeleteButtonCellEditor() {
        super(new JCheckBox());
        this.deleteBtn = new DeleteButton();
        deleteBtn.setOpaque(true);
        deleteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object o, boolean bln, int row, int column) {
        if (bln) {
            deleteBtn.setForeground(table.getSelectionForeground());
            deleteBtn.setBackground(table.getSelectionBackground());
        } else {
            deleteBtn.setForeground(table.getForeground());
            deleteBtn.setBackground(table.getBackground());
        }
        label = (o == null) ? "" : o.toString();
        deleteBtn.setText(label);
        isPushed = true;
        return deleteBtn;
    }
    public Object getCellEditorValue() {
        if (isPushed) {
            JOptionPane.showMessageDialog(deleteBtn, label + ": Ouch!");
        }
        isPushed = false;
        return new String(label);
    }
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }
    protected void fireEditingStopped() {
        super.fireEditingStopped();
    }
}

//class TagValueCellEditor extends DefaultCellEditor {
//    public static TagValueCellEditor make() {
//        JTextField field = new JTextField();
//        return new TagValueCellEditor(field);
//    }
//
//    public TagValueCellEditor(JTextField textField) {
//        super(textField);
//    }
//
//    @Override
//    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
//        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
//    }
//}

class SamToolsTagsTableModel extends DefaultTableModel{
    public static final int COL_VALUE = 0;
    public static final int COL_COLOR = 1;
    public static final int COL_DELETE = 2;
    public String[] columns = {"Tag Value","Color",""};
    boolean[] canEdit = new boolean[]{
            true, true, true
    };
    private Map<String,Object> samtoolsData;
    private static int default_rows =20;

    public SamToolsTagsTableModel(Map<String,Object> samtoolsData) {
        this.samtoolsData = samtoolsData;
    }

    @Override
    public String getColumnName(int column) {
        if(column == COL_VALUE) {
            return columns[COL_VALUE];
        }else if(column == COL_COLOR) {
            return columns[COL_COLOR];
        }else{
            return columns[COL_DELETE];
        }
    }

    @Override
    public int getRowCount() {
        return default_rows;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit[columnIndex];
    }
}

class DeleteButton extends JButton{
    public DeleteButton() {
        Icon delete_icon = CommonUtils.getInstance().getIcon("16x16/actions/delete.gif");
        setIcon(delete_icon);
        setBorder(BorderFactory.createEmptyBorder());
    }
}