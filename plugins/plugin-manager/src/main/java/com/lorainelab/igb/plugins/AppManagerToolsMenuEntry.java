package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import javafx.application.Platform;

/**
 * A panel for viewing and editing weblinks.
 */
@Component(immediate = true, provide = {IgbMenuItemProvider.class, GenericAction.class})
public final class AppManagerToolsMenuEntry extends GenericAction implements IgbMenuItemProvider {

    private final JRPMenuItem appManagerToolsMenuEntry;
    private static final int MENU_ITEM_WEIGHT = 7;
    private AppManagerFrame frame;

    public AppManagerToolsMenuEntry() {
        super("App Manager",
                "16x16/actions/fa-circle.png",
                "16x16/actions/fa-circle.png");
        appManagerToolsMenuEntry = new JRPMenuItem("App Manager", this, MENU_ITEM_WEIGHT);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        super.actionPerformed(evt);
        Platform.runLater(() -> {
            frame.setVisible(true);
        });
    }

    @Override
    public String getParentMenuName() {
        return "tools";
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return appManagerToolsMenuEntry;
    }

    @Override
    public int getMenuItemWeight() {
        return MENU_ITEM_WEIGHT;
    }

    @Reference
    public void setFxPanel(AppManagerFrame frame) {
        this.frame = frame;
    }
}
