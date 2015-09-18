/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.feedback;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class FeedbackMenuProvider extends GenericAction implements IgbMenuItemProvider {

    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private final int menuWeight;
    private final String parentMenuName;
    private final JRPMenuItem menuItem;

    public FeedbackMenuProvider() {
        super(getProperty("menu.name"),
                getProperty("menu.tooltip"),
                "16x16/actions/help.png",
                "22x22/actions/help.png",
                KeyEvent.VK_UNDEFINED);
        menuWeight = Integer.parseInt(getProperty("menu.weight"));
        parentMenuName = getProperty("menu.parent.name");
        menuItem = new JRPMenuItem(getProperty("menu.name"), this, menuWeight);
    }

    @Override
    public String getParentMenuName() {
        return parentMenuName;
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return menuItem;
    }

    @Override
    public int getMenuItemWeight() {
        return menuWeight;
    }

    private static String getProperty(String property) {
        return BUNDLE.getString(property);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }


}
