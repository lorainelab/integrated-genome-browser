/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.igb.services.window.menus;

/**
 *
 * @author dcnorris
 */
public enum IgbToolBarParentMenu {
    FILE("file"), EDIT("edit"), VIEW("view"), TOOLS("tools"), TABS("tabs"), HELP("help");
    private final String name;

    private IgbToolBarParentMenu(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
