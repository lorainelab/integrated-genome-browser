package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.EnableDisableAbleAction;
import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.tiers.TierLabelGlyph;

import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.Selections.RefreshSelectionListener;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class ChangeExpandMaxAction extends ChangeExpandMaxActionA implements EnableDisableAbleAction{
	private static final long serialVersionUID = 1L;
	private static final ChangeExpandMaxAction ACTION = new ChangeExpandMaxAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
			
	public static ChangeExpandMaxAction getAction() {
		return ACTION;
	}

	private ChangeExpandMaxAction() {
		super(BUNDLE.getString("changeExpandMaxAction"), "16x16/actions/max_stack_depth.png",
				"22x22/actions/max_stack_depth.png");
	}

	@Override
	protected List<TierLabelGlyph> getTiers() {
		return getTierManager().getSelectedTierLabels();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeExpandMax();
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

	public boolean getEnableDisable() {
		return (Selections.allGlyphs.size() > 0 && Selections.isAllAnnot());
	}

}
