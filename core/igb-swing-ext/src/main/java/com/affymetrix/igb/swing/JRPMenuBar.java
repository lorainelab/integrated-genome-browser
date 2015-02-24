/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.util.WeightUtil;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

/**
 *
 * @author tarun
 */
public class JRPMenuBar extends JMenuBar {

    private List<WeightedJRPWidget> menuComponents;

    public JRPMenuBar() {
        menuComponents = new ArrayList<>();
    }

    public void add(JMenu newMenu, int index) {

        if (newMenu instanceof WeightedJRPWidget) {
            int loc = WeightUtil.locationToAdd(menuComponents, (WeightedJRPWidget)newMenu);
            super.add(newMenu, loc);
            menuComponents.add(loc, (WeightedJRPWidget)newMenu);
        } else {
            super.add(newMenu, -1);
        }
    }
}
