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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(immediate = true, provide = PreferencesPanelProvider.class)
public class PluginConfigurationPanel extends JRPJPanel implements PreferencesPanelProvider {

    private static final Logger logger = LoggerFactory.getLogger(PluginConfigurationPanel.class);
    private static final int TAB_WEIGHT = 10;
    private static final String TAB_NAME = "Plugins";
    private PluginManagerFxPanel fxPanel;

    public PluginConfigurationPanel() {
        super(TAB_NAME);
        setLayout(new MigLayout("fill"));
        fxPanel = new PluginManagerFxPanel();
        add(fxPanel, "grow");
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
