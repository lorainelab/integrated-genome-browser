package com.lorainelab.igb.services.window.preferences;

import javax.swing.JPanel;

/**
 *
 * @author dcnorris
 */
public interface PreferencesPanelProvider {

    public String getName();

    //determines position of tab relative to other tabs
    public int getTabWeight();

    public JPanel getPanel();

    //Mainly for backwards compatibility with old abstract class, but may be useful
    public void refresh();
}
