package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.TrackView;

public class RemoveDataFromTracksAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static RemoveDataFromTracksAction ACTION;

	public static RemoveDataFromTracksAction getAction() {
		if (ACTION == null) {
			ACTION = new RemoveDataFromTracksAction();
		}
		return ACTION;
	}

	protected RemoveDataFromTracksAction() {
		super(IGBConstants.BUNDLE.getString("deleteAction"), null, "images/eraser.png", KeyEvent.VK_UNDEFINED);
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();

		if (IGB.confirmPanel(MessageFormat.format(IGBConstants.BUNDLE.getString("confirmDelete"), seq.getID()), PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_CLEAR, PreferenceUtils.default_confirm_before_clear)) {
			List<TierLabelGlyph> tiers = getTierManager().getSelectedTierLabels();
			for (TierLabelGlyph tlg : tiers) {
				ITrackStyleExtended style = tlg.getReferenceTier().getAnnotStyle();
				String method = style.getMethodName();
				if (method != null) {
					TrackView.getInstance().delete(getSeqMapView().getSeqMap(), method, style);
				} else {
					for (AbstractGraphGlyph gg : TierLabelManager.getContainedGraphs(tiers)) {
						style = gg.getGraphState().getTierStyle();
						method = style.getMethodName();
						TrackView.getInstance().delete(getSeqMapView().getSeqMap(), method, style);
					}
				}
			}
		}
		getSeqMapView().dataRemoved();	// refresh
	}
}
