/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import com.affymetrix.igb.swing.JRPJPanel;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author jeckstei
 */
@Component(immediate = true, provide = PreferencesPanelProvider.class)
public class PluginConfigurationPanel extends JRPJPanel implements PreferencesPanelProvider {

    private static final int TAB_WEIGHT = 10;
    private static final String TAB_NAME = "Plugins";
    private Text textPanel;

    public PluginConfigurationPanel() {
        super(TAB_NAME);
        setLayout(new MigLayout(new LC().fill().insetsAll("0")));
    }

    @Override
    public int getWeight() {
        return TAB_WEIGHT;
    }

    @Override
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public String getName() {
        return TAB_NAME;
    }

    @Override
    public void refresh() {

    }

    @Activate
    public void activate() {
        JFXPanel headerPanel = new JFXPanel();
        JFXPanel contentPanel = new JFXPanel();
        textPanel = new Text("hello world");
        ListView<String> list = new ListView<>();
        ObservableList<String> data = FXCollections.observableArrayList(
                "plugin1", "plugin12", "plugin3", "plugin4", "plugin5",
                "plugin6", "plugin7", "plugin8", "plugin9", "plugin10",
                "plugin11", "plugin12", "plugin13", "plugin14", "plugin15", "plugin16", "plugin117",
                "plugin18", "plugin19", "plugin20", "plugin21", "plugin22",
                "plugin23", "plugin24", "plugin25", "plugin26", "plugin1", "plugin1", "plugin1",
                "plugin1", "plugin1", "plugin1", "plugin1", "plugin1",
                "plugin1", "plugin1");

        

        HBox header = new HBox(0);
        TextField search = new TextField();
        search.setPromptText("search...");
        header.getChildren().add(search);
        header.setPadding(new Insets(5));

        HBox content = new HBox(0);
        content.getChildren().addAll(list, textPanel);
        
       
        Scene headerScene = new Scene(header);
        headerPanel.setScene(headerScene);

        Scene contentScene = new Scene(content);
        contentPanel.setScene(contentScene);

        list.setItems(data);
        list.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                textPanel.setText("clicked on " + list.getSelectionModel().getSelectedItem());
            }
        });

        list.setCellFactory((ListView<String> l) -> new BuildCell());

        headerPanel.setVisible(true);
        contentPanel.setVisible(true);
        add(headerPanel, "grow, wrap");
        add(contentPanel, "grow, push, span");
    }

    static class BuildCell extends ListCell<String> {

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            Image image = new Image("plugin.png");
            ImageView pluginImage = new ImageView();
            pluginImage.setFitWidth(16);
            pluginImage.setPreserveRatio(true);
            pluginImage.setSmooth(true);
            pluginImage.setCache(true);
            pluginImage.setImage(image);

            HBox row = new HBox(5);

            Text text = new Text(item);
            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            CheckBox cb = new CheckBox();

            row.getChildren().addAll(pluginImage, text, spacer, cb);
            setGraphic(row);
        }
    }


}
