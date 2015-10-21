/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.lorainelab.igb.plugins.repos.events.PluginRepositoryEventPublisher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = {BundleInfoManager.class})
public class BundleInfoManager {

    private static final Logger logger = LoggerFactory.getLogger(BundleInfoManager.class);
    private static final Predicate<? super Bundle> IS_PLUGIN = bundle -> !bundle.getLocation().startsWith("obr:");

    private RepositoryAdmin repoAdmin;
    private BundleListener bundleListener;
    private final List<Bundle> defaultBundles;
    private List<Bundle> repositoryManagedBundles;
    private BundleContext bundleContext;
    private EventBus eventBus;

    public BundleInfoManager() {
        defaultBundles = Lists.newArrayList();
        repositoryManagedBundles = Lists.newArrayList();
    }

    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
        initializeDefaultBundleFilter();
        initalizeBundleListener();
    }

    @Reference
    public void setEventBus(PluginRepositoryEventPublisher eventManager) {
        this.eventBus = eventManager.getPluginRepositoryEventBus();
        eventBus.register(this);
    }

    private void initializeDefaultBundleFilter() {
        final List<Bundle> runtimeBundles = Arrays.asList(bundleContext.getBundles());
        runtimeBundles.stream().filter(IS_PLUGIN).forEach(defaultBundles::add);
        repositoryManagedBundles = getFilteredRepositoryBundles();
        runtimeBundles.stream().filter(IS_PLUGIN.negate()).forEach(repositoryManagedBundles::add);
    }

    private void initalizeBundleListener() {
        bundleListener = (BundleEvent bundleEvent) -> {
            Bundle bundle = bundleEvent.getBundle();
            switch (bundleEvent.getType()) {
                case BundleEvent.STARTED:
                    //refresh
                    break;
                case BundleEvent.UNINSTALLED:
                    //refresh
                    break;
            }
        };
        bundleContext.addBundleListener(bundleListener);
    }

    @Reference(optional = false)
    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        repoAdmin = repositoryAdmin;
    }

    void reloadRepositoryBundles() {
        initializeDefaultBundleFilter();
        eventBus.post(new UpdateDataEvent());
    }

    private List<Bundle> getFilteredRepositoryBundles() {
        List<Bundle> repoBundles = new ArrayList<>();
        try {
            Resource[] allResourceArray = repoAdmin.discoverResources("(symbolicname=*)");
            Resolver resolver = repoAdmin.resolver();
            for (Resource resource : allResourceArray) {
                final ResourceWrapper bundle = new ResourceWrapper(resource);
                resolver.add(resource);
                if (resolver.resolve()) {
                    repoBundles.add(bundle);
                } else {
                    logger.info("Bundle from remote source is not compatible with this version of IGB: {}", bundle.getSymbolicName());
                    if (logger.isDebugEnabled()) {
                        for (Reason reason : resolver.getUnsatisfiedRequirements()) {
                            logger.debug(reason.getRequirement().getComment());
                        }
                    }
                }
            }

        } catch (InvalidSyntaxException ex) {
            logger.error(ex.getMessage(), ex);
        }
        Collections.sort(repoBundles, (Bundle o1, Bundle o2) -> {
            int result = o1.getSymbolicName().compareTo(o2.getSymbolicName());
            if (result == 0) {
                result = o1.getVersion().compareTo(o2.getVersion());
            }
            return result;
        });
        return repoBundles;
    }

    public List<Bundle> getRepositoryManagedBundles() {
        return repositoryManagedBundles;
    }

    public static boolean isInstalled(Bundle bundle) {
        return bundle.getState() != Bundle.UNINSTALLED;
    }

    private boolean isLatest(Bundle bundle) {
        return bundle.getVersion().equals(getLatestVersion(bundle));
    }

    public boolean isUpdateable(Bundle bundle) {
        return repositoryManagedBundles.stream()
                .filter(b -> b.getSymbolicName().equals(bundle.getSymbolicName()))
                .anyMatch(b -> bundle.getVersion().compareTo(b.getVersion()) == 1);
    }

    private Version getLatestVersion(Bundle bundle) {
        if (isUpdateable(bundle)) {
            List<Version> updateableVersions = repositoryManagedBundles.stream()
                    .filter(b -> b.getSymbolicName().equals(bundle.getSymbolicName()))
                    .filter(b -> bundle.getVersion().compareTo(b.getVersion()) == 1)
                    .map(b -> b.getVersion()).collect(Collectors.toList());
            Collections.sort(updateableVersions);
            if (!updateableVersions.isEmpty()) {
                return updateableVersions.get(0);
            }
        }
        return bundle.getVersion();
    }

    Bundle getLatestBundle(Bundle bundle) {
        if (isUpdateable(bundle)) {
            List<Bundle> updateableVersions = repositoryManagedBundles.stream()
                    .filter(b -> b.getSymbolicName().equals(bundle.getSymbolicName()))
                    .filter(b -> bundle.getVersion().compareTo(b.getVersion()) == 1)
                    .collect(Collectors.toList());
            Collections.sort(updateableVersions, (Bundle o1, Bundle o2) -> {
                return o1.getVersion().compareTo(o2.getVersion());
            });
            if (!updateableVersions.isEmpty()) {
                return updateableVersions.get(0);
            }
        }
        return bundle;
    }
}
