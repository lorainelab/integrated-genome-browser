/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager.menu;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import java.awt.event.ActionEvent;
import javafx.application.Platform;
import org.lorainelab.igb.plugin.manager.AppManagerFrame;
import org.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import org.lorainelab.igb.services.window.menus.IgbToolBarParentMenu;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(immediate = true)
public class AppManagerMenuProvider extends GenericAction implements IgbMenuItemProvider {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AppManagerMenuProvider.class);

    private static final int MENU_ITEM_WEIGHT = 8;

    private final JRPMenuItem menuItem;

    private AppManagerFrame frame;

    public AppManagerMenuProvider() {
        super("Open App Manager", null, null);
        menuItem = new JRPMenuItem("App Manager", this, MENU_ITEM_WEIGHT);
    }

    @Override
    public IgbToolBarParentMenu getParentMenu() {
        return IgbToolBarParentMenu.TOOLS;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> {
            frame.setVisible(true);
        });
    }

    @Override
    public com.affymetrix.igb.swing.JRPMenuItem getMenuItem() {
        return menuItem;
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
