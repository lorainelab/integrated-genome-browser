/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.cache.configuration.panel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 *
 * @author dfreese
 */

public class CacheEntry {
    private final StringProperty url; 
    private final StringProperty lastModified; 
    private final StringProperty cacheUpdate;
    private final StringProperty lastAccessed; 
    private final StringProperty cacheSize; 

    public CacheEntry(String url,String lastModified,String cacheUpdate,String lastAccessed,String cacheSize){
        this.url = new SimpleStringProperty(url);  
        this.lastModified = new SimpleStringProperty(lastModified); 
        this.cacheUpdate = new SimpleStringProperty(cacheUpdate); 
        this.lastAccessed = new SimpleStringProperty(lastAccessed); 
        this.cacheSize = new SimpleStringProperty(cacheSize); 
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url.get(); 
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url.set(url);
    }
    public StringProperty getUrlStringProperty(){
        return url; 
    }

    /**
     * @return the lastModified
     */
    public String getLastModified() {
        return lastModified.get();
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(String lastModified) {
        this.lastModified.set(lastModified);
    }
    
    public StringProperty getLastModifiedStringProperty(){
        return lastModified; 
    }

    /**
     * @return the cacheUpdate
     */
    public String getCacheUpdate() {
        return cacheUpdate.get(); 
    }

    /**
     * @param cacheUpdate the cacheUpdate to set
     */
    public void setCacheUpdate(String cacheUpdate) {
        this.cacheUpdate.set(cacheUpdate);
    }
    public StringProperty getCacheUpdateStringProperty(){
        return cacheUpdate; 
    }

    /**
     * @return the lastAccessed
     */
    public String getLastAccessed() {
        return lastAccessed.get();
    }

    public StringProperty getLastAccessedStringProperty(){
        return lastAccessed; 
    }
    /**
     * @param lastAccessed the lastAccessed to set
     */
    public void setLastAccessed(String lastAccessed) {
        this.lastAccessed.set(lastAccessed);
    }

    /**
     * @return the cacheSize
     */
    public String getCacheSize() {
        return cacheSize.get();
    }
    public StringProperty getCacheSizeStringProperty(){
        return cacheSize; 
    }

    /**
     * @param cacheSize the cacheSize to set
     */
    public void setCacheSize(String cacheSize) {
        this.cacheSize.set(cacheSize);
    }
    
    
    
}
