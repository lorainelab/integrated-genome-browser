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
import com.google.common.eventbus.Subscribe;
import static com.lorainelab.igb.plugins.Constants.MATERIAL_DESIGN_COLORS;
import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import com.lorainelab.igb.plugins.repos.events.PluginRepositoryEventPublisher;
import com.lorainelab.igb.plugins.repos.events.ShowBundleRepositoryPanelEvent;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.SwingUtilities;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, provide = AppManagerFxPanel.class)
public class AppManagerFxPanel extends JFXPanel {

    private static final Logger logger = LoggerFactory.getLogger(AppManagerFxPanel.class);
    private final String PLUGIN_INFO_TEMPLATE = "pluginInfoTemplate.html";

    @FXML
    private WebView description;
    @FXML
    private TextField search;
    @FXML
    private ComboBox filterOptions;
    @FXML
    private ListView<PluginListItemMetadata> listView;
    @FXML
    private Button updateAllBtn;
    @FXML
    private Button manageReposBtn;
    private VBox pane;
    private WebEngine webEngine;

    private EventBus eventBus;
    private Map<String, Color> repoToColor;
    private int colorIndex = 0;
    private BundleContext bundleContext;

    private final ObservableList<PluginListItemMetadata> listData;
    private final FilteredList<PluginListItemMetadata> filteredList;

    private Predicate<PluginListItemMetadata> currentStaticPredicate;
    private Predicate<PluginListItemMetadata> currentSearchPredicate;

    private BundleInfoManager bundleInfoManager;
    private BundleActionManager bundleActionManager;
    private RepositoryInfoManager repositoryInfoManager;

    private void refreshUpdateAllBtn() {
        Platform.runLater(() -> {
            updateAllBtn.setDisable(!filteredList.stream().anyMatch(plugin -> plugin.getIsUpdatable().getValue()));
        });
    }

    private void updateAllBundles() {
        listData.stream().filter(plugin -> plugin.getIsUpdatable().getValue()).forEach(plugin -> updatePlugin(plugin));
    }

    @Subscribe
    public void udpateDataEventNotification(UpdateDataEvent event) {
        Platform.runLater(() -> {
            listData.clear();
            if (bundleInfoManager.getRepositoryManagedBundles().isEmpty()) {
                updateWebContent();
                return;
            }
            List<Bundle> toAdd = Lists.newArrayList();
            for (Bundle bundle : bundleInfoManager.getRepositoryManagedBundles()) {
                Optional<Bundle> match = toAdd.stream()
                        .filter(b -> b.getSymbolicName().equals(bundle.getSymbolicName()))
                        .findFirst();
                if (match.isPresent()) {
                    if (bundle.getVersion().compareTo(match.get().getVersion()) == 1) {
                        toAdd.remove(match.get());
                        toAdd.add(bundle);
                    }
                } else {
                    toAdd.add(bundle);
                }
            }

            toAdd.stream().forEach(bundle -> {
                listData.add(new PluginListItemMetadata(bundle, bundleInfoManager.getBundleVersion(bundle), repositoryInfoManager.getBundlesRepositoryName(bundle), bundleInfoManager.isUpdateable(bundle)));
            });
            refreshListViewContent();
        });
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

    public AppManagerFxPanel() {
        listData = FXCollections.observableArrayList((PluginListItemMetadata p) -> new Observable[]{p});
        currentStaticPredicate = (PluginListItemMetadata s) -> true;
        filteredList = new FilteredList<>(listData, s -> true);
        Platform.runLater(() -> {
            init();
        });
    }

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        udpateDataEventNotification(new UpdateDataEvent());
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

    @FXML
    private void initialize() {
        filterOptions.getItems().addAll(FilterOption.ALL_APPS, FilterOption.INSTALLED, FilterOption.UNINSTALLED);
        filterOptions.valueProperty().addListener(new ChangeListener<FilterOption>() {
            @Override
            public void changed(ObservableValue ov, FilterOption t, FilterOption newValue) {
                changeStaticFilter(newValue);
            }
        });
        listView.setItems(filteredList.sorted());
        refreshUpdateAllBtn();
        listView.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends PluginListItemMetadata> observable,
                                PluginListItemMetadata previousSelection,
                                PluginListItemMetadata selectedPlugin) -> {
                    if (selectedPlugin != null) {
                        updateWebContent();
                    }
                });
        listView.setCellFactory((ListView<PluginListItemMetadata> l) -> new BuildCell());
        description.setContextMenuEnabled(false);
        webEngine = description.getEngine();
        JSObject jsobj = (JSObject) webEngine.executeScript("window");
        jsobj.setMember("Bridge", new JSBridge());
        jsobj.setMember("logger", new JSLogger());

        String htmlUrl;
        if (bundleContext != null) {
            htmlUrl = bundleContext.getBundle().getEntry(PLUGIN_INFO_TEMPLATE).toExternalForm();
        } else {
            htmlUrl = AppManagerFxPanel.class.getClassLoader().getResource(PLUGIN_INFO_TEMPLATE).toExternalForm();
        }
        webEngine.load(htmlUrl);

