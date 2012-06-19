package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.tiers.TierLabelGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;

public class ChangeExpandMaxAllAction extends ChangeExpandMaxActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeExpandMaxAllAction ACTION = new ChangeExpandMaxAllAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ChangeExpandMaxAllAction getAction() {
		return ACTION;
	}

	private ChangeExpandMaxAllAction() {
		super(BUNDLE.getString("changeExpandMaxAllAction"), null, null);
	}

	@Override
	protected List<TierLabelGlyph> getTiers() {
		return getTierManager().getAllTierLabels();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeExpandMax();
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
