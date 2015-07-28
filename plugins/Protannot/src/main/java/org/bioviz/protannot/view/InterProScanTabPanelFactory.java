/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.view;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Properties;
import org.bioviz.protannot.InterProScanResultSheet;
import org.bioviz.protannot.ProtAnnotService;
import org.osgi.service.component.ComponentFactory;

/**
 *
 * @author jeckstei
 */
@Component(factory = "interproscan.tab.factory.provider")
public class InterProScanTabPanelFactory implements TabPanelComponent {

    private static final String NAME = "InterProScan";

    private Map<String, Object> properties;
    private ComponentFactory protannotServiceFactory;

    @Reference(target = "(component.factory=protannot.service.factory.provider)")
    public void setProtannotServiceFactory(ComponentFactory protannotServiceFactory) {
        this.protannotServiceFactory = protannotServiceFactory;
    }

    @Activate
    public void activate(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public java.awt.Component getComponent() {
        InterProScanResultSheet tableView = new InterProScanResultSheet();
        tableView.getCancelAllJobs().addActionListener((ActionEvent e) -> {
            ((ProtAnnotService) protannotServiceFactory.newInstance(new Properties()).getInstance()).cancelBackgroundTasks();
        });

        return tableView;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
