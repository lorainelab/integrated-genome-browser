/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.window.service.def;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.igb.window.service.IWindowService;
import com.google.common.collect.Sets;
import org.lorainelab.igb.igb.services.window.tabs.IgbTabPanelI;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class IgbTabPanelRegistry {

    private IWindowService windowService;

    private static final Set<IgbTabPanelI> igbTabPanels = Sets.newConcurrentHashSet();

    @Activate
    public void activate() {
        if (!igbTabPanels.isEmpty()) {
            igbTabPanels.stream().forEach(panel -> {
                addTabToWindowService(panel);
            });
            igbTabPanels.clear();
        }
    }

    @Reference(multiple = true, unbind = "removeIgbTabPanel", optional = true, dynamic = true)
    public void addIgbTabPanel(final IgbTabPanelI tabPanel) {
        if (windowService != null) {
            addTabToWindowService(tabPanel);
        } else {
            igbTabPanels.add(tabPanel);
        }
    }

    private synchronized void addTabToWindowService(final IgbTabPanelI tabPanel) {
        windowService.addTab(tabPanel);
    }

    public void removeIgbTabPanel(final IgbTabPanelI tabPanel) {
        if (windowService != null) {
            windowService.removeTab(tabPanel);
        } else {
            igbTabPanels.remove(tabPanel);
        }
    }

    @Reference
    public void setWindowService(IWindowService windowService) {
        this.windowService = windowService;
    }

    public static Set<IgbTabPanelI> getIgbTabPanels() {
        return igbTabPanels;
    }

}
