/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
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
import javafx.application.Platform;
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
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
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
    private String htmlTemplate;
    private ObservableList<PluginListItemMetadata> fiteredAndSortedList;
    private FilteredList<PluginListItemMetadata> filteredData;
    private EventBus eventBus;
    private static final List<Color> materialDesignColors = ImmutableList.of(
            Color.rgb(156, 39, 176),
            Color.rgb(233, 30, 99),
            Color.rgb(244, 67, 54),
            Color.rgb(33, 150, 243),
            Color.rgb(63, 81, 181),
            Color.rgb(96, 125, 139),
            Color.rgb(255, 87, 34),
            Color.rgb(121, 85, 72),
            Color.rgb(158, 158, 158),
            Color.rgb(255, 235, 59),
            Color.rgb(255, 193, 7),
            Color.rgb(255, 152, 0),
            Color.rgb(76, 175, 80),
            Color.rgb(139, 195, 74),
            Color.rgb(205, 220, 57),
            Color.rgb(3, 169, 244),
            Color.rgb(0, 188, 212),
            Color.rgb(0, 150, 136),
            Color.rgb(103, 58, 183)
    );
    private Map<String, Color> repoToColor;
    private int colorIndex = 0;
    private BundleContext bundleContext;
    private AppController appController;
    private ObservableList<PluginListItemMetadata> listData;

    private void refreshUpdateAllBtn() {
        updateAllBtn.setDisable(!filteredData.stream().anyMatch(plugin -> plugin.isUpdatable()));
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
        fiteredAndSortedList = new FilteredList<>(FXCollections.emptyObservableList());
        Platform.runLater(() -> {
            init();
        });
    }

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.listData = appController.getListData();
        filteredData = new FilteredList<>(listData, s -> true);
        fiteredAndSortedList = appController.getListData();
    }

    @Reference
    public void setController(AppController appController) {
        this.appController = appController;
    }

    private void changeStaticFilter(FilterOption filter) {
        appController.changeStaticFilter(filter);
        refreshListViewContent();
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
        listView.setItems(fiteredAndSortedList);
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
            appController.changeSearchFilter(newValue);
            refreshListViewContent();
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
            jsobj.setMember("pluginInfo", new JSPluginWrapper());
            webEngine.executeScript("updatePluginInfo()");
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
        Scene scene = new Scene(pane, 885, 541);
        setScene(scene);
    }

    @FXML
    protected void updateAllBtnAction(ActionEvent event) {
        logger.info("updateAllBtnClicked");
        appController.updateAllBundles();
        refreshListViewContent();

    }

    private class BuildCell extends ListCell<PluginListItemMetadata> {

        @Override
        public void updateItem(PluginListItemMetadata plugin, boolean empty) {
            Platform.runLater(() -> {
                super.updateItem(plugin, empty);
                if (!empty) {
                    Image updateImage;
                    ImageView updateImageView = new ImageView();
                    updateImageView.setFitWidth(16);
                    updateImageView.setPreserveRatio(true);
                    updateImageView.setSmooth(true);
                    updateImageView.setCache(true);

                    if (plugin.isUpdatable()) {
                        if (bundleContext != null) {
                            updateImage = new Image(bundleContext.getBundle().getEntry("fa-arrow-circle-up.png").toExternalForm());
                        } else {
                            updateImage = new Image("fa-arrow-circle-up.png");
                        }
                        updateImageView.setImage(updateImage);
                        Tooltip updateTooltip = new Tooltip("Update available");
                        Tooltip.install(updateImageView, updateTooltip);
                    } else if (plugin.isInstalled()) {
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
                    if (repoToColor.containsKey(plugin.getRepository())) {
                        paneColor = repoToColor.get(plugin.getRepository());
                    } else {
                        if ((colorIndex + 1) > materialDesignColors.size()) {
                            colorIndex = 0;
                        }
                        paneColor = materialDesignColors.get(colorIndex);
                        colorIndex++;
                        repoToColor.put(plugin.getRepository(), paneColor);
                    }

                    Tooltip avatarTooltip = new Tooltip("Located in the " + plugin.getRepository() + " repository");
                    Tooltip.install(pane, avatarTooltip);
                    pane.setBackground(new Background(new BackgroundFill(paneColor, CornerRadii.EMPTY, Insets.EMPTY)));
                    Text avatar = new Text();

                    avatar.setText(plugin.getRepository().substring(0, 1).toUpperCase());
                    avatar.setFill(Color.rgb(255, 255, 255));
                    pane.getChildren().add(avatar);
                    avatar.setX(8);
                    avatar.setY(27);
                    avatar.setFont(Font.font(27));

                    HBox row = new HBox(5);
                    row.setAlignment(Pos.CENTER_LEFT);

                    String label = plugin.getPluginName();
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

    public class JSLogger {

        public void log(String message) {
            Platform.runLater(() -> {
                logger.info(message);
            });
        }
    }

    public class JSBridge {

        public void installPlugin() {
            final PluginListItemMetadata plugin = listView.getSelectionModel().getSelectedItem();
            appController.installBundle(plugin);
            plugin.setIsInstalled(Boolean.TRUE);
            refreshListViewContent();
        }

        public void handleUnInstallClick() {
            final PluginListItemMetadata plugin = listView.getSelectionModel().getSelectedItem();
            appController.uninstallBundle(plugin);
            plugin.setIsInstalled(Boolean.FALSE);
            refreshListViewContent();
        }

        public void handleUpdateClick() {
            final PluginListItemMetadata plugin = listView.getSelectionModel().getSelectedItem();
            appController.updateBundle(plugin);
            refreshListViewContent();
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

    private void refreshListViewContent() {
        ObservableList<PluginListItemMetadata> t = listView.getItems();
        listView.setItems(t);
        listView.setCellFactory((ListView<PluginListItemMetadata> l) -> new BuildCell());
        refreshUpdateAllBtn();
    }

    public class JSPluginWrapper {

        final PluginListItemMetadata plugin = listView.getSelectionModel().getSelectedItem();

        public String getPluginName() {
            return plugin.getPluginName();
        }

        public String getRepository() {
            return plugin.getRepository();
        }

        public String getVersion() {
            return plugin.getVersion();
        }

        public String getDescription() {
            return plugin.getDescription();
        }

        public Boolean isUpdatable() {
            return plugin.isUpdatable();
        }

        public Boolean isInstalled() {
            return plugin.isInstalled();
        }
    }
}
