package com.affymetrix.igb.general;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderComparator;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.EventService;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.general.DataProviderManager.DataProviderServiceChangeEvent;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Enabled;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Name;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Refresh;
import com.affymetrix.igb.swing.jide.StyledJTable;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = DataProviderTableModel.COMPONENT_NAME, immediate = true, provide = DataProviderTableModel.class)
public final class DataProviderTableModel extends AbstractTableModel {

    private static final Logger LOG = LoggerFactory.getLogger(DataProviderTableModel.class);
    public static final String COMPONENT_NAME = "DataProviderTableModel";
    private DataProviderManager dataProviderManager;
    private EventService eventService;
    private EventBus eventBus;
    private IgbService igbService;
    private GeneralLoadView loadView;

    public static enum DataProviderTableColumn {
        Refresh, Name, Type, URL, Enabled,Info
    }
    public static enum DataSourceInfoTableColumn {
        name,factoryName,URL, primaryLinkoutUrl,mirroURL, enabled,useMirror,loadPriority, status ,speciesInfo, genomeVersionSynonyms, supportedGenomeVersionInfo,chromosomeSynonymReference,preferencesNode,defaultDataProviderId
    }
    private final List<DataProviderTableColumn> tableColumns;
    private List<DataProvider> sortedDataProviders;
    private boolean temporarilyDisableRefresh; //only allow 1 server to be refreshed at a time
    private GenometryModel gmodel;

    public DataProviderTableModel() {
        gmodel = GenometryModel.getInstance();
        loadView = GeneralLoadView.getLoadView();
        temporarilyDisableRefresh = false;
        tableColumns = Lists.newArrayList(DataProviderTableColumn.values());
        sortDataSources();

    }

