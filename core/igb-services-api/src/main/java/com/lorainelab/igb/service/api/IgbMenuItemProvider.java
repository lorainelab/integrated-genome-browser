package com.lorainelab.igb.service.api;

import com.affymetrix.igb.swing.JRPMenuItem;
import javax.swing.JMenuItem;

/**
 *
 * @author dcnorris
 */
public interface IgbMenuItemProvider {

    public String getParentMenuName();

    public JRPMenuItem getMenuItem();

    public int getMenuItemWeight();
}
