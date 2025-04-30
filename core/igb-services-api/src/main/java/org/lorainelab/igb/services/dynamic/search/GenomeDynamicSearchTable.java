package org.lorainelab.igb.services.dynamic.search;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.LabelTableCellRenderer;
import com.affymetrix.igb.swing.jide.StyledJTable;

import javax.swing.Icon;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Color;

import static org.lorainelab.igb.services.dynamic.search.GenomeDynamicSearchTableModel.INFO_COLUMN;
import static org.lorainelab.igb.services.dynamic.search.GenomeDynamicSearchTableModel.LOAD_COLUMN;

public class GenomeDynamicSearchTable extends StyledJTable {
    private static final long serialVersionUID = 1L;
    private static final Icon INFO_ICON = CommonUtils.getInstance().getIcon("16x16/actions/info.png");
    private static final Icon LOAD_BUTTON = CommonUtils.getInstance().getIcon("images/load_button.png");

    public GenomeDynamicSearchTable(GenomeDynamicSearchTableModel model) {
        super(model);

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        super.list.add(LOAD_COLUMN);
        getColumnModel().getColumn(LOAD_COLUMN).setCellRenderer(new LabelTableCellRenderer(LOAD_BUTTON, true));
        getColumnModel().getColumn(LOAD_COLUMN).setCellEditor(new ButtonTableCellEditor(LOAD_BUTTON));

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
        if (!list.contains(c) && !isRowSelected(r)) {
            component.setBackground(Color.WHITE);
            component.setForeground(getForeground());
        } else if (!list.contains(c) && isRowSelected(r)) {
            component.setBackground(getSelectionBackground());
            component.setForeground(getSelectionForeground());
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