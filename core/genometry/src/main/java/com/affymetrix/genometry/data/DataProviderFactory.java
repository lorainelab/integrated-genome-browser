package com.affymetrix.genometry.data;

/**
 *
 * @author dcnorris
 */
public interface DataProviderFactory {

    /**
     * @return A user friendly Name which will presented in the UI to user's when adding DataProviders
     */
    public String getFactoryName();

    public DataProvider createDataProvider(String url, String name, int loadPriority);

    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority);

    /**
     * @return if user's should be allowed to create local file system based instances of this type
     */
    public default boolean supportsLocalFileInstances() {
        return false;
    }
}
