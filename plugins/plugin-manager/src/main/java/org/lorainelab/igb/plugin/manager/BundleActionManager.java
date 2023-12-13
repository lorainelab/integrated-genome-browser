/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import org.lorainelab.igb.services.IgbService;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.felix.bundlerepository.InterruptedResolutionException;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, service = BundleActionManager.class)
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        repoAdmin = repositoryAdmin;
    }

    public void updateBundle(PluginListItemMetadata plugin, final Function<Boolean, ? extends Class<Void>> callback) {
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
                installBundle(plugin, installSucceeded -> {
                    if (installSucceeded) {
                        plugin.setBundle(bundle);
                        plugin.setVersion(bundle.getVersion().toString());
                        plugin.setUpdateable(Boolean.FALSE);
                        callback.apply(installSucceeded);
                    }
                    return Void.TYPE;
                });
            }
            return true;
        });
    }

    /*
     * ~Kiran:IGBF-1108:Added this method as we cannot believe in InetAddress.isReachable method.
     */
    private static boolean isInternetReachable(URL url) {
        try {
            //Do this test only if it is http or https url skip for local url's
            if (url.toString().toLowerCase().startsWith("http")) {
                //open a connection to that source
                /*
                 * Author : Sameer Shanbhag
                 * IGBF-2164
                 * URL Input to this function might have the link to jar which will
                 * increase download count for that app (Refer to Issue for more
                 * information)
                 */
                // Get the Domain Name from the URL
                String urlHost = url.getAuthority();
                String urlProtocol = url.getProtocol();
                // Build URL to check the connection
                URL connectURL = new URL(urlProtocol + "://" + urlHost);
                HttpURLConnection urlConnect = (HttpURLConnection) connectURL.openConnection();
                //try connecting to the source, If there is no connection, this line will fail and throw exception
                Object objData = urlConnect.getContent();
            }
        } catch (UnknownHostException ex) {
            logger.error(ex.getMessage());
            return false;
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            return false;
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return false;
        }
        return true;
    }

    public void installBundle(final PluginListItemMetadata plugin, final Function<Boolean, ? extends Class<Void>> callback) {
        Bundle bundle = plugin.getBundle();
        Resource resource = ((ResourceWrapper) bundle).getResource();
        CompletableFuture.supplyAsync(new Supplier<Boolean>() {
            boolean tryToRecover = true;

            @Override
            public Boolean get() {
                try {
                    /*
                     * ~Kiran:IGBF-1108:Added to make sure an active internet connection exists
                     */
                    if (isInternetReachable(new URL(resource.getURI()))) {
                        installBundle(resource, bundle);
                    } else {
                        return false;
                    }
                } catch (IllegalStateException ex) {
                    if (tryToRecover && ex.getMessage().equals(KNOWN_FELIX_EXCEPTION)) {
                        tryToRecover = false; //only try this once
                        installBundle(plugin, callback);
                    }
                } catch (Throwable ex) {
                    logger.error(ex.getMessage(), ex);
                    return false;
                }
                return true;
            }
        }).thenApply(callback);

    }
    private final String KNOWN_FELIX_EXCEPTION = "Framework state has changed, must resolve again."; //See

    private synchronized void installBundle(Resource resource, Bundle bundle) throws InterruptedResolutionException {
        Resolver resolver = repoAdmin.resolver();
        resolver.add(resource);
        if (resolver.resolve()) {
            resolver.deploy(Resolver.START);
            logger.info("Installed app: " + bundle.getSymbolicName() + "," + bundle.getVersion());
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
    }

    public void uninstallBundle(final PluginListItemMetadata plugin, final Function<Boolean, ? extends Class<Void>> callback) {
        logger.info("Starting uninstallation process for plugin: " + plugin.getPluginName());

        CompletableFuture.supplyAsync(() -> {
            Bundle bundle = plugin.getBundle();
            String symbolicName = bundle.getSymbolicName();

            try {
                final List<Bundle> currentBundlesInRuntime = Arrays.asList(bundleContext.getBundles());
                boolean foundBundle = false;
                for (Bundle b : currentBundlesInRuntime) {
                    String bundleSymbolicName = b != null ? b.getSymbolicName() : null;

                    if (symbolicName != null && bundleSymbolicName != null) {
                        logger.info(bundleSymbolicName + " : " + symbolicName);
                        if (symbolicName.equals(bundleSymbolicName)) {
                            foundBundle = true;
                            logger.info("Found matching bundle for uninstallation: " + bundleSymbolicName + ", State: " + b.getState());

                            if (b.getState() == Bundle.ACTIVE) {
                                b.uninstall();
                                logger.info("Uninstalled app: " + bundleSymbolicName + ", Version: " + b.getVersion());
                            } else {
                                logger.warn("Bundle is not active and cannot be uninstalled: " + bundleSymbolicName + ", State: " + b.getState());
                            }
                        }
                    }
                }
                if (!foundBundle) {
                    logger.warn("No matching bundle found for uninstallation: " + symbolicName);
                }

            } catch (Exception bex) {
                String msg = BUNDLE.getString("bundleUninstallError");
                logger.error(msg + ", Bundle: " + symbolicName, bex);
                return false;
            }

            logger.info("Uninstallation process completed for plugin: " + plugin.getPluginName());
            return true;
        }).thenApply(callback);
    }

}
