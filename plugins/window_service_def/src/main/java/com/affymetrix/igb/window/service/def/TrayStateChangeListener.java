package com.affymetrix.igb.window.service.def;

/**
 * standard listener for being notified when there is a change to the state of a
 * tray
 */
public interface TrayStateChangeListener {

    /**
     * notification that there was a change to a tray state
     *
     * @param trayPane the pane that changed
     * @param trayState the new state of the pane
     */
    public void trayStateChanged(JTabbedTrayPane trayPane, JTabbedTrayPane.TrayState trayState);
}
