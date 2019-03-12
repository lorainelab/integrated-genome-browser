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
<<<<<<< HEAD
<<<<<<< HEAD
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
=======
>>>>>>> IBGF-1624 : Rest service to manage the lifecycle of app
=======
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
>>>>>>> IGBF-1624 : Process callbacks received while managing app lifecycle
import java.util.function.Function;
import java.util.logging.Level;
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
<<<<<<< HEAD
        INSTALLED, UPDATED, UNINSTALLED, APP_NOT_FOUND, ERROR;
=======
        INSTALLED, UPDATED, UNINSTALLED, NOT_FOUND, ERROR;
>>>>>>> IBGF-1624 : Rest service to manage the lifecycle of app
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
<<<<<<< HEAD
        return createManageAppResponse(AppStatus.APP_NOT_FOUND.toString(), "-", symbolicName);
        
    }

    /**
     * 
     * @param plugin
     * @return Response
     * 
     *  - Calls the BundleActionManager installBundle api to install the plugin
     *  - Once the bundle is installed and the callback is processed we will 
     *    return the Response ( Installed or Error ) back to the client. This is 
     *    achieved by using FutureTask which will prevent returning the Response 
     *    until callback is processed.
     */
    private String installApp(PluginListItemMetadata plugin) {
        try {
            final FutureTask<Object> ft = new FutureTask<>(() -> {}, new Object());
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if(t) {
                    logger.debug("Callback called for installed bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
                    plugin.setIsInstalled(Boolean.TRUE);
                    plugin.setIsBusy(Boolean.FALSE);                    
                    appManagerFxPanel.getListView().setItems(appManagerFxPanel.getListView().getItems());
                    
                } 
                ft.run();
                return Void.TYPE;
            };
            
            bundleActionManager.installBundle(plugin,functionCallback);
            ft.get();
            logger.info("Installed App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
                    plugin.getRepository());
            if(plugin.getIsInstalled().getValue())
                return createManageAppResponse(AppStatus.INSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
            
        } catch (InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(PluginManagerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
       return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
    }
    
    /**
     * 
     * @param plugin
     * @return Response
     * 
     *  - Calls the BundleActionManager uninstallBundle api to uninstall the plugin
     *  - Once the bundle is uninstalled and the callback is processed we will 
     *    return the Response ( Uninstalled or Error ) back to the client. This is 
     *    achieved by using FutureTask which will prevent returning the Response 
     *    until callback is processed.
     */
    private String uninstallApp(PluginListItemMetadata plugin) {
       
        try {
            final FutureTask<Object> ft = new FutureTask<>(() -> {}, new Object());
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if(t) {
                    
                    logger.debug("Callback called for uninstalled bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
                    plugin.setIsBusy(Boolean.FALSE);
                    plugin.setIsInstalled(Boolean.FALSE);
                    plugin.setIsUpdatable(Boolean.FALSE);
                                      
                }
                ft.run();
                return Void.TYPE;
            };
            
            bundleActionManager.uninstallBundle(plugin, functionCallback);
            ft.get();
            logger.info("Uninstalled App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
                    plugin.getRepository());
            if(!plugin.getIsUpdatable().getValue())
                return createManageAppResponse(AppStatus.UNINSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
            
        } catch (InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(PluginManagerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
        
    }
    
    /**
     * 
     * @param plugin
     * @return Response
     * 
     *  - Calls the BundleActionManager updateBundle api to update the plugin
     *  - Once the bundle is updated and the callback is processed we will 
     *    return the Response ( Updated or Error ) back to the client. This is 
     *    achieved by using FutureTask which will prevent returning the Response 
     *    until callback is processed.
     */
    private String updateApp(PluginListItemMetadata plugin) {
        try {
            final FutureTask<Object> ft = new FutureTask<>(() -> {}, new Object());
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if(t) {
                    logger.debug("Callback called for update bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
                    plugin.setIsBusy(Boolean.FALSE); 
                    plugin.setIsUpdatable(Boolean.FALSE); 
                }
                ft.run();
                return Void.TYPE;
            };
            
            bundleActionManager.updateBundle(plugin, functionCallback);
            ft.get();
            logger.info("Updated App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
                    plugin.getRepository());
            if(!plugin.getIsUpdatable().getValue())
                return createManageAppResponse(AppStatus.UPDATED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
            
        } catch (InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(PluginManagerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
    }
         
    /**
     * 
     * @param plugin
     * @return Response
     * 
     * Returns the status of the app, whether it is installed or uninstalled
     */
=======
        return createManageAppResponse(AppStatus.NOT_FOUND.toString(), "-", symbolicName);
        
    }

    /**
     * 
     * @param plugin
     * @return Response
     * 
     *  - Calls the BundleActionManager installBundle api to install the plugin
     *  - Once the bundle is installed and the callback is processed we will 
     *    return the Response ( Installed or Error ) back to the client. This is 
     *    achieved by using FutureTask which will prevent returning the Response 
     *    until callback is processed.
     */
    private String installApp(PluginListItemMetadata plugin) {
        try {
            final FutureTask<Object> ft = new FutureTask<>(() -> {}, new Object());
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if(t) {
                    logger.debug("Callback called for installed bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
                    plugin.setIsInstalled(Boolean.TRUE);
                    plugin.setIsBusy(Boolean.FALSE);                    
                    appManagerFxPanel.getListView().setItems(appManagerFxPanel.getListView().getItems());
                    
                } 
                ft.run();
                return Void.TYPE;
            };
            
            bundleActionManager.installBundle(plugin,functionCallback);
            ft.get();
            logger.info("Installed App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
                    plugin.getRepository());
            if(plugin.getIsInstalled().getValue())
                return createManageAppResponse(AppStatus.INSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
            
        } catch (InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(PluginManagerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
       return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
    }
    
    /**
     * 
     * @param plugin
     * @return Response
     * 
     *  - Calls the BundleActionManager uninstallBundle api to uninstall the plugin
     *  - Once the bundle is uninstalled and the callback is processed we will 
     *    return the Response ( Uninstalled or Error ) back to the client. This is 
     *    achieved by using FutureTask which will prevent returning the Response 
     *    until callback is processed.
     */
    private String uninstallApp(PluginListItemMetadata plugin) {
       
        try {
            final FutureTask<Object> ft = new FutureTask<>(() -> {}, new Object());
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if(t) {
                    
                    logger.debug("Callback called for uninstalled bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
                    plugin.setIsBusy(Boolean.FALSE);
                    plugin.setIsInstalled(Boolean.FALSE);
                    plugin.setIsUpdatable(Boolean.FALSE);
                                      
                }
                ft.run();
                return Void.TYPE;
            };
            
            bundleActionManager.uninstallBundle(plugin, functionCallback);
            ft.get();
            logger.info("Uninstalled App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
                    plugin.getRepository());
            if(!plugin.getIsInstalled().getValue())
                return createManageAppResponse(AppStatus.UNINSTALLED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
            
        } catch (InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(PluginManagerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
        
    }
    
    /**
     * 
     * @param plugin
     * @return Response
     * 
     *  - Calls the BundleActionManager updateBundle api to update the plugin
     *  - Once the bundle is updated and the callback is processed we will 
     *    return the Response ( Updated or Error ) back to the client. This is 
     *    achieved by using FutureTask which will prevent returning the Response 
     *    until callback is processed.
     */
    private String updateApp(PluginListItemMetadata plugin) {
        try {
            final FutureTask<Object> ft = new FutureTask<>(() -> {}, new Object());
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if(t) {
                    logger.debug("Callback called for update bundle with symbolic name: {}", plugin.getBundle().getSymbolicName());
                    plugin.setIsBusy(Boolean.FALSE);                    
                }
                ft.run();
                return Void.TYPE;
            };
            
            bundleActionManager.updateBundle(plugin, functionCallback);
            ft.get();
            logger.info("Updated App {} version {} from {}",plugin.getBundle().getSymbolicName(),plugin.getVersion(),
                    plugin.getRepository());
            if(!plugin.getIsInstalled().getValue())
                return createManageAppResponse(AppStatus.UPDATED.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
            
        } catch (InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(PluginManagerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return createManageAppResponse(AppStatus.ERROR.toString(), plugin.getVersion().getValue(), plugin.getBundle().getSymbolicName());
    }
         
<<<<<<< HEAD
>>>>>>> IBGF-1624 : Rest service to manage the lifecycle of app
=======
    /**
     * 
     * @param plugin
     * @return Response
     * 
     * Returns the status of the app, whether it is installed or uninstalled
     */
>>>>>>> IGBF-1624 : Process callbacks received while managing app lifecycle
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
