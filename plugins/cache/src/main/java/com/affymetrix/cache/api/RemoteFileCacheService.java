/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.cache.api;

import com.affymetrix.cache.disk.RemoteFileDiskCacheService.CacheStatus;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author jeckstei
 */
public interface RemoteFileCacheService {
    public Optional<InputStream> getFilebyUrl(URL url);
    public void clearAllCaches();
    public void clearCacheByUrl(URL url);
    public boolean cacheExists(URL url);
    public BigInteger getCacheSize();
    public void enforceCacheSize();
    public void enforceEvictionPolicies();
    public List<CacheStatus> getCacheEntries();
    public BigInteger getMaxCacheSizeMB();    
    public BigInteger getMinFileSizeBytes();    
    public BigInteger getCacheExpireMin();
    public void setMaxCacheSizeMB(BigInteger value);    
    public void setMinFileSizeBytes(BigInteger value);    
    public void setCacheExpireMin(BigInteger value);
            
}
