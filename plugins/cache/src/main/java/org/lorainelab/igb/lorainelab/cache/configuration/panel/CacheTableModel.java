/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.cache.configuration.panel;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.lorainelab.igb.cache.api.CacheStatus;
import org.lorainelab.igb.cache.api.RemoteFileCacheService;
import java.math.BigInteger;
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

    List<CacheStatus> cacheEntries;

    String[] columnNames = {"Source",
        "Last Modified",
        "Cached On",
        "Last Accessed",
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
                try {
                    return cacheStatus.getUrl();
                } catch (Exception ex) {
                    return "";
                }
            case 1:
                try {
                    Date lastModified = new Date(cacheStatus.getLastModified());
                    return lastModified;
                } catch (Exception ex) {
                    return new Date();
                }
            case 2:
                try {
                    Date cacheLastUpdate = new Date(cacheStatus.getCacheLastUpdate());
                    return cacheLastUpdate;
                } catch (Exception ex) {
                    return new Date();
                }
            case 3:
                Date lastAccessed;
                try {
                    lastAccessed = remoteFileCacheService.getLastRequestDate(new URL(cacheStatus.getUrl()));
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    lastAccessed = new Date();
                }
                return lastAccessed;
            case 4:
                try {
                    BigInteger size = cacheStatus.getSize();
                    if (size.compareTo(BigInteger.ZERO) <= 0) {
                        return "<1";
                    } else {
                        return cacheStatus.getSize();
                    }
                } catch (Exception ex) {
                    return 0;
                }
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
        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return Date.class;
            case 2:
                return Date.class;
            case 3:
                return Date.class;
            case 4:
                return BigInteger.class;
            default:
                throw new IllegalArgumentException();
        }
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
