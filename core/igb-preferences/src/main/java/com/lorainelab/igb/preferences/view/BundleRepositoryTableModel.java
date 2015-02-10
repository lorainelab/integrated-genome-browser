package com.lorainelab.igb.preferences.view;

import com.lorainelab.igb.preferences.model.PluginRepository;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author dcnorris
 */
public class BundleRepositoryTableModel extends AbstractTableModel {

    private final List<PluginRepository> pluginRepositories;

    public BundleRepositoryTableModel(List<PluginRepository> pluginRepositories) {
        this.pluginRepositories = pluginRepositories;
    }
    private static final String REFRESH_COLOMN = "Refresh";
    private static final String NAME_COLOMN = "Name";
    private static final String URL_COLOMN = "URL";
    private static final String ENABLED_COLOMN = "Enabled";
    private final String NAMES[] = {REFRESH_COLOMN, NAME_COLOMN, URL_COLOMN, ENABLED_COLOMN};
    private final Class CLASSES[] = {String.class, String.class, String.class, Boolean.class};

    public void addPluginRepository(PluginRepository pluginRepository) {
        pluginRepositories.add(pluginRepository);
        fireTableDataChanged();
    }

    public void removePluginRepository(PluginRepository pluginRepository) {
        if (pluginRepositories.contains(pluginRepository)) {
            pluginRepositories.remove(pluginRepository);
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return pluginRepositories.size();
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return CLASSES[col];
    }

    @Override
    public int getColumnCount() {
        return NAMES.length;
    }

    @Override
    public String getColumnName(int col) {
        return NAMES[col];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PluginRepository pluginRepository = pluginRepositories.get(rowIndex);
        switch (getColumnName(columnIndex)) {
            case REFRESH_COLOMN:
                return "";
            case NAME_COLOMN:
                return pluginRepository.getName();
            case URL_COLOMN:
                return pluginRepository.getUrl();
            case ENABLED_COLOMN:
                return Boolean.valueOf(pluginRepository.getEnabled());
            default:
                throw new IllegalArgumentException("columnIndex " + columnIndex + " is out of range");
        }
    }

}
