/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.view;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import java.util.Map;
import org.bioviz.protannot.InterProScanResultSheet;

/**
 *
 * @author jeckstei
 */
@Component(factory = "interproscan.tab.factory.provider")
public class InterProScanTabPanelFactory implements TabPanelComponent {
    private static final String NAME = "InterProScan";
    
    private Map<String, Object> properties;
    
    @Activate
    public void activate(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public java.awt.Component getComponent() {
        InterProScanResultSheet tableView = new InterProScanResultSheet();
        return tableView;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
