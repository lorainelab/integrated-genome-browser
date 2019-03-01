package org.lorainelab.igb.appstore;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.lorainelab.igb.plugin.manager.BundleActionManager;
import org.lorainelab.igb.plugin.manager.BundleInfoManager;
import org.lorainelab.igb.plugin.manager.RepositoryInfoManager;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;

/**
 *
 * @author kkorey
 */
@Component(name ="WebAppManager",immediate = true, provide = {WebAppManager.class})
public class WebAppManager {

    private static final Logger logger = LoggerFactory.getLogger(WebAppManager.class);

    private BundleContext bundleContext;
    private BundleInfoManager bundleInfoManager;
    private BundleActionManager bundleActionManager;
    private RepositoryInfoManager repositoryInfoManager;

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;

    }

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
            bundleActionManager.installBundle(new PluginListItemMetadata(bundle, bundleInfoManager.getBundleVersion(bundle), repositoryInfoManager.getBundlesRepositoryName(bundle), isInstalled, isUpdateable),functionCallback);
        }
    }

}
