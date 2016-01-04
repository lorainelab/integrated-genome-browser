package org.lorainelab.igb.igb.services.window.preferences;

import com.affymetrix.igb.swing.JRPJPanel;

/**
 *
 * @author dcnorris
 */
public interface PreferencesPanelProvider {

    public String getName();

    //determines position of tab relative to other tabs
    public int getWeight();

    public JRPJPanel getPanel();

    //Mainly for backwards compatibility with old abstract class, but may be useful
    public void refresh();
}
