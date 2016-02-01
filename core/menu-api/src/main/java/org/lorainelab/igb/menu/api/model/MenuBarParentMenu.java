/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.menu.api.model;

/**
 * ## MenuBarParentMenu
 *
 * This enum represents the parent menus that are available for
 * extension.
 *
 *  * FILE
 *  * EDIT
 *  * VIEW
 *  * TOOLS
 *  * TABS
 *  * HELP
 *
 * <img src="doc-files/toolbarParentMenus.png" alt="ToolBar Parent Menus"/>
 *
 * @author dcnorris
 * @module.info context-menu-api
 */
public enum MenuBarParentMenu {
    FILE("file"), EDIT("edit"), VIEW("view"), TOOLS("tools"), HELP("help");
    private final String name;

    private MenuBarParentMenu(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
