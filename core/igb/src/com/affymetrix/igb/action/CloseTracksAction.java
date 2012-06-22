package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.util.List;

public class CloseTracksAction
extends SeqMapViewActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final CloseTracksAction ACTION = new CloseTracksAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		ACTION.setEnabled(false);
		GenometryModel.getGenometryModel().addSymSelectionListener(ACTION);
	}
	
	public static CloseTracksAction getAction() {
		return ACTION;
	}

	protected CloseTracksAction() {
		super(IGBConstants.BUNDLE.getString("closeTracksAction"),
				"16x16/status/user-trash-full.png",
				"22x22/status/user-trash-full.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List<TierLabelGlyph> tiers = getTierManager().getSelectedTierLabels();
		for (TierLabelGlyph tlg : tiers) {
			TierGlyph tg = (TierGlyph)tlg.getInfo();
			GenericFeature gFeature = tg.getAnnotStyle().getFeature();
			if (gFeature != null) {
				GeneralLoadView.getLoadView().removeFeature(gFeature, true);
			}
		}
	}

	/**
	 * Override to enable only when there are tracks to close.
	 */
	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<TierLabelGlyph> tiers = getTierManager().getSelectedTierLabels();
		this.setEnabled(0 < tiers.size());
	}

}
