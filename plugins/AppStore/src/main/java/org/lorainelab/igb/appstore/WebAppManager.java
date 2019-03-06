package org.lorainelab.igb.appstore;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.lorainelab.igb.plugin.manager.BundleActionManager;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;
import org.lorainelab.igb.plugin.manager.AppManagerFxPanel;

/**
 * Re-factor this class to become a service interface offered by the Plugin
 * Manager bundle. 
 */
/**
 *
 * @author kkorey
 */
@Component(name ="WebAppManager",immediate = true, provide = {WebAppManager.class})
public class WebAppManager {

    private static final Logger logger = LoggerFactory.getLogger(WebAppManager.class);

    // provided by Plugin Manager module
    private BundleActionManager bundleActionManager; 
    
    //IGBF-1608 : Reference for AppManager      
    private AppManagerFxPanel appManagerFxPanel;
    //IGBF-1608
    
    @Reference
    public void setBundleActionManager(BundleActionManager bundleActionManager) {
        this.bundleActionManager = bundleActionManager;
    }
    
    @Reference
    public void setAppManagerFxPanel(AppManagerFxPanel appManagerFxPanel) {
        this.appManagerFxPanel = appManagerFxPanel;
    }
    /**
     * 
     * This method fetches the plugin from the plugins list stored in AppManagerFxPanel, 
     * installs that plugin and displays installed status in local App Store.
     * 
     * @param symbolicName
     * @return isAppInstalled
     */
    public boolean installApp(String symbolicName){
        //IGBF-1608 : Refactored code and added callback mechanism       
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
        //IGBF-1608 : end
    }
}
