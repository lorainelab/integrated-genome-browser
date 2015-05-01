/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.igb.swing.JRPJPanel;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = TrackDefaultsPanel.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class TrackDefaultsPanel extends TrackPreferencesPanel implements PreferencesPanelProvider {

    public static final String COMPONENT_NAME = "TrackDefaultsPanel";
    private static final long serialVersionUID = 1L;
    private static final int TAB_POSITION = 1;
    private static final Logger logger = LoggerFactory.getLogger(TrackDefaultsPanel.class);

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
    public int getWeight() {
        return TAB_POSITION;
    }

    @Override
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public String getHelpHtml() {
        try (InputStream stream = this.getClass().getResourceAsStream("/help/com.affymetrix.igb.prefs.TrackDefaultsPanel.html")) {
            return CommonUtils.getTextFromStream(stream);
        } catch (IOException ex) {
            logger.error("Help file not found ", ex);
        }
        return super.getHelpHtml(); //To change body of generated methods, choose Tools | Templates.
    }
}
