package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.event.GroupSelectionEvent;
import javax.swing.JPanel;

/**
 * This is the subclass of TrackPreferencesPanel which is used to represent
 * Preferences->Tracks Tab.
 *
 * @author Anuj
 */
public class TierPreferencesPanel extends TrackPreferencesPanel {

    private static final long serialVersionUID = 1L;
    private static final int TAB_POSITION = 2;

    /**
     * Creates new form TierPreferencesPanel
     */
    public TierPreferencesPanel() {
        super("Tracks", TierPrefsView.getSingleton());
        this.setToolTipText("Set Track Properties");
        validate();
    }

    @Override
    protected void enableSpecificComponents() {
        autoRefreshCheckBox.setVisible(true);
        refreshButton.setVisible(true);
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public int getTabWeight() {
        return TAB_POSITION;
    }

    @Override
    public void refresh() {
        ((TierPrefsView) tdv).refreshList();
    }

    @Override
    public void mapRefresh() {
        if (isVisible()) {
            ((TierPrefsView) tdv).refreshList();
        }
    }

    @Override
    public void groupSelectionChanged(GroupSelectionEvent evt) {
        mapRefresh();
    }

    //<editor-fold defaultstate="collapsed" desc="comment">
    @Override
    protected void deleteAndRestoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
        //</editor-fold>
        ((TierPrefsView) tdv).restoreToDefault();
    }

    @Override
    protected void selectAndAddButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ((TierPrefsView) tdv).selectAll();
    }
}
