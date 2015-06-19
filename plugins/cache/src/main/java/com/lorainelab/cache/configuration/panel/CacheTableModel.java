/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.cache.configuration.panel;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.cache.api.RemoteFileCacheService;
import com.lorainelab.cache.disk.RemoteFileDiskCacheService;
import com.lorainelab.cache.disk.RemoteFileDiskCacheService.CacheStatus;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(provide = CacheTableModel.class)
public class CacheTableModel extends AbstractTableModel {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CacheTableModel.class);

    private RemoteFileCacheService remoteFileCacheService;

    List<RemoteFileDiskCacheService.CacheStatus> cacheEntries;

    String[] columnNames = {"Source",
        "Last Modified",
        "Cached On",
        "Size (MB)"};

    @Activate
    public void activate() {
        refresh();
    }

    public void refresh() {
        cacheEntries = remoteFileCacheService.getCacheEntries();
    }

    @Override
    public int getRowCount() {
        return cacheEntries.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CacheStatus cacheStatus = cacheEntries.get(rowIndex);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        switch (columnIndex) {
            case 0:
                return cacheStatus.getUrl();
            case 1:
                Date lastModified = new Date(cacheStatus.getLastModified());
                return sdf.format(lastModified);
            case 2:
                Date cacheLastUpdate = new Date(cacheStatus.getCacheLastUpdate());
                return sdf.format(cacheLastUpdate);
            case 3:
                return cacheStatus.getSize().toString();
            default:
                return "";
        }
    }

    @Reference
    public void setRemoteFileCacheService(RemoteFileCacheService remoteFileCacheService) {
        this.remoteFileCacheService = remoteFileCacheService;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public void removeRow(int row) {
        CacheStatus cacheStatus = cacheEntries.get(row);
        try {
            remoteFileCacheService.clearCacheByUrl(new URL(cacheStatus.getUrl()));
        } catch (MalformedURLException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
