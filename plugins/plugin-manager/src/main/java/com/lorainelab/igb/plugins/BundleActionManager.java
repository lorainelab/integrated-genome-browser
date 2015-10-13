/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import static com.lorainelab.igb.plugins.BundleInfoManager.isInstalled;
import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import com.lorainelab.igb.services.IgbService;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = BundleActionManager.class)
public class BundleActionManager {

    private static final Logger logger = LoggerFactory.getLogger(BundleActionManager.class);
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("plugins");
    private BundleInfoManager bundleInfoManager;
    private IgbService igbService;
    private RepositoryAdmin repoAdmin;
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Reference
    public void setBundleInfoManager(BundleInfoManager bundleInfoManager) {
        this.bundleInfoManager = bundleInfoManager;
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference(optional = false)
    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        repoAdmin = repositoryAdmin;
    }

    protected void updateBundle(PluginListItemMetadata plugin) {
        Bundle bundle = plugin.getBundle();
        if (isInstalled(bundle)) {
            Bundle latestBundle = bundleInfoManager.getLatestBundle(bundle);
            if (!bundle.equals(latestBundle) && !isInstalled(latestBundle)) {
                try {
                    bundle.uninstall();
                } catch (BundleException ex) {
                    logger.error(ex.getMessage(), ex);
                }
                plugin.setBundle(latestBundle);
                plugin.setVersion(latestBundle.getVersion().toString());
                installBundle(plugin);
            }
        }
    }

    public void installBundle(final PluginListItemMetadata plugin) {
        Bundle bundle = plugin.getBundle();
        CThreadWorker woker = new CThreadWorker("Installing " + bundle.getSymbolicName()) {
            @Override
            protected Object runInBackground() {
                Resource resource = ((ResourceWrapper) bundle).getResource();
                Resolver resolver = repoAdmin.resolver();
                resolver.add(resource);
                if (resolver.resolve()) {
                    resolver.deploy(Resolver.START);
                    igbService.setStatus(MessageFormat.format(BUNDLE.getString("bundleInstalled"), bundle.getSymbolicName(), bundle.getVersion()));
                } else {
                    String msg = MessageFormat.format(BUNDLE.getString("bundleInstallError"), bundle.getSymbolicName(), bundle.getVersion());
                    StringBuilder sb = new StringBuilder(msg);
                    sb.append(" -> ");
                    boolean started = false;
                    for (Reason reason : resolver.getUnsatisfiedRequirements()) {
                        if (started) {
                            sb.append(", ");
                        }
                        started = true;
                        sb.append(reason.getRequirement().getComment());
                    }
                    igbService.setStatus(msg);
                    logger.error(sb.toString());
                }
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(bundle, woker);
    }

    public void uninstallBundle(final PluginListItemMetadata plugin) {
        Bundle bundle = plugin.getBundle();
        CThreadWorker woker = new CThreadWorker("Uninstalling " + bundle.getSymbolicName()) {
            @Override
            protected Object runInBackground() {
                try {
                    for (Bundle b : Arrays.asList(bundleContext.getBundles())) {
                        if (b.getSymbolicName().equals(bundle.getSymbolicName()) && b.getVersion().toString().equals(bundle.getVersion().toString())) {
                            b.uninstall();
                        }
                    }
                    igbService.setStatus(MessageFormat.format(BUNDLE.getString("bundleUninstalled"), bundle.getSymbolicName(), bundle.getVersion()));
                } catch (BundleException bex) {
                    String msg = BUNDLE.getString("bundleUninstallError");
                    logger.error(msg);
                }
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(bundle, woker);
    }
}
