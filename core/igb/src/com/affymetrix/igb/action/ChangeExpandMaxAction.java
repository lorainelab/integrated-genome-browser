package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.tiers.TierLabelGlyph;

import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.Selections.RefreshSelectionListener;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class ChangeExpandMaxAction extends ChangeExpandMaxActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeExpandMaxAction ACTION = new ChangeExpandMaxAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		Selections.addRefreshSelectionListener(ACTION.enabler);
	}
	
	RefreshSelectionListener enabler = new RefreshSelectionListener() {

		@Override
		public void selectionRefreshed() {
			ChangeExpandMaxAction.this.setEnabled(Selections.allGlyphs.size() > 0 && Selections.isAllAnnot());
		}
	};
			
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

	private static class RefreshSelectionListenerImpl implements RefreshSelectionListener {

		public RefreshSelectionListenerImpl() {
		}

		@Override
		public void selectionRefreshed() {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}
}
