package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Change the max slot depth on all selected tracks to an optimal value.
 */
public class ChangeExpandMaxOptimizeAction extends ChangeExpandMaxActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeExpandMaxOptimizeAction ACTION
			= new ChangeExpandMaxOptimizeAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ChangeExpandMaxOptimizeAction getAction() {
		return ACTION;
	}

	private ChangeExpandMaxOptimizeAction() {
		super(BUNDLE.getString("changeExpandMaxOptimizeAction"), null, null);
		putValue(SHORT_DESCRIPTION, BUNDLE.getString("changeExpandMaxOptimizeActionTooltip"));
	}

	@Override
	protected List<TierLabelGlyph> getTiers() {
		return getTierManager().getSelectedTierLabels();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeExpandMax(getOptimum());
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
