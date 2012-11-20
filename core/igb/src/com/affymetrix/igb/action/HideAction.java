package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.shared.RepackTiersAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;

public class HideAction extends RepackTiersAction{
	private static final long serialVersionUID = 1L;
	private static final HideAction ACTION = new HideAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static HideAction getAction() {
		return ACTION;
	}

	private HideAction() {
		super(BUNDLE.getString("hideAction"),
				"16x16/actions/hide.png",
				"22x22/actions/hide.png");
		this.ordinal = -6008400;
	}

	/**
	 * Hides multiple tiers and then repacks.
	 *
	 * @param tiers a List of GlyphI objects for each of which getInfo() returns
	 * a TierGlyph.
	 */
	void hideTiers(List<TierLabelGlyph> tiers) {
		for (TierLabelGlyph g : tiers) {
			if (g.getInfo() instanceof TierGlyph) {
				TierGlyph tier = (TierGlyph) g.getInfo();
				ITrackStyleExtended style = tier.getAnnotStyle();
				if (style != null) {
					style.setShow(false);
					tier.setVisibility(false);
					getSeqMapView().selectTrack(tier, false);
				}
			}
		}

		repack(false, false);

		/**
		 * Possible bug : When all strands are hidden. tier label and tier do
		 * appear at same position.
		 *
		 */
		// NOTE: Below call to stretchToFit is not redundancy. It is there
		//       to solve above mentioned bug.
		repack(false, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		hideTiers(getTierManager().getSelectedTierLabels());
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

	@Override
	public boolean isEnabled(){
		return Selections.allGlyphs.size() > 0;
	}
}
