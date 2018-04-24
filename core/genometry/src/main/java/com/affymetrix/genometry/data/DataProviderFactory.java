package com.affymetrix.genometry.data;

import com.affymetrix.genometry.util.Weighted;

/**
 *
 * @author dcnorris
 */
public interface DataProviderFactory extends Weighted {

    /**
     * @return A user friendly Name which will presented in the UI to user's when adding DataProviders
     */
    public String getFactoryName();

    public DataProvider createDataProvider(String url, String name, int loadPriority);

    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority);
    
    public DataProvider createDataProvider(String url, String name, int loadPriority, String id);

    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority, String id);

    /**
     * @return if user's should be allowed to create local file system based instances of this type
     */
    public default boolean supportsLocalFileInstances() {
        return false;
    }
}
