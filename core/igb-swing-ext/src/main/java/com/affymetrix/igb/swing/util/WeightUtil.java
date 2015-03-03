/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.swing.util;

import com.affymetrix.igb.swing.WeightedJRPWidget;
import java.util.List;

/**
 *
 * @author tarun
 */
public class WeightUtil {

    public static int locationToAdd(List<WeightedJRPWidget> menuItems, WeightedJRPWidget newMenuItem) {
        int index = newMenuItem.getWeight();
        if(index == -1) {
            return menuItems.size();
        }
        WeightedJRPWidget prevMenuItem = null;
        for (WeightedJRPWidget menuItem : menuItems) {
            if (menuItem.getWeight() > index) {
                break;
            } else {
                prevMenuItem = menuItem;
            }
        }
        if (prevMenuItem == null) {
            return 0;
        } else {
            return menuItems.indexOf(prevMenuItem) + 1;
        }
    }

}
