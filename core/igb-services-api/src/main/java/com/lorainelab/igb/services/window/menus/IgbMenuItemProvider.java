package com.lorainelab.igb.services.window.menus;

import javax.swing.JMenuItem;

/**
 *
 * @author dcnorris
 */
public interface IgbMenuItemProvider {

    public String getParentMenuName();

    public JMenuItem getMenuItem();

    public int getMenuItemPosition();
}