        search.textProperty().addListener((observable, oldValue, newValue) -> {
            changeSearchFilter(newValue);
        });
    }

    @Reference
    public void setEventBus(PluginRepositoryEventPublisher eventManager) {
        this.eventBus = eventManager.getPluginRepositoryEventBus();
        eventBus.register(this);
    }

    @FXML
    private void manageReposBtnAction() {
        SwingUtilities.invokeLater(() -> {
            eventBus.post(new ShowBundleRepositoryPanelEvent());
        });
    }

    private void updateWebContent() {
        Platform.runLater(() -> {
            JSObject jsobj = (JSObject) webEngine.executeScript("window");
            jsobj.setMember("pluginInfo", new JSPluginWrapper(listView, bundleInfoManager));
            try {
                webEngine.executeScript("updatePluginInfo()");
            } catch (JSException ex) {
                logger.debug(ex.getMessage());
            }
        });
    }

    private void init() {
        repoToColor = new HashMap<>();
        final URL resource = AppManagerFxPanel.class.getClassLoader().getResource("PluginConfigurationPanel.fxml");
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        pane = loader.getRoot();
        Platform.runLater(this::createScene);
    }

    private void createScene() {
        Scene scene = new Scene(pane);
        setScene(scene);
    }

    @FXML
    protected void updateAllBtnAction(ActionEvent event) {
        logger.info("updateAllBtnClicked");
        updateAllBundles();
        refreshListViewContent();

    }

    private class BuildCell extends ListCell<PluginListItemMetadata> {

        @Override
        public void updateItem(PluginListItemMetadata plugin, boolean empty) {
            if (plugin == null) {
                return;
            }
            updateItemAction(plugin, empty);
        }

        public void updateItemAction(PluginListItemMetadata plugin, boolean empty) {
            Platform.runLater(() -> {
                super.updateItem(plugin, empty);
                if (!empty) {
                    Image updateImage;
                    ImageView updateImageView = new ImageView();
                    updateImageView.setFitWidth(16);
                    updateImageView.setPreserveRatio(true);
                    updateImageView.setSmooth(true);
                    updateImageView.setCache(true);

                    if (plugin.getIsUpdatable().getValue()) {
                        if (bundleContext != null) {
                            updateImage = new Image(bundleContext.getBundle().getEntry("fa-arrow-circle-up.png").toExternalForm());
                        } else {
                            updateImage = new Image("fa-arrow-circle-up.png");
                        }
                        updateImageView.setImage(updateImage);
                        Tooltip updateTooltip = new Tooltip("Update available");
                        Tooltip.install(updateImageView, updateTooltip);
                    } else if (plugin.getIsInstalled().getValue()) {
                        if (bundleContext != null) {
                            updateImage = new Image(bundleContext.getBundle().getEntry("installed.png").toExternalForm());
                        } else {
                            updateImage = new Image("installed.png");
                        }
                        updateImageView.setImage(updateImage);
                        Tooltip updateTooltip = new Tooltip("Installed");
                        Tooltip.install(updateImageView, updateTooltip);
                    } else {
                        if (bundleContext != null) {
                            updateImage = new Image(bundleContext.getBundle().getEntry("uninstalled.png").toExternalForm());
                        } else {
                            updateImage = new Image("uninstalled.png");
                        }
                        updateImageView.setImage(updateImage);
                        Tooltip updateTooltip = new Tooltip("Uninstalled");
                        Tooltip.install(updateImageView, updateTooltip);
                    }
                    Pane pane = new Pane();
                    pane.setPrefHeight(35);
                    pane.setPrefWidth(35);
                    Color paneColor;
                    if (repoToColor.containsKey(plugin.getRepository().getValue())) {
                        paneColor = repoToColor.get(plugin.getRepository().getValue());
                    } else {
                        if ((colorIndex + 1) > MATERIAL_DESIGN_COLORS.size()) {
                            colorIndex = 0;
                        }
                        paneColor = MATERIAL_DESIGN_COLORS.get(colorIndex);
                        colorIndex++;
                        repoToColor.put(plugin.getRepository().getValue(), paneColor);
                    }

                    Tooltip avatarTooltip = new Tooltip("Located in the " + plugin.getRepository() + " repository");
                    Tooltip.install(pane, avatarTooltip);
                    pane.setBackground(new Background(new BackgroundFill(paneColor, CornerRadii.EMPTY, Insets.EMPTY)));
                    Text avatar = new Text();

                    avatar.setText(plugin.getRepository().getValue().substring(0, 1).toUpperCase());
                    avatar.setFill(Color.rgb(255, 255, 255));
                    pane.getChildren().add(avatar);
                    avatar.setX(8);
                    avatar.setY(27);
                    avatar.setFont(Font.font(27));

                    HBox row = new HBox(5);
                    row.setAlignment(Pos.CENTER_LEFT);

                    String label = plugin.getPluginName().getValue();
                    if (label.length() > 25) {
                        label = label.substring(0, 25) + "...";
                    }
                    Text text = new Text(label);
                    text.setWrappingWidth(200);
                    HBox spacer = new HBox();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    if (updateImageView.getImage() != null) {
                        row.getChildren().addAll(pane, text, spacer, updateImageView);
                    } else {
                        row.getChildren().addAll(pane, text, spacer);
                    }
                    setGraphic(row);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            });
        }
    }

    public class JSBridge {

        public void installPlugin() {
            final PluginListItemMetadata plugin = listView.getSelectionModel().getSelectedItem();
            if (plugin == null) {
                Platform.runLater(() -> {
                    updateWebContent();
                });
                return;
            }
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if (t) {
                    Platform.runLater(() -> {
                        plugin.setIsInstalled(Boolean.TRUE);
                        plugin.setIsBusy(Boolean.FALSE);
                        updateWebContent();
                        listView.getSelectionModel().select(plugin);
                    });
                }
                return Void.TYPE;
            };
            plugin.setIsBusy(Boolean.TRUE);
            updateWebContent();
            bundleActionManager.installBundle(plugin, functionCallback);
        }

        public void handleUnInstallClick() {
            final PluginListItemMetadata plugin = listView.getSelectionModel().getSelectedItem();
            final int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (plugin == null) {
                Platform.runLater(() -> {
                    updateWebContent();
                });
                return;
            }
            final Function<Boolean, ? extends Class<Void>> functionCallback = (Boolean t) -> {
                if (t) {
                    Platform.runLater(() -> {
                        plugin.setIsBusy(Boolean.FALSE);
                        plugin.setIsInstalled(Boolean.FALSE);
                        updateWebContent();
                        listView.getSelectionModel().select(selectedIndex);
                    });
                }
                return Void.TYPE;
            };
            Platform.runLater(() -> {
                plugin.setIsBusy(Boolean.TRUE);
                updateWebContent();
            });
            bundleActionManager.uninstallBundle(plugin, functionCallback);
        }

        public void handleUpdateClick() {
            final PluginListItemMetadata plugin = listView.getSelectionModel().getSelectedItem();
            if (plugin == null) {
                Platform.runLater(() -> {
                    updateWebContent();
                });
                return;
            }
            updatePlugin(plugin);
        }

        public void openWebpage(String uriString) {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                if (StringUtils.isNotBlank(uriString)) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            URI uri = new URI(uriString);
                            desktop.browse(uri);
                        } catch (IOException | URISyntaxException ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    });
                }
            }
        }
    }

    private void updatePlugin(final PluginListItemMetadata plugin) {
        final Function<Boolean, ? extends Class<Void>> handleUpdateCallback = (Boolean b) -> {
            if (b) {
                Platform.runLater(() -> {
                    plugin.setIsBusy(Boolean.FALSE);
                    updateWebContent();
                    listView.getSelectionModel().select(plugin);
                });
            }
            return Void.TYPE;
        };
        Platform.runLater(() -> {
            plugin.setIsBusy(Boolean.TRUE);
            updateWebContent();
        });
        bundleActionManager.updateBundle(plugin, handleUpdateCallback);
    }

    private void refreshListViewContent() {
        listView.setItems(filteredList.sorted());
        refreshUpdateAllBtn();
    }

    public void changeSearchFilter(String filter) {
        Platform.runLater(() -> {
            currentSearchPredicate = (s -> {
                String escapedFilter = Pattern.quote(filter);
                Pattern nameStartWith = Pattern.compile("^" + escapedFilter, Pattern.CASE_INSENSITIVE);
                Pattern nameContains = Pattern.compile(escapedFilter, Pattern.CASE_INSENSITIVE);
                Pattern descContains = Pattern.compile(escapedFilter, Pattern.CASE_INSENSITIVE);
                if (nameStartWith.matcher(s.getPluginName().getValue()).find()) {
                    s.setWeight(10);
                    return true;
                } else if (nameContains.matcher(s.getPluginName().getValue()).find()) {
                    s.setWeight(5);
                    return true;
                } else if (descContains.matcher(s.getDescription().getValue()).find()) {
                    s.setWeight(1);
                    return true;
                }
                s.setWeight(0);
                return false;
            });
            filteredList.setPredicate(currentSearchPredicate.and(currentStaticPredicate));
        });
    }

    void changeStaticFilter(FilterOption filter) {
        Platform.runLater(() -> {
            switch (filter) {
                case ALL_APPS:
                    currentStaticPredicate = (PluginListItemMetadata s) -> true;
                    filteredList.setPredicate(currentStaticPredicate.and(currentSearchPredicate));
                    break;
                case INSTALLED:
                    currentStaticPredicate = (PluginListItemMetadata s) -> s.getIsInstalled().getValue();
                    filteredList.setPredicate(currentStaticPredicate.and(currentSearchPredicate));
                    break;
                case UNINSTALLED:
                    currentStaticPredicate = (PluginListItemMetadata s) -> !s.getIsInstalled().getValue();
                    filteredList.setPredicate(currentStaticPredicate.and(currentSearchPredicate));
                    break;
                default:
                    currentStaticPredicate = (PluginListItemMetadata s) -> true;
                    filteredList.setPredicate(currentStaticPredicate.and(currentSearchPredicate));
                    break;
            };
        });
    }
}
