/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager.menu;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.SwingUtilities;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;
import org.lorainelab.igb.plugin.manager.AppManager;

/**
 *
 * @author jeckstei
 */
@Component(immediate = true)
public class AppManagerMenuProvider implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AppManagerMenuProvider.class);
    private static final int MENU_ITEM_WEIGHT = 8;

    private AppManager frame;

    @Reference
    public void setFxPanel(AppManager frame) {
        this.frame = frame;
    }

    @Override
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem menuItem = new MenuItem(APP_MANAGER_MENU_LABEL, (Void t) -> {
            SwingUtilities.invokeLater(() -> {
                frame.setVisible(true);
            });
            return t;
        });
        try (InputStream resourceAsStream = AppManagerMenuProvider.class.getClassLoader().getResourceAsStream(APP_MANAGER_MENU_ICON)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_ITEM_WEIGHT);
        return Optional.of(Arrays.asList(menuItem));
    }
    private static final String APP_MANAGER_MENU_LABEL = "Open App Manager";
    private static final String APP_MANAGER_MENU_ICON = "fa-circle.png";

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.TOOLS;
    }
}
