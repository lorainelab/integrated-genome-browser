package com.affymetrix.igb.window.service.def;

import org.lorainelab.igb.services.window.tabs.IgbTabPanel;

/**
 * interface for setting tab state
 */
public interface TabStateHandler {

    /**
     * set the given tab pane to its default state
     *
     * @param panel the tab pane
     */
    public void setDefaultState(IgbTabPanel panel);
}
