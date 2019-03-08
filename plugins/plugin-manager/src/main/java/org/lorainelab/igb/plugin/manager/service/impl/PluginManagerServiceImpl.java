/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager.service.impl;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
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
    public boolean installApp(String symbolicName) {
        final PluginListItemMetadata plugin = appManagerFxPanel.getListView().getItems().stream()
            .filter(plugins ->plugins.getBundle().getSymbolicName().equals(symbolicName)).findAny()    
            .orElse(null);
        
        if(plugin!=null){        
            
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                logger.debug("Callback called for bundle with symbolic name: {}", symbolicName);
                plugin.setIsInstalled(Boolean.TRUE);
                plugin.setIsBusy(Boolean.FALSE);
                appManagerFxPanel.getListView().setItems(appManagerFxPanel.getListView().getItems());
                return Void.TYPE;
            };
            
            bundleActionManager.installBundle(plugin,functionCallback); 
            logger.info("Installed App {} version {} from {}",symbolicName,plugin.getVersion(),
                plugin.getRepository());
            return true;
        }
        return false;
    }
    
}
