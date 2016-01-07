package org.lorainelab.igb.error.reporter.model;

/**
 *
 * @author dcnorris
 */
public class EnvironmentInfo {

    private final String igbVersion;
    private final String osVersionInfo;
    private final String heapInfo;

    public EnvironmentInfo(String igbVersion, String osVersionInfo, String heapInfo) {
        this.igbVersion = igbVersion;
        this.osVersionInfo = osVersionInfo;
        this.heapInfo = heapInfo;
    }

    public String getIgbVersion() {
        return igbVersion;
    }

    public String getOsVersionInfo() {
        return osVersionInfo;
    }

    public String getHeapInfo() {
        return heapInfo;
    }


}
