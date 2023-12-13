/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.lorainelab.igb.plugin.manager;

/**
 *
 * @author dcnorris
 */
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;
import org.osgi.service.component.annotations.Activate;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import org.lorainelab.igb.plugin.manager.repos.events.PluginRepositoryEventPublisher;
import org.lorainelab.igb.plugin.manager.repos.events.ShowBundleRepositoryPanelEvent;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import org.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import static org.lorainelab.igb.services.ServiceComponentNameReference.APP_MANAGER_TAB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = APP_MANAGER_TAB, immediate = true, service = {IgbTabPanelI.class, AppManager.class})
public class AppManager extends IgbTabPanel {

    private static final Logger LOG = LoggerFactory.getLogger(AppManager.class);
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("plugins");
    private static final int TAB_POSITION = 8;
    private HtmlPane htmlPane;
    private JList<PluginListItemMetadata> listView;
    private DefaultListModel<PluginListItemMetadata> listModel;
    private List<PluginListItemMetadata> masterList;
    private Predicate<PluginListItemMetadata> currentStaticPredicate;
    private Predicate<PluginListItemMetadata> currentSearchPredicate;
    private JTextField searchField;
    private JComboBox<FilterOption> filterOptions;
    private JButton updateAllBtn, manageReposBtn, installBtn, uninstallBtn;
    private Predicate<PluginListItemMetadata> allAppsFilter = s -> true;
    private Predicate<PluginListItemMetadata> installedFilter = PluginListItemMetadata::isInstalled;
    private Predicate<PluginListItemMetadata> uninstalledFilter = installedFilter.negate();

    private BundleContext bundleContext;
    private BundleInfoManager bundleInfoManager;
    private BundleActionManager bundleActionManager;
    private RepositoryInfoManager repositoryInfoManager;
    private IgbService igbService;
    private EventBus eventBus;

