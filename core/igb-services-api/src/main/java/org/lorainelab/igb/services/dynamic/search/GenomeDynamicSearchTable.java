package org.lorainelab.igb.services.dynamic.search;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.LabelTableCellRenderer;
import com.affymetrix.igb.swing.jide.StyledJTable;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.lorainelab.igb.services.dynamic.search.GenomeDynamicSearchTableModel.INFO_COLUMN;
import static org.lorainelab.igb.services.dynamic.search.GenomeDynamicSearchTableModel.LOAD_COLUMN;

public class GenomeDynamicSearchTable extends StyledJTable {
    private static final long serialVersionUID = 1L;
    private final GenomeDynamicSearchTableModel model;
    private final ExternalGenomeDataProvider dataProvider;
    private static final Icon INFO_ICON = CommonUtils.getInstance().getIcon("16x16/actions/info.png");

    public GenomeDynamicSearchTable(GenomeDynamicSearchTableModel model, ExternalGenomeDataProvider dataProvider) {
        super(model);
        this.model = model;
        this.dataProvider = dataProvider;

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(false);

        super.list.add(LOAD_COLUMN);
        getColumnModel().getColumn(LOAD_COLUMN).setCellRenderer(new ButtonRenderer());
        getColumnModel().getColumn(LOAD_COLUMN).setCellEditor(new ButtonEditor(new JCheckBox(), dataProvider, model));

        getColumnModel().getColumn(LOAD_COLUMN).setPreferredWidth(75);
        getColumnModel().getColumn(LOAD_COLUMN).setMaxWidth(75);
        getColumnModel().getColumn(LOAD_COLUMN).setMinWidth(75);

        super.list.add(INFO_COLUMN);
        getColumnModel().getColumn(INFO_COLUMN).setCellRenderer(new LabelTableCellRenderer(INFO_ICON, true));
        getColumnModel().getColumn(INFO_COLUMN).setCellEditor(new ButtonTableCellEditor(INFO_ICON));

        getColumnModel().getColumn(INFO_COLUMN).setPreferredWidth(25);
        getColumnModel().getColumn(INFO_COLUMN).setMaxWidth(25);
        getColumnModel().getColumn(INFO_COLUMN).setMinWidth(25);

    }

    @Override
    public Component prepareRenderer(TableCellRenderer tcr, int r, int c) {
        Component component = super.prepareRenderer(tcr, r, c);
        if (!list.contains(c)) {
            component.setBackground(Color.WHITE);
        }
        return component;
    }

    @Override
    public TableCellRenderer getCellRenderer(int r, int c) {
        TableCellRenderer renderer = super.getCellRenderer(r, c);
        if (renderer instanceof DefaultTableCellRenderer) {
            ((DefaultTableCellRenderer) renderer).setHorizontalAlignment(SwingConstants.LEFT);
        }
        return renderer;
    }
}

class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
        setBackground(new Color(46, 100, 227));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value != null ? value.toString() : "Load");
        if (isSelected) {
            setBackground(new Color(30, 80, 200));
        } else {
            setBackground(new Color(46, 100, 227));
        }
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return this;
    }
}

class ButtonEditor extends DefaultCellEditor {
    private final JButton button;
    private ExternalGenomeData genomeData;
    private final ExternalGenomeDataProvider dataProvider;
    private final GenomeDynamicSearchTableModel model;
    private boolean isClicked;

    public ButtonEditor(JCheckBox checkBox, ExternalGenomeDataProvider dataProvider, GenomeDynamicSearchTableModel model) {
        super(checkBox);
        this.dataProvider = dataProvider;
        this.model = model;
        button = new JButton("Load");
        button.setOpaque(true);
        button.setBackground(new Color(46, 100, 227));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isClicked = true;
                fireEditingStopped();
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        genomeData = model.getData().get(row);
        button.setText("Load");
        button.setBackground(new Color(30, 80, 200));
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        if (isClicked) {
            dataProvider.performLoadGenome(genomeData);
        }
        isClicked = false;
        return "Load";
    }

    @Override
    public boolean stopCellEditing() {
        isClicked = false;
        return super.stopCellEditing();
    }
}
