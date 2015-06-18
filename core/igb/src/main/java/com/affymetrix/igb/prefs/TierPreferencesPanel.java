package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.event.GenomeVersionSelectionEvent;
import com.affymetrix.igb.swing.JRPJPanel;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.lorainelab.igb.services.window.HtmlHelpProvider;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the subclass of TrackPreferencesPanel which is used to represent
 * Preferences->Tracks Tab.
 *
 * @author Anuj
 */
public class TierPreferencesPanel extends TrackPreferencesPanel implements HtmlHelpProvider {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TierPreferencesPanel.class);
    public static final int TAB_POSITION = 5;

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
    public void groupSelectionChanged(GenomeVersionSelectionEvent evt) {
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
        String htmlText = null;
        try {
            htmlText = Resources.toString(TierPreferencesPanel.class.getResource("/help/com.affymetrix.igb.prefs.TierPreferencesPanel.html"), Charsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Help file not found ", ex);
        }
        return htmlText;
    }

    @Override
    public int getWeight() {
        return TAB_POSITION;
    }

}
