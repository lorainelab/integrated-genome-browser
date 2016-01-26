/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.context.menu;

import java.util.List;
import java.util.Optional;
import org.lorainelab.igb.context.menu.model.AnnotationContextEvent;
import org.lorainelab.igb.context.menu.model.ContextMenuItem;

/**
 * @author dcnorris
 * @module.info context-menu-api
 */
public interface AnnotationContextMenuProvider {

    /**
     *
     * This method will be called when the the annotation
     * context popup menu is being constructed.
     *
     * @param event an event object containing selection info
     * @return a list of ContextMenuItem objects to be added to the annotation
     * context popup menu. It is possible an implementor will chose not to
     * append any ContextMenuItems to the context menu, and in this case should
     * return Optional.empty() or an empty list.
     *
     */
    public Optional<List<ContextMenuItem>> buildMenuItem(AnnotationContextEvent event);

}
