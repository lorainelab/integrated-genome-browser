/**
 * Provide services to external packages to support managing Apps.
 * Developed to support appstore bundle, which offers a REST endpoint
 * for managing Apps installed from bundle repositories (app stores).
 */
package org.lorainelab.igb.plugin.manager.service;

import com.google.gson.JsonObject;
import fi.iki.elonen.NanoHTTPD.Response;

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
     * @param requestBody
     * @return ManageAppResponse
     */
    Response manageApp(JsonObject requestBody);
    
}