    public AppManager() {
        super(BUNDLE.getString("viewTab"), BUNDLE.getString("viewTab"), BUNDLE.getString("pluginsTooltip"), false, TAB_POSITION);
        currentStaticPredicate = (PluginListItemMetadata s) -> true;
        currentSearchPredicate = (PluginListItemMetadata s) -> true;
        masterList = new ArrayList<>();
        MigLayout migLayout = new MigLayout("fill");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(migLayout);
        setSize(new Dimension(1000, 618));
        initLayout();
        setUpListeners();
    }

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        updateDataEventNotification(new UpdateDataEvent());
    }

    private void updateListModel(Predicate<PluginListItemMetadata> filter) {
        listModel.clear();
        masterList.stream().filter(filter).forEach(listModel::addElement);
    }

    public static void main(String[] args) {
        AppManager am = new AppManager();
        SwingUtilities.invokeLater(() -> am.setVisible(true));
    }

    private void initLayout() {
        setLayout(new MigLayout("fill, insets 0", "[grow]", "[][grow]"));

        // Top bar components
        JPanel topBar = new JPanel(new MigLayout("insets 10", "[][grow][]", "[]"));
        searchField = new JTextField();
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSearchIcon());
        filterOptions = new JComboBox<>();
        for (FilterOption option : FilterOption.values()) {
            filterOptions.addItem(option);
        }
        installBtn = new JButton("Install Plugin");
        uninstallBtn = new JButton("Uninstall Plugin");
        updateAllBtn = new JButton("Update All");
        manageReposBtn = new JButton("Manage Repositories...");
        topBar.add(searchField, "width 200:200:200, height 26:26:26");
        topBar.add(filterOptions, "width 150:150:150");
        topBar.add(installBtn);
        topBar.add(uninstallBtn);
        topBar.add(updateAllBtn);
        topBar.add(manageReposBtn);

        // SplitPane setup
        JSplitPane splitPane = new JSplitPane();
        listModel = new DefaultListModel<PluginListItemMetadata>();
        listView = new JList<PluginListItemMetadata>(listModel);
        listView.setCellRenderer(new PluginListCellRenderer());
        JScrollPane listViewPane = new JScrollPane(listView);
        htmlPane = new HtmlPane();
        //button6.putClientProperty("JButton.buttonType", "roundRect");
        JScrollPane descriptionScrollPane = new JScrollPane(htmlPane);
        splitPane.setLeftComponent(listViewPane);
        splitPane.setRightComponent(descriptionScrollPane);

        // Adding components to the frame
        add(topBar, "wrap, growx");
        add(splitPane, "grow");
        pack();
    }

    private void setUpListeners() {
        // Example listener for list selection
        listView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    PluginListItemMetadata selectedPlugin = listView.getSelectedValue();
                    // Handle plugin selection change
                    updateDescriptionPane(selectedPlugin);
                    updateButtonStates();
                }
            }
        });

        updateAllBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAllBundles();
            }
        });

        manageReposBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> {
                    eventBus.post(new ShowBundleRepositoryPanelEvent());
                });
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changeSearchFilter(searchField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                changeSearchFilter(searchField.getText());
            }

            public void insertUpdate(DocumentEvent e) {
                changeSearchFilter(searchField.getText());
            }
        });

        filterOptions.addActionListener(e -> {
            FilterOption selectedFilter = (FilterOption) filterOptions.getSelectedItem();
            changeStaticFilter(selectedFilter);
        });
        updateAllBtn.addActionListener((ActionEvent e) -> {
            updateAllBundles();
        });
        installBtn.addActionListener(e -> {
            PluginListItemMetadata selectedPlugin = listView.getSelectedValue();
            if (selectedPlugin != null) {
                installPlugin(selectedPlugin);
            }
        });

        uninstallBtn.addActionListener(e -> {
            PluginListItemMetadata selectedPlugin = listView.getSelectedValue();
            if (selectedPlugin != null) {
                uninstallPlugin(selectedPlugin);
            }
        });
    }

    private void updateButtonStates() {
        PluginListItemMetadata selectedPlugin = listView.getSelectedValue();
        if (selectedPlugin != null) {
            installBtn.setEnabled(!selectedPlugin.isInstalled());
            uninstallBtn.setEnabled(selectedPlugin.isInstalled());
        }
    }

    private void updateAllBundles() {
        masterList.stream().filter(plugin -> plugin.isUpdateable()).forEach(plugin -> {
            updatePlugin(plugin);
        });
    }

    private void updatePlugin(final PluginListItemMetadata plugin) {
        final Function<Boolean, ? extends Class<Void>> handleUpdateCallback = (Boolean b) -> {
            if (b) {
                SwingUtilities.invokeLater(() -> {
                    plugin.setBusy(Boolean.FALSE);
                    refreshUpdateAllBtn();
                });
            }
            return Void.TYPE;
        };
        SwingUtilities.invokeLater(() -> {
            plugin.setBusy(Boolean.TRUE);
        });
        bundleActionManager.updateBundle(plugin, handleUpdateCallback);
    }

    private void refreshUpdateAllBtn() {
        SwingUtilities.invokeLater(() -> {
            updateDataEventNotification(new UpdateDataEvent());
            boolean isAnyUpdatable = masterList.stream()
                    .anyMatch(PluginListItemMetadata::isUpdateable);
            updateAllBtn.setEnabled(isAnyUpdatable);
        });
    }

    @Subscribe
    public void updateDataEventNotification(UpdateDataEvent event) {
        SwingUtilities.invokeLater(() -> {
            // Store the selected item
            PluginListItemMetadata selectedItem = listView.getSelectedValue();

            listModel.clear();
            masterList.clear();
            List<Bundle> toAdd = new ArrayList<>();
            for (Bundle bundle : bundleInfoManager.getRepositoryManagedBundles()) {
                Optional<Bundle> match = toAdd.stream()
                        .filter(b -> b.getSymbolicName().equals(bundle.getSymbolicName()))
                        .findFirst();
                if (match.isPresent()) {
                    if (bundle.getVersion().compareTo(match.get().getVersion()) >= 1) {
                        toAdd.remove(match.get());
                        toAdd.add(bundle);
                    }
                } else {
                    toAdd.add(bundle);
                }
            }

            toAdd.forEach(bundle -> {
                final boolean isInstalled = bundleInfoManager.isVersionOfBundleInstalled(bundle);
                final boolean isUpdatable = bundleInfoManager.isUpdateable(bundle);
                masterList.add(new PluginListItemMetadata(
                        bundle, bundleInfoManager.getBundleVersion(bundle),
                        repositoryInfoManager.getBundlesRepositoryName(bundle), isInstalled, isUpdatable));
            });
            masterList.stream().forEach(listModel::addElement);

            // Restore the selection
            if (selectedItem != null) {
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.getElementAt(i).equals(selectedItem)) {
                        listView.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
    }

    private void updateDescriptionPane(PluginListItemMetadata selectedPlugin) {
        // Update htmlPane content based on selected plugin
        htmlPane.load(selectedPlugin);
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void setEventBus(PluginRepositoryEventPublisher eventManager) {
        this.eventBus = eventManager.getPluginRepositoryEventBus();
        eventBus.register(this);
    }

    public List<PluginListItemMetadata> getPluginItems() {
        return masterList;
    }

    private void changeSearchFilter(String filter) {
        currentSearchPredicate = s -> {
            String escapedFilter = Pattern.quote(filter);
            Pattern nameStartWith = Pattern.compile("^" + escapedFilter, Pattern.CASE_INSENSITIVE);
            Pattern nameContains = Pattern.compile(escapedFilter, Pattern.CASE_INSENSITIVE);
            Pattern descContains = Pattern.compile(escapedFilter, Pattern.CASE_INSENSITIVE);
            return nameStartWith.matcher(s.getPluginName()).find()
                    || nameContains.matcher(s.getPluginName()).find()
                    || descContains.matcher(s.getDescription()).find();
        };
        updateListModel(currentSearchPredicate.and(currentStaticPredicate));
    }

    void changeStaticFilter(FilterOption filter) {
        currentStaticPredicate = switch (filter) {
            case ALL_APPS ->
                allAppsFilter;
            case INSTALLED ->
                installedFilter;
            case UNINSTALLED ->
                uninstalledFilter;
            default ->
                allAppsFilter;
        };
        updateListModel(currentStaticPredicate.and(currentSearchPredicate));
    }

    public enum FilterOption {

        ALL_APPS("All Apps"), INSTALLED("Installed"), UNINSTALLED("Uninstalled");

        private final String label;

        private FilterOption(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private void installPlugin(PluginListItemMetadata plugin) {
        if (plugin == null) {
            SwingUtilities.invokeLater(() -> {
//                updateWebContent();
            });
            return;
        }
        final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
            if (t) {
                SwingUtilities.invokeLater(() -> {
                    plugin.setInstalled(Boolean.TRUE);
                    plugin.setBusy(Boolean.FALSE);
                    refreshUpdateAllBtn();
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    plugin.setBusy(Boolean.FALSE);
                    refreshUpdateAllBtn();
                });
            }
            return Void.TYPE;
        };
        plugin.setBusy(Boolean.TRUE);
        bundleActionManager.installBundle(plugin, functionCallback);
    }

    private void uninstallPlugin(PluginListItemMetadata plugin) {
        LOG.info("UninstallPlugin method called for: " + plugin.getPluginName());
        if (plugin == null) {
            LOG.error("Plugin is null in uninstallPlugin method");
            SwingUtilities.invokeLater(() -> {
                refreshUpdateAllBtn();
            });
            return;
        }
        final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
            if (t) {
                LOG.info("Uninstallation successful for plugin: " + plugin.getPluginName());
                SwingUtilities.invokeLater(() -> {
                    plugin.setBusy(Boolean.FALSE);
                    plugin.setInstalled(Boolean.FALSE);
                    plugin.setUpdateable(Boolean.FALSE);
                    refreshUpdateAllBtn();
                });
            } else {
                LOG.warn("Uninstallation failed for plugin: " + plugin.getPluginName());
            }
            return Void.TYPE;
        };
        SwingUtilities.invokeLater(() -> {
            plugin.setBusy(Boolean.TRUE);
        });
        bundleActionManager.uninstallBundle(plugin, functionCallback);
    }

}
