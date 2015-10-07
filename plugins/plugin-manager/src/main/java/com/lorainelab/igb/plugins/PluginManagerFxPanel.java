/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class PluginManagerFxPanel extends JFXPanel {

    private static final Logger logger = LoggerFactory.getLogger(PluginManagerFxPanel.class);

    @FXML
    private WebView description;
    @FXML
    private TextField search;
    @FXML
    private ComboBox filterOptions;
    @FXML
    private ListView listView;
    @FXML
    private Button updateAllBtn;
    @FXML
    private Button manageReposBtn;

    private VBox pane;
    private WebEngine webEngine;
    private String htmlTemplate;

    @FXML
    private void initialize() {
        webEngine = description.getEngine();
        htmlTemplate = getClassPathResourceAsString("pluginInfoTemplate.html");
        String html = htmlTemplate.replace("{pluginName}", "ProtAnnot");
        webEngine.loadContent(html);
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
}
