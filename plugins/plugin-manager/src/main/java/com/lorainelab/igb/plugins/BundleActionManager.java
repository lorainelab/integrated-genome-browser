/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import com.lorainelab.igb.services.IgbService;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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

    protected void updateBundle(PluginListItemMetadata plugin, final Function<Boolean, ? extends Class<Void>> callback) {
        CompletableFuture.supplyAsync(() -> {
            Bundle bundle = plugin.getBundle();
            Optional<Bundle> installedBundled = Arrays.asList(bundleContext.getBundles()).stream()
                    .filter(installedBundle -> installedBundle.getSymbolicName().equals(bundle.getSymbolicName())).findFirst();
            if (installedBundled.isPresent()) {
                try {
                    installedBundled.get().uninstall();
                } catch (BundleException ex) {
                    logger.error(ex.getMessage(), ex);
                }
                plugin.setVersion(bundle.getVersion().toString());
                plugin.setIsUpdatable(Boolean.FALSE);
                installBundle(plugin, f -> {
                    return Void.TYPE;
                });
            }
            return true;
        }).thenApply(callback);
    }

    public void installBundle(final PluginListItemMetadata plugin, final Function<Boolean, ? extends Class<Void>> callback) {
        CompletableFuture.supplyAsync(() -> {
            Bundle bundle = plugin.getBundle();
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
                logger.error(sb.toString());
            }
            return true;
        }).thenApply(callback);

    }

    public void uninstallBundle(final PluginListItemMetadata plugin, final Function<Boolean, ? extends Class<Void>> callback) {
        CompletableFuture.supplyAsync(() -> {
            Bundle bundle = plugin.getBundle();
            try {
                for (Bundle b : Arrays.asList(bundleContext.getBundles())) {
                    if (b.getSymbolicName().equals(bundle.getSymbolicName()) && b.getVersion().toString().equals(bundle.getVersion().toString())) {
                        b.uninstall();
                    }
                }

            } catch (BundleException bex) {
                String msg = BUNDLE.getString("bundleUninstallError");
                logger.error(msg);
            }
            return true;
        }).thenApply(callback);
    }
}
