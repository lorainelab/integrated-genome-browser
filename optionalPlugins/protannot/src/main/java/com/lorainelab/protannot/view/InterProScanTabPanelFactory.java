/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Properties;
import com.lorainelab.protannot.InterProScanResultSheet;
import com.lorainelab.protannot.ProtAnnotService;

/**
 *
 * @author jeckstei
 */
@Component(factory = "interproscan.tab.factory.provider")
public class InterProScanTabPanelFactory implements TabPanelComponent {

    private static final String NAME = "InterProScan";

    private Map<String, Object> properties;
    private ProtAnnotService protannotService;

    @Activate
    public void activate(Map<String, Object> properties) {
        this.properties = properties;
        this.protannotService = (ProtAnnotService) properties.get("protannotService");
    }

    @Override
    public java.awt.Component getComponent() {
        InterProScanResultSheet tableView = new InterProScanResultSheet();
        tableView.getCancelAllJobs().addActionListener((ActionEvent e) -> {
            Properties props = new Properties();
            props.put("id", properties.get("id"));
            protannotService.cancelBackgroundTasks();
        });

        return tableView;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
