package org.lorainelab.igb.plugin.manager.repos.view;

import java.net.MalformedURLException;
import java.net.URL;
import org.lorainelab.igb.plugin.manager.repos.PluginRepositoryList;
import org.lorainelab.igb.preferences.model.PluginRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author dcnorris
 */
public class BundleRepositoryTableModel extends AbstractTableModel {

    private final PluginRepositoryList pluginRepositoryList;
    private List<PluginRepository> pluginRepositories;
    public static final String REFRESH_COLOMN = "Refresh";
    public static final String NAME_COLOMN = "Name";
    public static final String URL_COLOMN = "URL";
    public static final String ENABLED_COLOMN = "Enabled";
    private final String NAMES[] = {REFRESH_COLOMN, NAME_COLOMN, URL_COLOMN, ENABLED_COLOMN};
    private final Class CLASSES[] = {String.class, String.class, String.class, Boolean.class};
    private PluginRepository selectedPluginRepository;

    public BundleRepositoryTableModel(PluginRepositoryList pluginRepositoryList) {
        this.pluginRepositoryList = pluginRepositoryList;
        this.pluginRepositories = new ArrayList<>(pluginRepositoryList.getPluginRepositories());
    }

    public int getColumnIndex(String columnName) {
        switch (columnName) {
            case REFRESH_COLOMN:
                return 0;
            case NAME_COLOMN:
                return 1;
            case URL_COLOMN:
                return 2;
            case ENABLED_COLOMN:
                return 3;
            default:
                throw new IllegalArgumentException("columnName " + columnName + " is not found");
        }
    }

    public void updateRepositories(Set<PluginRepository> pluginRepositories) {
        this.pluginRepositories = new ArrayList<>(pluginRepositories);
        fireTableDataChanged();
    }

    public void setSelectedRow(int selectedRow) {
        if (selectedRow != -1 && selectedRow <= pluginRepositories.size() - 1) {
            selectedPluginRepository = pluginRepositories.get(selectedRow);
        }
    }

    public PluginRepository getSelectedPluginRepository() {
        return selectedPluginRepository;
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
        if(rowIndex >= pluginRepositories.size()){
            return "";
        }
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

    @Override
    public boolean isCellEditable(int row, int columnIndex) {
        if(row >= pluginRepositories.size()){
            return false;
        }
        PluginRepository pluginRepository = pluginRepositories.get(row);

        switch (getColumnName(columnIndex)) {
            case NAME_COLOMN:
                return true;
            case ENABLED_COLOMN:
                return true;
            case REFRESH_COLOMN:
                return Boolean.valueOf(pluginRepository.getEnabled());
            //IGBF-1902 Make URL Column editable
            case URL_COLOMN:
                return true;
            //end of IGBF-1902
        }

        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if(rowIndex >= pluginRepositories.size()){
            return;
        }
        PluginRepository pluginRepository = pluginRepositories.get(rowIndex);
        switch (getColumnName(columnIndex)) {
            case REFRESH_COLOMN:
                pluginRepositoryList.pluginRepositoryRefreshed(pluginRepository);
            case NAME_COLOMN:
                if (aValue instanceof String) {
                    pluginRepository.setName((String) aValue);
                }
            case ENABLED_COLOMN:
                pluginRepository.setEnabled(!Boolean.valueOf(pluginRepository.getEnabled()));
                pluginRepositoryList.pluginRepoAvailabilityChanged(pluginRepository);
             //IGBF-1902 Update modified URL Value in pluginRepository
               break;
            case URL_COLOMN:
                if (isValidUrl(aValue.toString())) {
                    pluginRepository.setUrl(aValue.toString());
                }
                break;
             //end of IGBF-1902
               
        }
        pluginRepositoryList.updatePluginRepoPrefs(pluginRepository);
        this.fireTableDataChanged();
    }
    
    /**
     * The function checks if the given string is a valid url.
     * 
     * @param url
     * @return 
     */
    public boolean isValidUrl(String url) {
        try {
             new URL(url);
             return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

}
