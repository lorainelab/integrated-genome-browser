/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.menu.api.util;

import java.awt.event.ActionEvent;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;

/**
 *
 * @author dcnorris
 */
public class MenuUtils {

    public static JMenuItem convertContextMenuItemToJMenuItem(MenuItem menuItem) {
        JMenuItem jMenuItem = new JMenuItem(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menuItem.getAction().apply(null);
            }
        });
        jMenuItem.setText(menuItem.getMenuLabel());
        Optional<MenuIcon> menuItemIcon = menuItem.getMenuIcon();
        if (menuItemIcon.isPresent()) {
            jMenuItem.setIcon(new ImageIcon(menuItemIcon.get().getEncodedImage()));
        }
        jMenuItem.setEnabled(menuItem.isEnabled());
        return jMenuItem;
    }
}
