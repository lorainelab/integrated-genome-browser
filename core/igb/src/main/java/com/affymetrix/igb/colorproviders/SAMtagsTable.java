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

public final class SAMtagsTable extends JRPStyledTable {
    public Map<String,Object> samtoolsData;
    private IParameters iParameters;

    public SAMtagsTable() {
        super("SAMtagsTable");
        this.setRowHeight(this.getRowHeight()+10);
        samtoolsData = new HashMap<>();
        DefaultTableModel dm = new DefaultTableModel();
        this.setModel(dm);
        dm.setDataVector(new Object[20][3],new Object[]{"Tag Value","Color",""});

        //Color
        this.getColumnModel().getColumn(SAMtagsTableModel.COL_COLOR).setCellRenderer(new ColorComboBoxCellRenderer());
        this.getColumnModel().getColumn(SAMtagsTableModel.COL_COLOR).setCellEditor(new ColorComboBoxCellEditor());
        this.getColumnModel().getColumn(SAMtagsTableModel.COL_COLOR).setMaxWidth(60);

        // Delete
        this.getColumnModel().getColumn(SAMtagsTableModel.COL_DELETE).setCellRenderer(new DeleteButtonCellRenderer());
        this.getColumnModel().getColumn(SAMtagsTableModel.COL_DELETE).setCellEditor(new DeleteButtonCellEditor(dm));
        this.getColumnModel().getColumn(SAMtagsTableModel.COL_DELETE).setMaxWidth(20);
    }
    public Map<String, Object> saveAndApply(){
        for(int i = 0;i<this.getRowCount();i++){
            if(this.getValueAt(i,0) != null && this.getValueAt(i,1) != null){
//                String[] tag_array=this.getValueAt(i,0).toString().toUpperCase().split(";");
//                for (String tag:tag_array) {
                    samtoolsData.put(this.getValueAt(i,0).toString(),this.getValueAt(i,1));
//                }
            }
        }
        return samtoolsData;
    }

    public IParameters getiParameters() {
        return iParameters;
    }

    public void populateUserData(IParameters iParameters) {
        this.iParameters = iParameters;
        Map<String, Color> params = (Map<String, Color>) iParameters.getParameterValue("values");
        int i = 0;
        for(String tag : params.keySet()){
            this.setValueAt(tag,i,0);
            this.setValueAt(params.get(tag),i,1);
            i++;
        }
    }

}
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
    private DefaultTableModel model;
    private int row,column;
    public DeleteButtonCellEditor(DefaultTableModel model) {
        super(new JCheckBox());
        this.model = model;
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
        this.row = row;
        if (bln) {
            deleteBtn.setForeground(table.getSelectionForeground());
            deleteBtn.setBackground(table.getSelectionBackground());
        } else {
            deleteBtn.setForeground(table.getForeground());
            deleteBtn.setBackground(table.getBackground());
        }
        label = (o == null) ? "" : o.toString();
        deleteBtn.setText(label);
        return deleteBtn;
    }

    @Override
    public Object getCellEditorValue() {
        SwingUtilities.invokeLater(() ->{
            if(row < model.getRowCount()){
                model.setValueAt("",row,SAMtagsTableModel.COL_VALUE);
                model.setValueAt(null,row, SAMtagsTableModel.COL_COLOR);
            }
        });
        return new String(label);
    }
    public boolean stopCellEditing() {
        return super.stopCellEditing();
    }
    protected void fireEditingStopped() {
        super.fireEditingStopped();
    }
}


class SAMtagsTableModel extends DefaultTableModel{
    public static final int COL_VALUE = 0;
    public static final int COL_COLOR = 1;
    public static final int COL_DELETE = 2;
    public String[] columns = {"Tag Value","Color",""};
    boolean[] canEdit = new boolean[]{
            true, true, true
    };
    private Map<String,Object> samtoolsData;
    private static int default_rows =20;

    public SAMtagsTableModel(Map<String,Object> samtoolsData) {
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