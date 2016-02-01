/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.menu.api.model;

import java.util.Set;
import java.util.function.Function;

/**
 *
 * @author dcnorris
 */
public class CheckBoxMenuItem extends MenuItem {

    public CheckBoxMenuItem(String menuLabel, Function<Void, Void> action) {
        super(menuLabel, action);
    }

    public CheckBoxMenuItem(String menuLabel, Set<MenuItem> subMenuItems) {
        super(menuLabel, subMenuItems);
    }

}
