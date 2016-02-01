/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot.menu;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.protannot.ProtAnnotAction;
import org.osgi.service.component.ComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

/**
 *
 * @author jeckstei
 */
@Component(immediate = true)
public class ProtAnnotMenuProvider implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ProtAnnotMenuProvider.class);
    private static final String PROTANNOT_MENU_ITEM_LABEL = "Start ProtAnnot";
    private static final int MENU_ITEM_WEIGHT = 8;
    private ComponentFactory protannotFactory;

    @Reference(target = "(component.factory=protannot.factory.provider)")
    public void setProtannotFactory(final ComponentFactory protannotFactory) {
        this.protannotFactory = protannotFactory;
    }

    @Override
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem newGenomeMenuItem = new MenuItem(PROTANNOT_MENU_ITEM_LABEL, (Void t) -> {
            final Hashtable<String, ?> props = new Hashtable<>();
            ProtAnnotAction instance = (ProtAnnotAction) protannotFactory.newInstance(props).getInstance();
            instance.actionPerformed(null);
            return t;
        });
        newGenomeMenuItem.setWeight(MENU_ITEM_WEIGHT);
        return Optional.of(Arrays.asList(newGenomeMenuItem));
    }

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.FILE;
    }
}
