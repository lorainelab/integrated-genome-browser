package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import java.awt.event.ActionEvent;

/**
 *
 * @author nick
 */
public class ShowIGBTrackMarkAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1;
    private static final ShowIGBTrackMarkAction ACTION = new ShowIGBTrackMarkAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        PreferenceUtils.saveToPreferences(TrackConstants.PREF_SHOW_IGB_TRACK_MARK, TrackConstants.default_show_igb_track_mark, ACTION);
    }

    public static ShowIGBTrackMarkAction getAction() {
        return ACTION;
    }

    private ShowIGBTrackMarkAction() {
        super(BUNDLE.getString("showIGBTrackMark"), "16x16/actions/blank_placeholder.png", null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        boolean b = (Boolean) getValue(SELECTED_KEY);
        IGBStateProvider.setShowIGBTrackMark(b);
        for (TierLabelGlyph glyph : getTierManager().getAllTierLabels()) {
            glyph.setShowIGBTrack(b);
        }
        getSeqMapView().getSeqMap().updateWidget();
    }

    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