    @Activate
    public void activate() {
        eventBus = eventService.getEventBus();
        eventBus.register(this);
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void setDataProviderManager(DataProviderManager dataProviderManager) {
        this.dataProviderManager = dataProviderManager;
    }

    @Reference
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    @Subscribe
    public void refreshEvent(DataProviderServiceChangeEvent event) {
        sortDataSources();
    }

    public void sortDataSources() {
        sortedDataProviders = Lists.newArrayList(DataProviderManager.getAllServers());
        Collections.sort(sortedDataProviders, new DataProviderComparator());
        ThreadUtils.runOnEventQueue(() -> fireTableDataChanged());
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    int getColumnIndex(DataProviderTableColumn dataProviderTableColumn) {
        return tableColumns.indexOf(dataProviderTableColumn);
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Refresh";
        } else if (column == 1) {
            return "Name";
        } else if (column == 2) {
            return "Type";
        } else if (column == 3) {
            return "URL";
        } else if (column == 4) {
            return "Enabled";
        } else if (column == 5) {
            return "Info";
        }

        throw new IllegalStateException();
    }

    DataProvider getElementAt(int row) {
        return sortedDataProviders.get(row);
    }

    int getRowFromDataProvider(DataProvider dataProvider) {
        return sortedDataProviders.indexOf(dataProvider);
    }

    public Object getColumnValue(DataProvider dataProvider, int columnIndex) {
        if (columnIndex >= getColumnCount()) {
            throw new IllegalStateException();
        }
        switch (tableColumns.get(columnIndex)) {
            case Refresh:
                return "";
            case Name: {
                try {
                    return URLDecoder.decode(dataProvider.getName(), "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    return dataProvider.getName();
                }
            }
            case Type:
                return dataProvider.getFactoryName().get();
            case URL:
                try {
                    return URLDecoder.decode(dataProvider.getUrl(), "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    return dataProvider.getUrl();
                }
            case Enabled:
                return dataProvider.getStatus() != ResourceStatus.Disabled;
            case Info:
                return "";
            default:
                throw new IllegalArgumentException("columnIndex " + columnIndex + " is out of range");
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        if (columnIndex >= getColumnCount()) {
            throw new IllegalStateException();
        }
        switch (tableColumns.get(columnIndex)) {
            case Refresh: {
                return ImageIcon.class;
            }
            case Info: {
                return ImageIcon.class;
            }
            case Enabled: {
                return Boolean.class;
            }
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        DataProvider dataProvider = sortedDataProviders.get(rowIndex);
        return isEditable(dataProvider, columnIndex);
    }

    public boolean isEditable(DataProvider dataProvider, int columnIndex) {
        //IGBF-1502  Make default datasources in Preferences uneditable.
        boolean isEditable = false;
        if (dataProvider.useMirrorUrl()) {
            isEditable = PreferenceUtils.getDataProviderNode(dataProvider.getUrl()).getBoolean(DataProviderPrefKeys.IS_EDITABLE, false);
        } else {
            isEditable = PreferenceUtils.getDataProviderNode(dataProvider.getUrl()).getBoolean(DataProviderPrefKeys.IS_EDITABLE, true);
        }
        //END IGBF-1502
        switch (tableColumns.get(columnIndex)) {
            case Refresh: {
                return dataProvider.getStatus() != ResourceStatus.Disabled;
            }
            case Info: {
                return dataProvider.getStatus() != ResourceStatus.Disabled;
            }
            case Name: {
                return isEditable;
            }
            case Enabled: {
                return true;
            }
            default:
                return false;
        }
    }


    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue instanceof String && Strings.isNullOrEmpty((String) aValue)) {
            aValue = getValueAt(rowIndex, columnIndex);
        }
        DataProvider dataProvider = sortedDataProviders.get(rowIndex);
        setColumnValue(dataProvider, aValue, rowIndex, columnIndex);
    }

    public void setColumnValue(DataProvider dataProvider, Object editedValue, int rowindex, int column) {
        switch (tableColumns.get(column)) {
            case Refresh:
                if ((Boolean) getValueAt(rowindex, getColumnIndex(DataProviderTableColumn.Enabled))) {
                    if (dataProvider.getStatus() != ResourceStatus.Disabled
                            && confirmRefresh()) {
                        if (!temporarilyDisableRefresh) {
                            temporarilyDisableRefresh = true;
                            CompletableFuture.runAsync(() -> {
                                dataProviderManager.disableDataProvider(dataProvider);
                            }).whenComplete((result, ex) -> {
                                dataProviderManager.enableDataProvider(dataProvider);
                                temporarilyDisableRefresh = false;
                                fireTableRowsUpdated(sortedDataProviders.indexOf(dataProvider), sortedDataProviders.indexOf(dataProvider));
                            });
                        }
                    }
                }

                fireTableRowsUpdated(sortedDataProviders.indexOf(dataProvider), sortedDataProviders.indexOf(dataProvider));
                break;
            case Enabled:
                if ((Boolean) editedValue) {
                    dataProviderManager.enableDataProvider(dataProvider);
                } else if (confirmDelete()) {
                    CompletableFuture.runAsync(() -> {
                        dataProviderManager.disableDataProvider(dataProvider);
                    }).whenComplete((result, ex) -> {
                        fireTableRowsUpdated(sortedDataProviders.indexOf(dataProvider), sortedDataProviders.indexOf(dataProvider));
                    });
                }
                break;
            case Name:
                dataProvider.setName((String) editedValue);
                if ((Boolean) getValueAt(rowindex, getColumnIndex(DataProviderTableColumn.Enabled))) {
                    loadView.refreshTreeViewAndRestore();
                }
                break;
            case URL:
                //do nothing
                break;
            case Type:
                //do nothing
                break;
            case Info:
                infoActionOnIconClick(dataProvider);
                break;
            default: {
                throw new IllegalArgumentException("columnIndex " + column + " not editable");
            }

        }

    }

    @Override
    public int getRowCount() {
        return sortedDataProviders.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DataProvider dataProvider = sortedDataProviders.get(rowIndex);
        return getColumnValue(dataProvider, columnIndex);
    }

    public boolean confirmRefresh() {
        String message = "Warning:\n"
                + "Refreshing the server will force IGB to re-read configuration files from the server.\n"
                + "This means all data sets currently loaded from the server will be deleted.\n"
                + "This is useful mainly for setting up or configuring a QuickLoad site.";

        return ModalUtils.confirmPanel(
                message, PreferenceUtils.getTopNode(),
                PreferenceUtils.CONFIRM_BEFORE_REFRESH,
                PreferenceUtils.default_confirm_before_refresh);
    }

    public boolean confirmDelete() {
        String message = "Warning:\n"
                + "Disabling or removing a server will cause any"
                + " currently loaded data from that server to be removed from IGB.\n";

        return ModalUtils.confirmPanel(
                message, PreferenceUtils.getTopNode(),
                PreferenceUtils.CONFIRM_BEFORE_DELETE,
                PreferenceUtils.default_confirm_before_delete);
    }

    public void infoActionOnIconClick(DataProvider  dataProvider)
    {

        JFrame messageFrame = new JFrame();
        JTable infoDsTable = buildQuickLoadTableDetails(dataProvider);
        JScrollPane scroll_pane = new JScrollPane(infoDsTable,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messageFrame.add(scroll_pane);
        messageFrame.setTitle(dataProvider.getName());
        messageFrame.setSize(new Dimension(450, 50));
        messageFrame.setMinimumSize(new Dimension(450, 50));
        infoDsTable.getColumnModel().getColumn(0).setMinWidth(150);
        infoDsTable.getColumnModel().getColumn(1).setMinWidth(350);

        infoDsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        messageFrame.pack();
        messageFrame.setLocationRelativeTo(IGB.getInstance().getFrame());
        messageFrame.setVisible(true);
    }


    private StyledJTable buildQuickLoadTableDetails(DataProvider dataProvider)
    {
        LinkedList<String[][]> rowColumnMap = getDataSourceInfoTableColumns(dataProvider);
        if(!rowColumnMap.isEmpty())
        {

            String[][] row = new String[rowColumnMap.size()][2];
            String[] column = new String[]{"Property","Value"};

            int rowCounter = 0;
            while(rowCounter < rowColumnMap.size()){
                row[rowCounter][0] = rowColumnMap.get(rowCounter)[0][0];
                row[rowCounter][1] = rowColumnMap.get(rowCounter)[0][1];
                rowCounter++;
            }
            StyledJTable infoDsTable = new StyledJTable(row,column){
                public boolean isCellEditable(int row, int column){
                    return false;
                }
                public String getToolTipText(MouseEvent e) {
                    String tip = null;
                    java.awt.Point p = e.getPoint();
                    int rowIndex = rowAtPoint(p);
                    int colIndex = columnAtPoint(p);
                    try {
                        tip = getValueAt(rowIndex, colIndex).toString();
                    } catch (RuntimeException e1) {
                        //catch null pointer exception if mouse is over an empty line
                    }
                    return tip;
                }
            };
            infoDsTable.setBackground(infoDsTable.getBackground());
            return infoDsTable;
        }

        return null;

    }

    private LinkedList<String[][]> getDataSourceInfoTableColumns(DataProvider dataProvider)
    {
        LinkedList<String[][]> dataSourceInfo = new LinkedList<>();

        if(dataProvider.getName() != null)
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.name.toString(), dataProvider.getName()}});
        if(dataProvider.getFactoryName().isPresent())
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.factoryName.toString(),dataProvider.getFactoryName().get()}});
        if(dataProvider.getUrl() != null)
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.URL.toString(),dataProvider.getUrl()}});
        if(dataProvider.getStatus() != null)
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.status.toString(),dataProvider.getStatus().getName()}});

        if(dataProvider.getPrimaryLinkoutUrl().isPresent())
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.primaryLinkoutUrl.toString(), dataProvider.getPrimaryLinkoutUrl().get()}});

        if(dataProvider.getMirrorUrl().isPresent())
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.mirroURL.toString(), dataProvider.getMirrorUrl().get()}});
        dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.loadPriority.toString(),dataProvider.getLoadPriority()+""}});

        if(dataProvider.getSpeciesInfo().get().size() > 0)
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.speciesInfo.toString(),dataProvider.getSpeciesInfo().get().toString()}});
        if(!dataProvider.getSupportedGenomeVersionNames().isEmpty())
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.supportedGenomeVersionInfo.toString(),dataProvider.getSupportedGenomeVersionNames().toString()}});
        if(dataProvider.getGenomeVersionSynonyms().get().size() > 0)
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.genomeVersionSynonyms.toString(),dataProvider.getGenomeVersionSynonyms().get().toString()}});
        if(((BaseDataProvider)dataProvider) != null)
            dataSourceInfo.add(new String[][]{{DataSourceInfoTableColumn.defaultDataProviderId.toString(),((BaseDataProvider)dataProvider).getId()}});

        return dataSourceInfo;
    }

}
