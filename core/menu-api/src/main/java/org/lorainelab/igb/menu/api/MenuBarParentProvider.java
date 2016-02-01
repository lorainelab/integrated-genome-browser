/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.menu.api;

import org.lorainelab.igb.menu.api.model.MenuItem;

/**
 * This interface allows Apps to provide top level extensions to the IGB menubar.
 * These "parent" menu entries will not be extensible by other apps.
 * @module.info context-menu-api
 * @author dcnorris
 */
public interface MenuBarParentProvider {

    /**
     * @return The MenuItem that will be added to the IGB menubar. 
     */
    public MenuItem getParentMenuItem();

    /**
     * Weight
     * =====
     *
     * The weight property specifies the sorting of MenuItems.
     * A greater weight is always below an element with a lower weight.
     *
     * @return menu weight
     */
    public int getWeight();
}
