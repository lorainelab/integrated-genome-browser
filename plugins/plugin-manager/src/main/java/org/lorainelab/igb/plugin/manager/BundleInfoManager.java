/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.lorainelab.igb.plugin.manager.repos.events.PluginRepositoryEventPublisher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = {BundleInfoManager.class})
public class BundleInfoManager {

    private static final Logger logger = LoggerFactory.getLogger(BundleInfoManager.class);
    private static final Predicate<? super Bundle> IS_PLUGIN = bundle -> bundle.getLocation().startsWith("obr:");

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
        final List<Bundle> runtimeBundles = Arrays.asList(bundleContext.getBundles());
        runtimeBundles.stream().filter(IS_PLUGIN.negate()).forEach(defaultBundles::add);
        refreshBundleInfo();
        initalizeBundleListener();
    }

    @Reference
    public void setEventBus(PluginRepositoryEventPublisher eventManager) {
        this.eventBus = eventManager.getPluginRepositoryEventBus();
        eventBus.register(this);
    }

    private void refreshBundleInfo() {
        repositoryManagedBundles = getFilteredRepositoryBundles();
        defaultBundles.stream().filter(IS_PLUGIN).forEach(repositoryManagedBundles::add);
    }

    private void initalizeBundleListener() {
        bundleListener = (BundleEvent bundleEvent) -> {
            Bundle bundle = bundleEvent.getBundle();
            switch (bundleEvent.getType()) {
                case BundleEvent.STARTED:
                    if (!defaultBundles.contains(bundle)) {
                        defaultBundles.add(bundle);
                    }
                    break;
                case BundleEvent.UNINSTALLED:
                    if (defaultBundles.contains(bundle)) {
                        defaultBundles.remove(bundle);
                        refreshBundleInfo();
                        if (!repositoryManagedBundles.stream()
                                .filter(plugin -> plugin.getSymbolicName().equals(bundle.getSymbolicName()))
                                .anyMatch(plugin -> plugin.getVersion().equals(bundle.getVersion()))) {
                            eventBus.post(new UpdateDataEvent());
                        }
                    }
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
        refreshBundleInfo();
        eventBus.post(new UpdateDataEvent());
    }

    private List<Bundle> getFilteredRepositoryBundles() {
        List<Bundle> repoBundles = new ArrayList<>();
        try {
            Resource[] allResourceArray = repoAdmin.discoverResources("(symbolicname=*)");
            for (Resource resource : allResourceArray) {
                Resolver resolver = repoAdmin.resolver();
                final ResourceWrapper bundle = new ResourceWrapper(resource);
                resolver.add(resource);
                if (resolver.resolve()) {
                    repoBundles.add(bundle);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Bundle from remote source is not compatible with this version of IGB: {}", bundle.getSymbolicName());
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

    public boolean isVersionOfBundleInstalled(Bundle bundle) {
        return Arrays.asList(bundleContext.getBundles()).stream()
                .anyMatch(installedBundle -> installedBundle.getSymbolicName().equals(bundle.getSymbolicName()));
    }

    public boolean isUpdateable(Bundle bundle) {
        if (Arrays.asList(bundleContext.getBundles()).stream()
                .anyMatch(installedBundle -> installedBundle.getSymbolicName().equals(bundle.getSymbolicName()))) {
            Bundle installedBundle = Arrays.asList(bundleContext.getBundles()).stream()
                    .filter(b -> b.getSymbolicName().equals(bundle.getSymbolicName())).findFirst().get();
            return repositoryManagedBundles.stream()
                    .filter(repoBundle -> repoBundle.getSymbolicName().equals(bundle.getSymbolicName()))
                    .anyMatch(repoBundle -> repoBundle.getVersion().compareTo(installedBundle.getVersion()) == 1);
        } else {
            return false;
        }
    }

    Bundle getLatestBundle(Bundle bundle) {
        if (isUpdateable(bundle)) {
            List<Bundle> updateableVersions = repositoryManagedBundles.stream()
                    .filter(b -> b.getSymbolicName().equals(bundle.getSymbolicName()))
                    .collect(Collectors.toList());
            Collections.sort(updateableVersions, (Bundle o1, Bundle o2) -> {
                return o1.getVersion().compareTo(o2.getVersion());
            });
            if (!updateableVersions.isEmpty()) {
                return updateableVersions.get(updateableVersions.size() - 1);
            }
        }
        return bundle;
    }

    String getBundleVersion(Bundle bundle) {
        Optional<Bundle> installedBundleMatch = Arrays.asList(bundleContext.getBundles()).stream().filter(installedBundle -> installedBundle.getSymbolicName().equals(bundle.getSymbolicName())).findFirst();
        if (installedBundleMatch.isPresent()) {
            return installedBundleMatch.get().getVersion().toString();
        }
        return bundle.getVersion().toString();
    }
}
