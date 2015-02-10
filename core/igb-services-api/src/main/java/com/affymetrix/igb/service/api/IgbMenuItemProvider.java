package com.affymetrix.igb.service.api;

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
