/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.menu.api.model;

/**
 *
 * @author dcnorris
 */
public class MenuBarMenuItemEvent {

    private final MenuItem menuItem;
    private final MenuBarParentMenu menuBarParentMenu;

    public MenuBarMenuItemEvent(MenuItem menuItem, MenuBarParentMenu menuBarParentMenu) {
        this.menuItem = menuItem;
        this.menuBarParentMenu = menuBarParentMenu;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public MenuBarParentMenu getMenuBarParentMenu() {
        return menuBarParentMenu;
    }

//    public static class Builder {
//
//        private MenuItem menuItem;
//        private MenuBarParentMenu menuBarParentMenu;
//
//        public Builder() {
//        }
//
//        public Builder withMenuBarParentMenu(MenuBarParentMenu menuBarParentMenu) {
//            this.menuBarParentMenu = menuBarParentMenu;
//            return this;
//        }
//
//        public Builder withMenuItem(MenuItem menuItem) {
//            this.menuItem = menuItem;
//            return this;
//        }
//
//        public MenuBarMenuItemEvent build() {
//            return new MenuBarMenuItemEvent(this);
//        }
//
//    }

}
