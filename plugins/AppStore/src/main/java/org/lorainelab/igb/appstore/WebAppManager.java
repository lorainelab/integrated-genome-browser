package org.lorainelab.igb.appstore;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.lorainelab.igb.plugin.manager.BundleActionManager;
import org.lorainelab.igb.plugin.manager.BundleInfoManager;
import org.lorainelab.igb.plugin.manager.RepositoryInfoManager;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;

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
    private BundleInfoManager bundleInfoManager;
    private BundleActionManager bundleActionManager;
    private RepositoryInfoManager repositoryInfoManager;


    @Reference
    public void setBundleInfoManager(BundleInfoManager bundleInfoManager) {
        this.bundleInfoManager = bundleInfoManager;
    }

    @Reference
    public void setBundleActionManager(BundleActionManager bundleActionManager) {
        this.bundleActionManager = bundleActionManager;
    }

    
    @Reference
    public void setRepositoryInfoManager(RepositoryInfoManager repositoryInfoManager) {
        this.repositoryInfoManager = repositoryInfoManager;
    }
    
 
    public void installApp(String symbolicName){
        Bundle bundle = bundleInfoManager.getRepositoryManagedBundles().stream()
                .filter(plugin -> plugin.getSymbolicName().equals(symbolicName)).findAny().orElse(null);

        final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
            logger.debug("Callback called for bundle with symbolic name: {}", symbolicName);
            return Void.TYPE;
        };
        if(bundle!=null){
            final boolean isInstalled = bundleInfoManager.isVersionOfBundleInstalled(bundle);
            final boolean isUpdateable = bundleInfoManager.isUpdateable(bundle);
            String bundleVersion = bundleInfoManager.getBundleVersion(bundle);
            String repositoryName=repositoryInfoManager.getBundlesRepositoryName(bundle);
            PluginListItemMetadata bundleMetadata = new PluginListItemMetadata(bundle,bundleVersion,
                    repositoryName,isInstalled,isUpdateable);
            bundleActionManager.installBundle(bundleMetadata,functionCallback); 
            logger.info("Installed App {} version {} from {}",symbolicName,bundleVersion,
                    repositoryName);
        }
    }

}
