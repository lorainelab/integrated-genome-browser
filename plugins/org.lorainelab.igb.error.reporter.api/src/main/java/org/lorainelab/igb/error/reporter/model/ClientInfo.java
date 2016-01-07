package org.lorainelab.igb.error.reporter.model;

import java.util.Set;

/**
 *
 * @author dcnorris
 */
public class ClientInfo {

    private final EnvironmentInfo environmentInfo;
    private final Set<BundleInfo> runtimeBundleInfo;

    public ClientInfo(EnvironmentInfo environmentInfo, Set<BundleInfo> runtimeBundleInfo) {
        this.environmentInfo = environmentInfo;
        this.runtimeBundleInfo = runtimeBundleInfo;
    }

    public EnvironmentInfo getEnvironmentInfo() {
        return environmentInfo;
    }

    public Set<BundleInfo> getRuntimeBundleInfo() {
        return runtimeBundleInfo;
    }

}
