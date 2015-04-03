/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.google.common.base.Strings;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import java.awt.event.ActionEvent;
import java.net.URI;
import static javax.swing.Action.SELECTED_KEY;

/**
 *
 * @author tkanapar
 */
public class ShowFullFilePathInTrack extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final ShowFullFilePathInTrack ACTION = new ShowFullFilePathInTrack();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        PreferenceUtils.saveToPreferences(TrackConstants.PREF_SHOW_FULL_FILE_PATH_IN_TRACK, TrackConstants.default_show_full_file_path_in_track, ACTION);
    }

    public static ShowFullFilePathInTrack getAction() {
        return ACTION;
    }

    private ShowFullFilePathInTrack() {
        super(BUNDLE.getString("showFullFilePathInTrack"), "16x16/actions/blank_placeholder.png", null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        boolean b = (Boolean) getValue(SELECTED_KEY);
        IGBStateProvider.setShowFullFilePathInTrackMark(b);

        for (TierGlyph glyph : getTierManager().getAllTierGlyphs(true)) {
            if (glyph.getAnnotStyle() instanceof TrackStyle
                    && glyph.getAnnotStyle().getFeature() != null) {
                if (b) {
                    URI uri = glyph.getAnnotStyle().getFeature().getURI();
                    if (uri != null) {
                        ((TrackStyle) glyph.getAnnotStyle()).resetTrackName(uri.getPath());
                    }
                } else {
                    String track_name = glyph.getAnnotStyle().getFeature().getFeatureName();
                    if (!Strings.isNullOrEmpty(track_name)) {
                        ((TrackStyle) glyph.getAnnotStyle()).resetTrackName(track_name);
                    }

                }
            }
        }
        getSeqMapView().getSeqMap().updateWidget();
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    public boolean isToggle() {
        return true;
    }
}
