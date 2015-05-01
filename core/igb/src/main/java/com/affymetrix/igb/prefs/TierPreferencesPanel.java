package com.affymetrix.igb.prefs;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.event.GroupSelectionEvent;
import com.affymetrix.igb.swing.JRPJPanel;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the subclass of TrackPreferencesPanel which is used to represent
 * Preferences->Tracks Tab.
 *
 * @author Anuj
 */
public class TierPreferencesPanel extends TrackPreferencesPanel {

    private static final long serialVersionUID = 1L;
    private static final int TAB_POSITION = 2;
    private static final Logger logger = LoggerFactory.getLogger(TierPreferencesPanel.class);

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
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public int getWeight() {
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

    @Override
    public String getHelpHtml() {
        try (InputStream stream = this.getClass().getResourceAsStream("/help/com.affymetrix.igb.prefs.TierPreferencesPanel.html")) {
            return CommonUtils.getTextFromStream(stream);
        } catch (IOException ex) {
            logger.error("Help file not found ", ex);
        }
        return super.getHelpHtml(); //To change body of generated methods, choose Tools | Templates.
    }
}
