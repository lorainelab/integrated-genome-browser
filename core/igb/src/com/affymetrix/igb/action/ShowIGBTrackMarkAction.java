package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;

/**
 *
 * @author nick
 */
public class ShowIGBTrackMarkAction extends SeqMapViewActionA{

	private static final long serialVersionUID = 1;
	private static final ShowIGBTrackMarkAction ACTION = new ShowIGBTrackMarkAction();

	public static ShowIGBTrackMarkAction getAction() {
		return ACTION;
	}

	private ShowIGBTrackMarkAction() {
		super(BUNDLE.getString("showIGBTrackMark"), null, null);

		this.putValue(SELECTED_KEY, TrackStyle.getShowIGBTrackMarkState());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean b = (Boolean)getValue(SELECTED_KEY);
		TrackStyle.setShowIGBTrackMark(b);

		for (TierLabelGlyph glyph : getTierManager().getAllTierLabels()) {
			glyph.setShowIGBTrack(b);
		}

		((IGB) IGB.getSingleton()).getMapView().getSeqMap().updateWidget();
	}
}
