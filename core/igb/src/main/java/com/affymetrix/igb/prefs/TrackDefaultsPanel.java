/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Component;
import com.lorainelab.igb.service.api.PreferencesPanelProvider;
import javax.swing.JPanel;

@Component(name = TrackDefaultsPanel.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class TrackDefaultsPanel extends TrackPreferencesPanel implements PreferencesPanelProvider {

    public static final String COMPONENT_NAME = "TrackDefaultsPanel";
    private static final long serialVersionUID = 1L;

    //TODO remove this dependency on a singleton
    public TrackDefaultsPanel() {
        super("Track Defaults", TrackDefaultView.getSingleton());
    }

    @Override
    protected void enableSpecificComponents() {
        autoRefreshCheckBox.setVisible(false);
        refreshButton.setVisible(false);
    }

    @Override
    protected void deleteAndRestoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ((TrackDefaultView) tdv).deleteTrackDefaultButton();
    }

    @Override
    protected void selectAndAddButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ((TrackDefaultView) tdv).addTrackDefaultButton();
    }

    @Override
    public int getTabWeight() {
        return 2;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}
