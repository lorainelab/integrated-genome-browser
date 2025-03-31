package org.lorainelab.igb.services.dynamic.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class GenomeDynamicSearchTableModel extends AbstractTableModel {
    private static final Logger LOG = LoggerFactory.getLogger(GenomeDynamicSearchTableModel.class);
    public static final int COMMON_NAME_COLUMN = 0;
    public static final int SCIENTIFIC_NAME_COLUMN = 1;
    public static final int ASSEMBLY_COLUMN = 2;
    public static final int LOAD_COLUMN = 3;
    public static final int INFO_COLUMN = 4;
    private List<ExternalGenomeData> data;
    private final ExternalGenomeDataProvider externalGenomeDataProvider;
    private final String[] columnNames = {"Common Name", "Scientific Name", "Assembly Version", "", ""};

    public GenomeDynamicSearchTableModel(ExternalGenomeDataProvider externalGenomeDataProvider) {
        this.externalGenomeDataProvider = externalGenomeDataProvider;
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
        return switch (columnIndex) {
            case COMMON_NAME_COLUMN -> genomeData.getCommonName();
            case SCIENTIFIC_NAME_COLUMN -> genomeData.getScientificName();
            case ASSEMBLY_COLUMN -> genomeData.getAssemblyVersion();
            case LOAD_COLUMN -> "Load";
            case INFO_COLUMN -> "";
            default -> null;
        };
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
