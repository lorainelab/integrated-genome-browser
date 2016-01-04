package org.lorainelab.igb.igb.services.window.menus;

import com.affymetrix.igb.swing.JRPMenuItem;

/**
 *
 * @author dcnorris
 */
public interface IgbMenuItemProvider {

    public IgbToolBarParentMenu getParentMenu();

    public JRPMenuItem getMenuItem();

    public int getMenuItemWeight();
}
