package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import static com.affymetrix.common.CommonUtils.isDevelopmentMode;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.ErrorHandler;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import com.lorainelab.igb.plugins.repos.events.PluginRepositoryEventPublisher;
import com.lorainelab.igb.preferences.model.PluginRepository;
import com.lorainelab.igb.services.IgbService;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import static org.apache.felix.bundlerepository.Resource.SYMBOLIC_NAME;
import static org.apache.felix.bundlerepository.Resource.VERSION;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, provide = {AppController.class})
public class AppController implements Constants {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("plugins");
    private BundleFilter DEFAULT_BUNDLE_FILTER;

    private BundleContext bundleContext;
    private RepositoryAdmin repoAdmin;
    private BundleListener bundleListener;
    private List<Bundle> installedBundles;
    private List<Bundle> repositoryBundles;
    private List<Bundle> unfilteredBundles; // all methods that access filteredBundles should be synchronized
    private List<Bundle> filteredBundles; // all methods that access filteredBundles should be synchronized
    private HashMap< String, Bundle> latest;
    private final List<PluginRepository> externalRepositories;
    private final List<Bundle> defaultBundles;
    private IgbService igbService;
    private EventBus eventBus;
    private PluginManagerFxPanel fxPanel;

    public AppController() {
        latest = new HashMap<>();
        defaultBundles = new ArrayList<>();
        externalRepositories = new ArrayList<>();
    }

    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
        initializeDefaultBundleFilter();
        init();
    }

    @Reference
    public void setEventBus(PluginRepositoryEventPublisher eventManager) {
        this.eventBus = eventManager.getPluginRepositoryEventBus();
        eventBus.register(this);
    }

    @Reference
    public void setFxPanel(PluginManagerFxPanel fxPanel) {
        this.fxPanel = fxPanel;
    }

    private void initializeDefaultBundleFilter() {
        defaultBundles.addAll(Arrays.asList(bundleContext.getBundles()));
        DEFAULT_BUNDLE_FILTER = (Bundle bundle) -> !defaultBundles.contains(bundle);
    }

    private void init() {
        latest = new HashMap<>();
        bundleListener = (BundleEvent bundleEvent) -> {
            if (bundleContext != null) {
                setInstalledBundles(Arrays.asList(bundleContext.getBundles()));
                reloadBundleList();
            }
        };
        setInstalledBundles(Arrays.asList(bundleContext.getBundles()));
        setRepositoryBundles();
        bundleContext.addBundleListener(bundleListener);
    }

    private void installBundleIfNecessary(Bundle bundle) {
        if (isInstalled(bundle)) {
            Bundle latestBundle = latest.get(bundle.getSymbolicName());
            if (!bundle.equals(latestBundle) && !isInstalled(latestBundle)) {
                try {
                    bundle.uninstall();
                } catch (BundleException ex) {
                    logger.error(ex.getMessage(), ex);
                }
                installBundle(latestBundle);
            }
        }
    }

    private static boolean isInstalled(Bundle bundle) {
        return bundle.getState() != Bundle.UNINSTALLED;
    }

    private synchronized void updateAllBundles() {
        if (filteredBundles != null) {
            filteredBundles.forEach(this::installBundleIfNecessary);
        }
    }

    private synchronized boolean isUpdateBundlesExist() {
        boolean updateBundlesExist = false;
        if (filteredBundles != null) {
            for (Bundle bundle : filteredBundles) {
                if (isInstalled(bundle) && !bundle.equals(latest.get(bundle.getSymbolicName()))) {
                    updateBundlesExist = true;
                }
            }
        }
        return updateBundlesExist;
    }

    public Version getLatestVersion(Bundle bundle) {
        return latest.get(bundle.getSymbolicName()).getVersion();
    }

    /**
     * determines whether the bundle is the latest version checks all the bundles with the same Symbolic name for the
     * highest version number
     *
     * @param bundle the bundle to check
     * @return true if the bundle is the latest version, false otherwise
     */
    private boolean isLatest(Bundle bundle) {
        return bundle.getVersion().equals(getLatestVersion(bundle));
    }

    public boolean isUpdatable(Bundle bundle) {
        return bundle != null && isInstalled(bundle) && !isLatest(bundle);
    }

    public synchronized int getFilteredBundleCount() {
        if (filteredBundles == null) {
            return 0;
        }
        return filteredBundles.size();
    }

    /**
     * gets the full set of all bundles in all the bundle repositories in the Preferences tab, filter by IGB version
     */
    private void setRepositoryBundles() {
        try {
            Resource[] allResourceArray = repoAdmin.discoverResources("(symbolicname=*)");
            List<Bundle> repositoryBundles = new ArrayList<>();
            for (Resource resource : allResourceArray) {
                repositoryBundles.add(new ResourceWrapper(resource));
            }
            setRepositoryBundles(repositoryBundles);
        } catch (InvalidSyntaxException ex) {
            logger.error("Error setting adding bundles from obr repository to PluginsView", ex);
        }
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference(optional = false)
    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        repoAdmin = repositoryAdmin;
    }

    /**
     * reload the table data due to any changes
     */
    private void reloadBundleList() {
        filterBundles();
        List<PluginListItemMetadata> pluginMetaData = Lists.newArrayList();
        for (Bundle bundle : filteredBundles) {
            String description = bundle.getSymbolicName();
            try {
                description = bundle.getHeaders().get("Bundle-Description");
                byte[] decode = Base64.getDecoder().decode(description);
                description = new String(decode);
            } catch (Exception ex) {

            }
            PluginListItemMetadata itemMetadata = new PluginListItemMetadata(bundle.getSymbolicName(), getRepository(bundle), bundle.getVersion().toString(), description, isUpdatable(bundle), isInstalled(bundle));
            pluginMetaData.add(itemMetadata);
        }
        fxPanel.updateListContent(pluginMetaData);
    }

    /**
     * called before the page is closed
     */
    public void deactivate() {
        bundleContext.removeBundleListener(bundleListener);
        bundleContext = null;
    }

    private void displayError(String error) {
        igbService.setStatus(error);
        logger.error(error);
    }

    public void installBundle(final Bundle bundle) {
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
                    String msg = MessageFormat.format(AppController.BUNDLE.getString("bundleInstallError"), bundle.getSymbolicName(), bundle.getVersion());
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

    public void uninstallBundle(final Bundle bundle) {
        CThreadWorker woker = new CThreadWorker("Uninstalling " + bundle.getSymbolicName()) {
            @Override
            protected Object runInBackground() {
                try {
                    bundle.uninstall();
                    igbService.setStatus(MessageFormat.format(BUNDLE.getString("bundleUninstalled"), bundle.getSymbolicName(), bundle.getVersion()));
                } catch (BundleException bex) {
                    String msg = AppController.BUNDLE.getString("bundleUninstallError");
                    displayError(msg);
                }
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(bundle, woker);
    }

    /**
     * saves the currently installed bundles
     *
     * @param installedBundles the currently installed bundles
     */
    private void setInstalledBundles(List< Bundle> installedBundles) {
        this.installedBundles = installedBundles;
        setUnfilteredBundles();
    }

    /**
     * saves the current set of bundles in all repositories
     *
     * @param repositoryBundles the bundles in all repositories
     */
    private void setRepositoryBundles(List< Bundle> repositoryBundles) {
        this.repositoryBundles = repositoryBundles;
        setUnfilteredBundles();
    }

    /**
     * add a new bundle (installed or repository)
     *
     * @param bundle the bundle to add
     */
    private synchronized void addBundle(Bundle bundle) {
        String symbolicName = bundle.getSymbolicName();
        if (StringUtils.isBlank(symbolicName)) {
            return;
        }
        Version version = bundle.getVersion();
        for (Bundle unfilteredBundle : unfilteredBundles) {
            if (symbolicName.equals(unfilteredBundle.getSymbolicName()) && version.equals(unfilteredBundle.getVersion())) {
                return;
            }
        }
        unfilteredBundles.add(bundle);
        if (latest.get(symbolicName) == null || version.compareTo(latest.get(symbolicName).getVersion()) > 0) {
            latest.put(symbolicName, bundle);
        }
    }

    /**
     * update the set of all bundles (unfiltered) due to a change
     */
    private synchronized void setUnfilteredBundles() {
        unfilteredBundles = new ArrayList<>();
        latest.clear();
        if (installedBundles != null) {
            installedBundles.forEach(this::addBundle);
        }
        if (repositoryBundles != null) {
            repositoryBundles.forEach(this::addBundle);
        }
    }

    /**
     * filter the unfiltered bundles using the current bundle filter
     */
    private synchronized void filterBundles() {
        filteredBundles = new ArrayList<>();
        for (Bundle bundle : unfilteredBundles) {
            if (bundle.getLocation().startsWith("obr:") || (DEFAULT_BUNDLE_FILTER.filterBundle(bundle))) {
                try {
                    Resource[] resources = repoAdmin.discoverResources("(&(" + SYMBOLIC_NAME + "=" + bundle.getSymbolicName() + ")(" + VERSION + "=" + bundle.getVersion() + "))");
                    Resolver resolver = repoAdmin.resolver();
                    if (resources.length > 0) {
                        resolver.add(resources[0]);
                        if (resolver.resolve()) {
                            filteredBundles.add(bundle);
                        } else {
                            logger.info("Bundle from remote source is not compatible with this version of IGB: {}", bundle.getSymbolicName());
                            if (logger.isDebugEnabled()) {
                                for (Reason reason : resolver.getUnsatisfiedRequirements()) {
                                    logger.debug(reason.getRequirement().getComment());
                                }
                            }
                        }
                    } else {
                        //add anyway
                        filteredBundles.add(bundle);
                    }
                } catch (InvalidSyntaxException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
        Collections.sort(
                filteredBundles,
                new Comparator< Bundle>() {
            @Override
            public int compare(Bundle o1, Bundle o2) {
                int result = o1.getSymbolicName().compareTo(o2.getSymbolicName());
                if (result == 0) {
                    result = o1.getVersion().compareTo(o2.getVersion());
                }
                return result;
            }
        });
//        updateAllBundlesButton.setEnabled(isUpdateBundlesExist());
    }

    public boolean addPluginRepository(PluginRepository pluginRepository) {
        String url = pluginRepository.getUrl();
        if (defaultBundles.isEmpty()) {
            initializeDefaultBundleFilter();
        }
        CThreadWorker< Void, Void> worker = new CThreadWorker< Void, Void>("repositoryAdded") {
            @Override
            protected Void runInBackground() {
                try {
                    repoAdmin.addRepository(new URL(url + REPOSITORY_XML_FILE_PATH));
                    externalRepositories.add(pluginRepository);
                    setRepositoryBundles();
                    reloadBundleList();
                } catch (ConnectException x) {
                    displayError(MessageFormat.format(BUNDLE.getString("repositoryFailError"), url));
                } catch (MalformedURLException x) {
                    displayError(MessageFormat.format(BUNDLE.getString("invalidRepositoryError"), url));
                } catch (Exception x) {
                    ErrorHandler.errorPanelWithReportBug(BUNDLE.getString("repository"),
                            MessageFormat.format(BUNDLE.getString("invalidRepositoryError"), url),
                            Level.SEVERE);
                }
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(this, worker);
        return true;
    }
    private static final String REPOSITORY_XML_FILE_PATH = "/repository.xml";

    public void removePluginRepository(PluginRepository pluginRepository) {
        String url = pluginRepository.getUrl();
        try {
            repoAdmin.removeRepository(new URL(url + REPOSITORY_XML_FILE_PATH).toURI().toString());
            externalRepositories.remove(pluginRepository);
        } catch (MalformedURLException | URISyntaxException ex) {
            logger.error("Error removing repository.", ex);
        }
        setRepositoryBundles();
        reloadBundleList();
    }

    public void updateBundleTable() {
        setRepositoryBundles();
        reloadBundleList();
    }

    public boolean isEmbedded() {
        return true;
    }

    public String getRepository(Bundle bundle) {
        String repository = "";
        if (bundle != null) {
            String location = bundle.getLocation();
            if (location != null) {
                for (PluginRepository repo : externalRepositories) {
                    if (location.startsWith(repo.getUrl())) {
                        repository = repo.getName();
                    }
                }

                // The 'Repository' column in 'Plug-ins' tab will be set to empty after plug-in installed
                // The following code is trying to find the installed bundles in repository by comparing the symbolic name and version,
                // use the name from repository for 'Repository' column if a matched record found
                if (repository.isEmpty()) {
                    for (Bundle b : repositoryBundles) {
                        if (b != null && b.getLocation() != null && bundle.getSymbolicName().equals(b.getSymbolicName()) && bundle.getVersion().equals(b.getVersion())) {
                            for (PluginRepository repo : externalRepositories) {
                                String repoUrl = repo.getUrl();
                                if (b.getLocation().startsWith(repoUrl)) {
                                    repository = repo.getName();
                                }
                            }
                        }
                    }
                }
                if (repository.isEmpty() && isDevelopmentMode()) {
                    repository = "Development Mode";
                }
            }
        }
        return repository;
    }

}
