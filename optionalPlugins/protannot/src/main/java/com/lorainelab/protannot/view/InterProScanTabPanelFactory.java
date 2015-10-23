/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.protannot.InterProScanResultSheet;
import com.lorainelab.protannot.ProtAnnotEventService;
import com.lorainelab.protannot.ProtAnnotService;
import com.lorainelab.protannot.event.StartInterProScanEvent;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author jeckstei
 */
@Component(factory = "interproscan.tab.factory.provider")
public class InterProScanTabPanelFactory implements TabPanelComponent {

    private static final String NAME = "InterProScan";

    private Map<String, Object> properties;
    private ProtAnnotService protannotService;
    private ProtAnnotEventService protAnnotEventService;
    private InterProScanResultSheet tableView;

    @Activate
    public void activate(Map<String, Object> properties) {
        this.properties = properties;
        this.protannotService = (ProtAnnotService) properties.get("protannotService");
        
        tableView = new InterProScanResultSheet();
        tableView.getCancelAllJobs().addActionListener((ActionEvent e) -> {
            Properties props = new Properties();
            props.put("id", properties.get("id"));
            protannotService.cancelBackgroundTasks();
        });
        
        tableView.getRunInterProScan().addActionListener((ActionEvent e) -> {
            protAnnotEventService.getEventBus().post(new StartInterProScanEvent((String)properties.get("id")));
        });
    }

    @Override
    public java.awt.Component getComponent() {
        return tableView;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Reference
    public void setProtAnnotEventService(ProtAnnotEventService protAnnotEventService) {
        this.protAnnotEventService = protAnnotEventService;
    }
    
    
}
