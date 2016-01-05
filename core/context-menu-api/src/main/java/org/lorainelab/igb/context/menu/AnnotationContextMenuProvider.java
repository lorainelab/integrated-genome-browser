/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.context.menu;

import org.lorainelab.igb.context.menu.model.AnnotationContextEvent;
import org.lorainelab.igb.context.menu.model.ContextMenuItem;
import java.util.Optional;


/**
 *
 * @author dcnorris
 */
public interface AnnotationContextMenuProvider {

    public Optional<ContextMenuItem> buildMenuItem(AnnotationContextEvent event);

    public MenuSection getMenuSection();

    public static enum MenuSection {
        INFORMATION, SEQUENCE, APP, UI_ACTION;
    }

}
