/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.swing;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

/**
 *
 * @author tarun
 */
public class JRPMenuBar extends JMenuBar {

    List<JRPMenu> menuComponents;

    public JRPMenuBar() {
        menuComponents = new ArrayList<>();
    }

    public void add(JMenu newMenu, int index) {

        if (newMenu instanceof JRPMenu) {
            JMenu prevMenu = null;
            for (JRPMenu menu : menuComponents) {
                if (menu.getIndex() > index) {
                    break;
                } else {
                    prevMenu = menu;
                }
            }
            if (prevMenu == null) {
                super.add(newMenu, 0);
            } else {
                super.add(newMenu, menuComponents.indexOf(prevMenu) + 1);
            }
            menuComponents.add(menuComponents.indexOf(prevMenu) + 1, (JRPMenu)newMenu);
        } else {
            super.add(newMenu, -1);
        }
        
        
    }

}
