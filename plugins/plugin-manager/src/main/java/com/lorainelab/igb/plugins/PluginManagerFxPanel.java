/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.eventbus.EventBus;
import com.google.common.io.CharStreams;
import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import com.lorainelab.igb.plugins.repos.events.PluginRepositoryEventPublisher;
import com.lorainelab.igb.plugins.repos.events.ShowBundleRepositoryPanelEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class PluginManagerFxPanel extends JFXPanel {

    private static final Logger logger = LoggerFactory.getLogger(PluginManagerFxPanel.class);

    @FXML
    private WebView description;
    @FXML
    private TextField search;
    @FXML
    private ComboBox filterOptions;
    @FXML
    private ListView<String> listView;
    @FXML
    private Button updateAllBtn;
    @FXML
    private Button manageReposBtn;

    private VBox pane;
    private WebEngine webEngine;
    private String htmlTemplate;
    private Map<String, PluginListItemMetadata> listData;
    private EventBus eventBus;

    @FXML
    private void initialize() {
        webEngine = description.getEngine();
        webEngine.load(PluginManagerFxPanel.class.getClassLoader().getResource("pluginInfoTemplate.html").toExternalForm());
    }

    public void updateListContent(Map<String, PluginListItemMetadata> list) {
        Platform.runLater(() -> {
            listData = list;
            ObservableList<String> data = FXCollections.observableArrayList(list.keySet());
            listView.setItems((ObservableList<String>) data);
            listView.setOnMouseClicked((MouseEvent event) -> {
                //TODO
            });
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

        });
    }

    public PluginManagerFxPanel() {
        Platform.runLater(() -> {
            init();
        });
    }

    private void init() {
        final URL resource = PluginManagerFxPanel.class.getClassLoader().getResource("PluginConfigurationPanel.fxml");
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
    }

    private static String getClassPathResourceAsString(String resourcePath) {
        try {
            String htmlString = CharStreams.toString(new InputStreamReader(PluginManagerFxPanel.class.getClassLoader().getResourceAsStream(resourcePath)));
            return htmlString;
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return "";
    }

    private PluginListItemMetadata getListItemMeta(String pluginName) {
        return listData.get(pluginName);
    }

    private class BuildCell extends ListCell<String> {

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            PluginListItemMetadata pli = getListItemMeta(item);
            if (!empty) {
                Image image = new Image("plugin.png");
                if (pli.isUpdatable()) {
                    image = new Image("update.png");
                }
                ImageView pluginImage = new ImageView();
                pluginImage.setFitWidth(16);
                pluginImage.setPreserveRatio(true);
                pluginImage.setSmooth(true);
                pluginImage.setCache(true);
                pluginImage.setImage(image);

                HBox row = new HBox(5);

                Text text = new Text(pli.getPluginName());
                HBox spacer = new HBox();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                CheckBox cb = new CheckBox();
                cb.setSelected(pli.isChecked());
                cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    public void changed(ObservableValue ov,
                            Boolean old_val, Boolean new_val) {
                        //TODO
                    }
                });

                row.getChildren().addAll(pluginImage, text, spacer, cb);
                setGraphic(row);
            }
        }
    }

}
