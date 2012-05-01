package com.affymetrix.igb.action;

import com.affymetrix.igb.shared.RepackTiersAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;

public class ShowAllAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static final ShowAllAction ACTION = new ShowAllAction();

	public static ShowAllAction getAction() {
		return ACTION;
	}

	private ShowAllAction() {
		super(BUNDLE.getString("showAllAction"), null, null);
	}

	public void showAllTiers() {
		List<TierLabelGlyph> tiervec = getTierManager().getAllTierLabels();

		for (TierLabelGlyph label : tiervec) {
			TierGlyph tier = (TierGlyph) label.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null) {
				style.setShow(true);
				tier.setVisibility(true);
			}
		}
		getTierManager().sortTiers();
		repack(false);
		//refreshMap(false, true); // when re-showing all tier, do strech_to_fit in the y-direction
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		showAllTiers();
	}
}
