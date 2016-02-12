package org.lorainelab.igb.menu.api.model;

import java.util.Set;
import java.util.function.Function;

/**
 *
 * @author dcnorris
 */
public class ContextMenuItem extends MenuItem {

    private ContextMenuSection menuSection = ContextMenuSection.APP;

    public ContextMenuItem(String menuLabel, Set<MenuItem> subMenuItems) {
        super(menuLabel, subMenuItems);
    }

    public ContextMenuItem(String menuLabel, Function<Void, Void> action) {
        super(menuLabel, action);
    }

    public void setMenuSection(ContextMenuSection menuSection) {
        this.menuSection = menuSection;
    }

    public ContextMenuSection getMenuSection() {
        return menuSection;
    }

}
