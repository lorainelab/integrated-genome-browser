package com.affymetrix.igb.util;

import com.affymetrix.igb.swing.JRPMenu;
import org.lorainelab.igb.services.window.menus.IgbToolBarParentMenu;

/**
 *
 * @author dcnorris
 */
public interface MainMenuManager {

    JRPMenu getMenu(IgbToolBarParentMenu igbToolBarParentMenu);

}
