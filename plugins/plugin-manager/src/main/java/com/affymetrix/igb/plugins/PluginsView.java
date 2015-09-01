package com.affymetrix.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import static com.affymetrix.common.CommonUtils.isDevelopmentMode;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.igb.plugins.BundleTableModel.NameInfoPanel;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.jide.JRPStyledTable;
import com.google.common.eventbus.EventBus;
import com.lorainelab.igb.plugins.repos.events.PluginRepositoryEventPublisher;
import com.lorainelab.igb.plugins.repos.events.ShowBundleRepositoryPanelEvent;
import com.lorainelab.igb.preferences.model.PluginRepository;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import com.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
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

/**
 * Tab Panel for managing plugins / bundles.
 */
@Component(name = PluginsView.COMPONENT_NAME, provide = {IgbTabPanelI.class, PluginsView.class}, immediate = true)
public class PluginsView extends IgbTabPanel implements IPluginsHandler, Constants {

    public static final String COMPONENT_NAME = "PluginsView";
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PluginsView.class);
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("plugins");
    private static final int TAB_POSITION = 7;
    private static final BundleFilter BOTH_BUNDLE_FILTER = bundle -> true;
    private static final BundleFilter NEITHER_BUNDLE_FILTER = bundle -> false;
    private BundleFilter DEFAULT_BUNDLE_FILTER;

    /**
     * determins if a given bundle is installed
     *
     * @param bundle the bundle to check
     * @return true if the bundle is installed, false otherwise
     */
    private static boolean isInstalled(Bundle bundle) {
        return bundle.getState() != Bundle.UNINSTALLED;
    }

    private BundleContext bundleContext;
    private JScrollPane jScrollPane;
    private BundleTableModel bundleTableModel;
    private JRPStyledTable bundleTable;
    private JRPButton updateAllBundlesButton;
    private JRPButton updateSelectedBundlesButton;
    private JRPButton repositoryPrefsButton;
    private RepositoryAdmin repoAdmin;
    private BundleListener bundleListener;
    private List<Bundle> installedBundles;
    private List<Bundle> repositoryBundles;
    private List<Bundle> unfilteredBundles; // all methods that access filteredBundles should be synchronized
    private List<Bundle> filteredBundles; // all methods that access filteredBundles should be synchronized
    private HashMap< String, Bundle> latest;
    private List<PluginRepository> externalRepositories;
    private List< Bundle> defaultBundles;
    private IgbService igbService;
    private EventBus eventBus;

    private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private final Cursor defaultCursor = null;

    public PluginsView() {
        super(BUNDLE.getString("viewTab"), BUNDLE.getString("viewTab"), BUNDLE.getString("pluginsTooltip"), false, TAB_POSITION);
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

    private void initializeDefaultBundleFilter() {
        defaultBundles.addAll(Arrays.asList(bundleContext.getBundles()));
        DEFAULT_BUNDLE_FILTER = (Bundle bundle) -> !defaultBundles.contains(bundle);
    }

    private void init() {
        latest = new HashMap<>();
        setLayout(new BorderLayout());
        BundleTableModel.setPluginsHandler(this); // is there a better way ?
        bundleTableModel = new BundleTableModel();
        bundleTable = new JRPStyledTable("PluginsView_bundleTable", bundleTableModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                super.valueChanged(e);
                updateSelectedBundlesButton.setEnabled(isUpdateSelectedBundlesExist());
            }
        };
        bundleTable.setCellSelectionEnabled(false);
        //		bundleTable.setAutoCreateRowSorter(true);
        //		bundleTable.getRowSorter().setSortKeys(BundleTableModel.SORT_KEYS);
        bundleTable.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        Bundle bundle = getNameInfoBundle(e.getPoint());
                        if (bundle != null) {
                            String bundleDocURL = bundle.getHeaders().get(Constants.BUNDLE_DOCURL);
                            if (bundleDocURL != null) {
                                GeneralUtils.browse(bundleDocURL);
                            }
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        Bundle bundle = getNameInfoBundle(e.getPoint());
                        if (bundle == null) {
                            setCursor(defaultCursor);
                        } else {
                            setCursor(handCursor);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        setCursor(defaultCursor);
                    }
                });

        bundleTable.addMouseMotionListener(
                new MouseAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        Bundle bundle = getNameInfoBundle(e.getPoint());
                        if (bundle == null) {
                            setCursor(defaultCursor);
                        } else {
                            setCursor(handCursor);
                        }
                    }
                });

        bundleTableModel.setJTable(bundleTable);

        jScrollPane = new JScrollPane(bundleTable);
        add("Center", jScrollPane);
        add("South", getButtonPanel());

        bundleListener = (BundleEvent bundleEvent) -> {
            if (bundleContext != null) {
                setInstalledBundles(Arrays.asList(bundleContext.getBundles()));
                reloadBundleTable();
            }
        };
        setInstalledBundles(Arrays.asList(bundleContext.getBundles()));
        setRepositoryBundles();
        bundleContext.addBundleListener(bundleListener);
    }

    /**
     * get the Bundle on the line where the cursor is
     *
     * @param p the point where the cursor is
     * @return the bundle on the line where the cursor is
     */
    private Bundle getNameInfoBundle(Point p) {
        int row = bundleTable.rowAtPoint(p);
        int column = bundleTable.columnAtPoint(p);
        if (column == bundleTableModel.getColumnIndex(BUNDLE_NAME)) {
            Bundle bundle = getBundleAtRow(row);
            Rectangle r = bundleTable.getCellRect(row, column, false);
            NameInfoPanel nameInfoPanel = NameInfoPanel.getPanel(bundle); // kludge
            if (nameInfoPanel.isOnInfoIcon(p.x - r.x, p.y - r.y)) {
                return bundle;
            }
        }
        return null;
    }

    /**
     * Create the button panel for the Plugin View
     *
     * @return the JPanel with all the buttons
     */
    private JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        updateAllBundlesButton = new JRPButton("PluginsView_updateAllBundlesButton", BUNDLE.getString("updateAllBundles"));
        updateAllBundlesButton.setToolTipText(BUNDLE.getString("updateAllBundlesTooltip"));
        updateAllBundlesButton.addActionListener(
                e -> {
                    updateAllBundles();
                    igbService.setStatus("");
                });
        updateAllBundlesButton.setEnabled(false);
        buttonPanel.add(updateAllBundlesButton);

        updateSelectedBundlesButton = new JRPButton("PluginsView_updateSelectedBundlesButton", BUNDLE.getString("updateSelectedBundles"));
        updateSelectedBundlesButton.setToolTipText(BUNDLE.getString("updateSelectedBundlesTooltip"));
        updateSelectedBundlesButton.addActionListener(
                e -> {
                    updateSelectedBundles();
                    igbService.setStatus("");
                });
        updateSelectedBundlesButton.setEnabled(false);
        buttonPanel.add(updateSelectedBundlesButton);

        buttonPanel.add(Box.createHorizontalGlue());

        repositoryPrefsButton = new JRPButton("PluginsView_repositoryPrefsButton", BUNDLE.getString("repositoryButton"));
        repositoryPrefsButton.addActionListener(
                evt -> {
                    eventBus.post(new ShowBundleRepositoryPanelEvent());
                });
        repositoryPrefsButton.setToolTipText(BUNDLE.getString("repositoryTooltip"));
        buttonPanel.add(repositoryPrefsButton);

        return buttonPanel;
    }

    @Override
    public ImageIcon getIcon(String name) {
        return MenuUtil.getIcon("16x16/actions/" + name);
    }

    @Override
    public Bundle getBundleAtRow(int row) {
        int modelRow = bundleTable.convertRowIndexToModel(row);
        return getFilteredBundle(modelRow);
    }

    private void installBundleIfNecessary(Bundle bundle) {
        if (isInstalled(bundle)) {
            Bundle latestBundle = latest.get(bundle.getSymbolicName());
            if (!bundle.equals(latestBundle) && !isInstalled(latestBundle)) {
                try {
                    bundle.uninstall();
                } catch (BundleException x) {
                    x.printStackTrace(System.err);
                }
                installBundle(latestBundle);
            }
        }
    }

    private synchronized void updateAllBundles() {
        if (filteredBundles != null) {
            filteredBundles.forEach(this::installBundleIfNecessary);
        }
    }

    private void updateSelectedBundles() {
        int[] rowIndices = bundleTable.getSelectedRows();
        for (int rowIndice : rowIndices) {
            Bundle bundle = getBundleAtRow(rowIndice);
            installBundleIfNecessary(bundle);
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

    private boolean isUpdateSelectedBundlesExist() {
        boolean updateSelectedBundlesExist = false;
        int[] rowIndices = bundleTable.getSelectedRows();
        for (int rowIndice : rowIndices) {
            Bundle bundle = getBundleAtRow(rowIndice);
            if (latest.get(bundle.getSymbolicName()) == null) {
                System.out.println(MessageFormat.format(BUNDLE.getString("internalError1"), bundle.getSymbolicName()));
                logger.warn(MessageFormat.format(BUNDLE.getString("internalError1"), bundle.getSymbolicName()));
            }
            if (isInstalled(bundle) && !bundle.equals(latest.get(bundle.getSymbolicName()))) {
                updateSelectedBundlesExist = true;
            }
        }
        return updateSelectedBundlesExist;
    }

    @Override
    public Version getLatestVersion(Bundle bundle) {
        return latest.get(bundle.getSymbolicName()).getVersion();
    }

    /**
     * determines whether the bundle is the latest version checks all the
     * bundles with the same Symbolic name for the highest version number
     *
     * @param bundle the bundle to check
     * @return true if the bundle is the latest version, false otherwise
     */
    private boolean isLatest(Bundle bundle) {
        return bundle.getVersion().equals(getLatestVersion(bundle));
    }

    @Override
    public boolean isUpdatable(Bundle bundle) {
        return bundle != null && isInstalled(bundle) && !isLatest(bundle);
    }

    @Override
    public synchronized Bundle getFilteredBundle(int index) {
        if (index < 0 || index >= filteredBundles.size()) {
            logger.warn(MessageFormat.format(BUNDLE.getString("internalError2"), index, filteredBundles.size()));
            return null;
        }
        return filteredBundles.get(index);
    }

    @Override
    public synchronized int getFilteredBundleCount() {
        if (filteredBundles == null) {
            return 0;
        }
        return filteredBundles.size();
    }

//    private boolean checkRequirements(Requirement[] requirements) {
//        boolean checked = false;
//        for (Requirement requirement : requirements) {
//            checked |= requirement.isSatisfied(osgiImpl.getCapability());
//        }
//        return checked;
//    }
    /**
     * gets the full set of all bundles in all the bundle repositories in the
     * Preferences tab, filter by IGB version
     */
    private void setRepositoryBundles() {
        try {
            Resource[] allResourceArray = repoAdmin.discoverResources("(symbolicname=*)");
            List< Bundle> repositoryBundles = new ArrayList<>();
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
    private void reloadBundleTable() {
        filterBundles();
        bundleTableModel.fireTableDataChanged();
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

    @Override
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
                    String msg = MessageFormat.format(PluginsView.BUNDLE.getString("bundleInstallError"), bundle.getSymbolicName(), bundle.getVersion());
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

    @Override
    public void uninstallBundle(final Bundle bundle) {
        CThreadWorker woker = new CThreadWorker("Uninstalling " + bundle.getSymbolicName()) {
            @Override
            protected Object runInBackground() {
                try {
                    bundle.uninstall();
                    igbService.setStatus(MessageFormat.format(BUNDLE.getString("bundleUninstalled"), bundle.getSymbolicName(), bundle.getVersion()));
                } catch (BundleException bex) {
                    String msg = PluginsView.BUNDLE.getString("bundleUninstallError");
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
        updateAllBundlesButton.setEnabled(isUpdateBundlesExist());
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
                    reloadBundleTable();
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
        reloadBundleTable();
    }

    public void updateBundleTable() {
        setRepositoryBundles();
        reloadBundleTable();
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }

    @Override
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
