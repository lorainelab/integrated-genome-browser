package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.AbstractAction;

/**
 *
 * @author nick
 */
public class ShowIGBTrackMarkAction extends SeqMapViewActionA implements PreferenceChangeListener{

	private static final long serialVersionUID = 1;
	private static final ShowIGBTrackMarkAction ACTION = new ShowIGBTrackMarkAction();

	public static ShowIGBTrackMarkAction getAction() {
		return ACTION;
	}

	private ShowIGBTrackMarkAction() {
		super(BUNDLE.getString("showIGBTrackMark"), "16x16/actions/blank_placeholder.png", null);
		this.putValue(SELECTED_KEY, IGBStateProvider.getShowIGBTrackMarkState());
		this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				PreferenceUtils.SHOW_IGB_TRACKMARK_OPTION, PreferenceUtils.default_show_igb_track));
		PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean b = (Boolean)getValue(SELECTED_KEY);
		IGBStateProvider.setShowIGBTrackMark(b);
		ACTION.putValue(AbstractAction.SELECTED_KEY, IGBStateProvider.getShowIGBTrackMarkState());
		PreferenceUtils.getTopNode().putBoolean(
				PreferenceUtils.SHOW_IGB_TRACKMARK_OPTION, (Boolean)getValue(SELECTED_KEY));
		for (TierLabelGlyph glyph : getTierManager().getAllTierLabels()) {
			glyph.setShowIGBTrack(b);
		}

		((IGB) IGB.getSingleton()).getMapView().getSeqMap().updateWidget();
	}

	public void preferenceChange(PreferenceChangeEvent pce) {
		if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }
		if (pce.getKey().equals(PreferenceUtils.SHOW_IGB_TRACKMARK_OPTION)) {
			this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				PreferenceUtils.SHOW_IGB_TRACKMARK_OPTION, PreferenceUtils.default_show_igb_track));
			IGBStateProvider.setShowIGBTrackMark((Boolean)(this.getValue(SELECTED_KEY)));
			for (TierLabelGlyph glyph : getTierManager().getAllTierLabels()) {
				glyph.setShowIGBTrack((Boolean)(this.getValue(SELECTED_KEY)));
			}
			((IGB) IGB.getSingleton()).getMapView().getSeqMap().updateWidget();
        }
	}
}
