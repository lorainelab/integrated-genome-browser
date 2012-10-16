package com.affymetrix.igb.shared;

import com.affymetrix.igb.action.TierHeightAction;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class UnlockTierHeightAction extends TierHeightAction{
	private static final long serialVersionUID = 1L;
	private final static UnlockTierHeightAction unlockTierAction = new UnlockTierHeightAction();
	
	public static UnlockTierHeightAction getAction(){
		return unlockTierAction;
	}
	
	static{
		Selections.addRefreshSelectionListener(getAction().enabler);
	}
	
	private UnlockTierHeightAction() {
		super(BUNDLE.getString("unlockTierHeightAction"), null, null);
	}

	@Override
	protected void setHeightFixed(DefaultTierGlyph dtg) {
		dtg.setHeightFixed(false);
	}
}
