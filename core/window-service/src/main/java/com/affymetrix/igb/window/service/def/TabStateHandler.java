package com.affymetrix.igb.window.service.def;

import com.lorainelab.igb.service.api.IgbTabPanel;

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
