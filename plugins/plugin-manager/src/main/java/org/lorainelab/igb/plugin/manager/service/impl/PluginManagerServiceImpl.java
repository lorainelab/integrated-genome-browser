/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager.service.impl;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import java.util.function.Function;
import org.lorainelab.igb.plugin.manager.AppManagerFxPanel;
import org.lorainelab.igb.plugin.manager.BundleActionManager;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import org.lorainelab.igb.plugin.manager.service.PluginManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This annotation declares the class as an SCR component and specifies
 * that it should be immediately activated once its dependencies have been
 * satisfied. Additionally, because this class implements an interface, it will 
 * automatically be registered as a provider of the PluginManagerService interface.
 * 
 * Alternatively, if we could have explicitly declared the provided interface using
 * the 'provide' annotation parameter (e.g. @Component(immediate = true, provide = GreetingService.class))
 *
 * @author Ann Loraine, Riddhi Patil
 */
@Component(immediate = true)
public class PluginManagerServiceImpl implements PluginManagerService {

    private static final Logger logger = LoggerFactory.getLogger(PluginManagerServiceImpl.class);
    
    private static final String INSTALL_APP = "install";
    private static final String UNINSTALL_APP = "uninstall";
    private static final String UPDATE_APP = "update";
    private static final String APP_INFO = "getInfo";
    private static final String UNKNOWN_ACTION = "UNKNOWN_ACTION";

    enum AppStatus {
        INSTALLED, UPDATED, UNINSTALLED, NOT_FOUND, ERROR;
    }

    private BundleActionManager bundleActionManager; 
    
    private AppManagerFxPanel appManagerFxPanel;
    
    @Reference
    public void setBundleActionManager(BundleActionManager bundleActionManager) {
        this.bundleActionManager = bundleActionManager;
    }
    
    @Reference
    public void setAppManagerFxPanel(AppManagerFxPanel appManagerFxPanel) {
        this.appManagerFxPanel = appManagerFxPanel;
    }
    
    @Override
    public String manageApp(String payload) {
         
        JsonObject body = new JsonParser().parse(payload).getAsJsonObject();
        
        String symbolicName = body.get("symbolicName").getAsString();
        String action = body.get("action").getAsString();
        
        final PluginListItemMetadata plugin = appManagerFxPanel.getListView().getItems().stream()
            .filter(plugins ->plugins.getBundle().getSymbolicName().equals(symbolicName)).findAny()    
            .orElse(null);
        if(plugin != null) {
            switch (action) {
                case INSTALL_APP:               
                        return installApp(plugin);                        
                case UNINSTALL_APP:
                        return uninstallApp(plugin);
                case UPDATE_APP:
                        return updateApp(plugin);
                case APP_INFO:   
                        return getAppInfo(plugin);
                default:
                        return createManageAppResponse(UNKNOWN_ACTION, "-", symbolicName);

            }
        }
        return createManageAppResponse(AppStatus.NOT_FOUND.toString(), "-", symbolicName);
        
    }

    private String installApp(PluginListItemMetadata plugin) {
        final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
            logger.debug("Callback called for bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
            plugin.setIsInstalled(Boolean.TRUE);
            plugin.setIsBusy(Boolean.FALSE);
            appManagerFxPanel.getListView().setItems(appManagerFxPanel.getListView().getItems());
            return Void.TYPE;
        };
            
        bundleActionManager.installBundle(plugin,functionCallback); 
        logger.info("Installed App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
        plugin.getRepository());
        if(plugin.getIsInstalled().getValue())
            return createManageAppResponse(AppStatus.INSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
        else 
            return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
       
    }
    
    private String uninstallApp(PluginListItemMetadata plugin) {
       final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {            
            logger.debug("Callback called for bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
            plugin.setIsBusy(Boolean.FALSE);
            plugin.setIsInstalled(Boolean.FALSE);
            plugin.setIsUpdatable(Boolean.FALSE);               
           
            return Void.TYPE;
        };
           
        bundleActionManager.uninstallBundle(plugin, functionCallback);
        logger.info("Uninstalled App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
        plugin.getRepository());
        if(!plugin.getIsInstalled().getValue())
            return createManageAppResponse(AppStatus.UNINSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
        else 
            return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
    }
    
    private String updateApp(PluginListItemMetadata plugin) {
        final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
            logger.debug("Callback called for bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
            plugin.setIsBusy(Boolean.FALSE);              
            
            return Void.TYPE;
        };
            
        bundleActionManager.updateBundle(plugin, functionCallback);
        logger.info("Updated App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
        plugin.getRepository());
        if(!plugin.getIsInstalled().getValue())
            return createManageAppResponse(AppStatus.UPDATED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
        else 
            return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
    }
         
    private String getAppInfo(PluginListItemMetadata plugin) {
        if(plugin.getIsInstalled().getValue()) {
            return createManageAppResponse(AppStatus.INSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
        } else {
            return createManageAppResponse(AppStatus.UNINSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
        } 
    }
    
    private String createManageAppResponse(String appStatus, String version, String symbolicName) {
       
        JsonObject respose = new JsonObject();
        respose.addProperty("status", appStatus);
        respose.addProperty("version", version);
        respose.addProperty("symbolicName", symbolicName);
        
        return new Gson().toJson(respose);
    }
       
}
