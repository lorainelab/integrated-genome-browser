/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.menu.api.model;

/**
 * ## ContextMenuSection

 This enum represents the sections of the context menu that are available for
 extension.
 *
 *
 * The order of the sections is as follows:
 *
 *  * INFORMATION
 *  * SEQUENCE
 *  * APP
 *  * UI_ACTION
 *
 * <img src="doc-files/contextMenuSections.png" alt="MenuSection order"/>
 *
 * @author dcnorris
 * @module.info context-menu-api
 */
public enum ContextMenuSection {

    INFORMATION,
    SEQUENCE,
    APP,
    UI_ACTION;
}
