package org.lorainelab.igb.services.dynamic.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GenomeDynamicSearchTableModel extends AbstractTableModel {
    private static final Logger LOG = LoggerFactory.getLogger(GenomeDynamicSearchTableModel.class);
    public static int LOAD_COLUMN = 0;
    public static int INFO_COLUMN = 0;
    private List<ExternalGenomeData> data;
    private final ExternalGenomeDataProvider externalGenomeDataProvider;
    private final String[] columnNames;

    public GenomeDynamicSearchTableModel(ExternalGenomeDataProvider externalGenomeDataProvider) {
        this.externalGenomeDataProvider = externalGenomeDataProvider;
        List<String> dataProviderColumnNames = new ArrayList<>(externalGenomeDataProvider.getColumnNames());
        dataProviderColumnNames.add("");
        dataProviderColumnNames.add("");
        columnNames = dataProviderColumnNames.toArray(new String[0]);
        LOAD_COLUMN = columnNames.length - 2;
        INFO_COLUMN = columnNames.length - 1;
    }

    public void setData(List<ExternalGenomeData> data) {
        this.data = data;
        fireTableDataChanged();
    }

    public List<ExternalGenomeData> getData() {
        return data;
    }

    @Override
    public int getRowCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (data == null || rowIndex >= data.size())
            return null;
        ExternalGenomeData genomeData = data.get(rowIndex);
        if(columnIndex < genomeData.getColumnValueMap().size()) {
            String colName = columnNames[columnIndex];
            return genomeData.getColumnValueMap().getOrDefault(colName, "");
        }
        else if (columnIndex == LOAD_COLUMN)
            return "Load";
        else if (columnIndex == INFO_COLUMN)
            return "";
        else
            return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if(data != null && rowIndex < data.size()) {
            ExternalGenomeData genomeData = data.get(rowIndex);
            if(columnIndex == INFO_COLUMN)
                openExternalInfoLink(genomeData);
        }
    }

    private void openExternalInfoLink(ExternalGenomeData genomeData) {
        try {
            Desktop.getDesktop().browse(new URI(genomeData.getInfoLinkUrl()));
        } catch (URISyntaxException | IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == LOAD_COLUMN || columnIndex == INFO_COLUMN;
    }
}
