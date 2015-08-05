/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import java.awt.Dimension;
import java.util.Map;
import com.lorainelab.protannot.ModPropertySheet;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(factory = "properties.tab.factory.provider")
public class PropertiesTabPanelFactory implements TabPanelComponent {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(PropertiesTabPanelFactory.class);
    
    private static final String NAME = "Properties";
    
    private Map<String, Object> properties;
    
    @Activate
    public void activate(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public java.awt.Component getComponent() {
        ModPropertySheet table_view = new ModPropertySheet();
        table_view.setPreferredSize(new Dimension(Integer.parseInt("500"), Integer.parseInt((String)properties.get("table.height"))));
        return table_view;
    }

    @Override
    public String getName() {
        return NAME;
    }
    
}
