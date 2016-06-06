package com.affymetrix.igb.general;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderComparator;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.EventService;
import com.affymetrix.igb.general.DataProviderManager.DataProviderServiceChangeEvent;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Enabled;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Name;
import static com.affymetrix.igb.general.DataProviderTableModel.DataProviderTableColumn.Refresh;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.ImageIcon;
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

        Refresh, Name, Type, URL, Enabled
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
        return 5;
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
        boolean isEditable = PreferenceUtils.getDataProviderNode(dataProvider.getUrl()).getBoolean(DataProviderPrefKeys.IS_EDITABLE, true);
        switch (tableColumns.get(columnIndex)) {
            case Refresh: {
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
                        if (temporarilyDisableRefresh) {
                            CompletableFuture.runAsync(() -> {
                                temporarilyDisableRefresh = true;
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
}
