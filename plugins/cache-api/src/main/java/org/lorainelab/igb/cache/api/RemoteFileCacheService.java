/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.cache.api;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author jeckstei
 */
public interface RemoteFileCacheService {

    public Optional<InputStream> getFilebyUrl(URL url, boolean asynchronously);

    public void clearAllCaches();

    public void clearCacheByUrl(URL url);

    public boolean cacheExists(URL url);

    public BigInteger getCacheSizeInMB();

    public void enforceEvictionPolicies();

    public List<CacheStatus> getCacheEntries();
    
    public CacheStatus getCacheStatus(URL url);

    public BigInteger getMaxCacheSizeMB();

    public BigInteger getMinFileSizeBytes();

    public BigInteger getCacheExpireMin();
    
    public boolean getCacheEnabled();

    public void setMaxCacheSizeMB(BigInteger value);

    public void setMinFileSizeBytes(BigInteger value);

    public void setCacheExpireMin(BigInteger value);
    
    public void setCacheEnabled(boolean value);
    
    public void promptToCacheInBackground(URL url, boolean defaultIsYes);
    
    public boolean isCachingInBackground(URL url);
    
    public Date getLastRequestDate(URL url);
    
    public void registerEventListener(Object listener);
    
    public void unregisterEventListener(Object listener); 

}
