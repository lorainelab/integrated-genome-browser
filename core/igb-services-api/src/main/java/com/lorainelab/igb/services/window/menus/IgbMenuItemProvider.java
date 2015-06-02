package com.lorainelab.igb.services.window.menus;

import com.affymetrix.igb.swing.JRPMenuItem;
import java.util.Optional;

/**
 *
 * @author dcnorris
 */
public interface IgbMenuItemProvider {

    public String getParentMenuName();

    public JRPMenuItem getMenuItem();

    public int getMenuItemWeight();
    
    public default Optional<String> getSubMenuName() {
        return Optional.empty();
    }
}
