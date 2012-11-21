package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.action.TierHeightAction;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.shared.Selections.*;

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
		GenericActionHolder.getInstance().addGenericAction(unlockTierAction);
	}
	
	private UnlockTierHeightAction() {
		super(BUNDLE.getString("unlockTierHeightAction"), "16x16/actions/unlock_track.png", "22x22/actions/unlock_track.png");
	}

	@Override
	protected void setHeightFixed(DefaultTierGlyph dtg) {
		dtg.setHeightFixed(false);
	}
		
	@Override
	public boolean isEnabled(){
		return isAnyLocked();
	}
}
