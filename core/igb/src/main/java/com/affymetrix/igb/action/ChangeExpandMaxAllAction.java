package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * Changes the maximum stack depth (max expand) of all the tiers in the map,
 * not just those selected.
 * It does not affect graph tracks.
 * @author blossome
 */
public class ChangeExpandMaxAllAction extends ChangeExpandMaxActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeExpandMaxAllAction ACTION = new ChangeExpandMaxAllAction();

		
	public static ChangeExpandMaxAllAction getAction() {
		return ACTION;
	}

	private ChangeExpandMaxAllAction() {
		super(BUNDLE.getString("changeExpandMaxAllAction"), null, null);
	}

	@Override
	protected List<TierLabelGlyph> getTiers() {
		List<TierLabelGlyph> answer = new ArrayList<TierLabelGlyph>();
		TierLabelManager m = getTierManager();
		List<TierLabelGlyph> allTiers = m.getAllTierLabels();
		if (null == allTiers) {
			return answer;
		}
		for (TierLabelGlyph t : allTiers) {
			TierGlyph tg = t.getReferenceTier();
			if (tg.getAnnotStyle().isGraphTier()) {
				continue;
			}
			answer.add(t);
		}
		return answer;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeExpandMax();
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
