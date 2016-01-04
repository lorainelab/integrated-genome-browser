/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot.menu;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import org.lorainelab.igb.igb.services.window.menus.IgbMenuItemProvider;
import org.lorainelab.igb.igb.services.window.menus.IgbToolBarParentMenu;
import org.lorainelab.igb.protannot.ProtAnnotAction;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import org.osgi.service.component.ComponentFactory;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(immediate = true)
public class ProtAnnotMenuProvider extends GenericAction implements IgbMenuItemProvider {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ProtAnnotMenuProvider.class);
    
    private static final int MENU_ITEM_WEIGHT = 8;
    
    private ComponentFactory protannotFactory;
    
    private final JRPMenuItem menuItem;
    
    public ProtAnnotMenuProvider() {
        super("Start ProtAnnot", null, null);
        menuItem = new JRPMenuItem("Protannot", this, MENU_ITEM_WEIGHT);
    }

    @Override
    public com.affymetrix.igb.swing.JRPMenuItem getMenuItem() {
        return menuItem;
    }

    @Override
    public int getMenuItemWeight() {
        return MENU_ITEM_WEIGHT;
    }

    @Override
    public IgbToolBarParentMenu getParentMenu() {
        return IgbToolBarParentMenu.TOOLS;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Hashtable<String, ?> props = new Hashtable<>();
        ProtAnnotAction instance = (ProtAnnotAction) protannotFactory.newInstance(props).getInstance();
        instance.actionPerformed(null);
    }
    
    @Reference(target = "(component.factory=protannot.factory.provider)")
    public void setProtannotFactory(final ComponentFactory protannotFactory) {
        this.protannotFactory = protannotFactory;
    }
    
    
}
