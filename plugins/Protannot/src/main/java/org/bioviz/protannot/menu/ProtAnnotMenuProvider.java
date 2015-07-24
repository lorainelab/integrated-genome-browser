/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.menu;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import java.util.Properties;
import org.bioviz.protannot.ProtAnnotAction;
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
    
    public ProtAnnotMenuProvider() {
        super("Start ProtAnnot", null, null);
    }

    @Override
    public com.affymetrix.igb.swing.JRPMenuItem getMenuItem() {
        return new JRPMenuItem("Protannot", this, getMenuItemWeight());
    }

    @Override
    public int getMenuItemWeight() {
        return MENU_ITEM_WEIGHT;
    }

    @Override
    public String getParentMenuName() {
        return "tools";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Properties props = new Properties();
        ProtAnnotAction instance = (ProtAnnotAction) protannotFactory.newInstance(props).getInstance();
        instance.actionPerformed(null);
    }
    
    @Reference(target = "(component.factory=protannot.factory.provider)")
    public void setProtannotFactory(final ComponentFactory protannotFactory) {
        this.protannotFactory = protannotFactory;
    }
    
    
}
