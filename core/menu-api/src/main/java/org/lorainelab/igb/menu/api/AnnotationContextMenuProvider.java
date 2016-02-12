package org.lorainelab.igb.menu.api;

import java.util.List;
import java.util.Optional;
import org.lorainelab.igb.menu.api.model.AnnotationContextEvent;
import org.lorainelab.igb.menu.api.model.ContextMenuItem;

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
     * @return a list of MenuItem objects to be added to the annotation
     * context popup menu. It is possible an implementor will chose not to
     * append any MenuItems to the context menu, and in this case should
     * return Optional.empty() or an empty list.
     *
     */
    public Optional<List<ContextMenuItem>> buildMenuItem(AnnotationContextEvent event);

}
