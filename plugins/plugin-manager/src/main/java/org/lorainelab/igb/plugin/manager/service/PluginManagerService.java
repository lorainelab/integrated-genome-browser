/**
 * Provide services to external packages to support managing Apps.
 * Developed to support appstore bundle, which offers a REST endpoint
 * for managing Apps installed from bundle repositories (app stores).
 */
package org.lorainelab.igb.plugin.manager.service;

/**
 *
 * @author Ann Loraine
 */
public interface PluginManagerService {
    
    /**
     *
     * Install the latest available version of the given App bundle,
     * identified using its symbolic name. It is assumed that IGB already
     * has a listing of available App bundles.
     *
     * @param payload
<<<<<<< HEAD
<<<<<<< HEAD
     * @return ManageAppResponse
=======
     * @return isAppInstalled
>>>>>>> IBGF-1624 : Rest service to manage the lifecycle of app
=======
     * @return ManageAppResponse
>>>>>>> IGBF-1624 : Process callbacks received while managing app lifecycle
     */
    String manageApp(String payload);
    
}
