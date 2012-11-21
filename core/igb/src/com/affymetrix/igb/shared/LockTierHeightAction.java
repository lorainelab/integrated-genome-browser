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
public class LockTierHeightAction extends TierHeightAction{
	private static final long serialVersionUID = 1L;
	private final static LockTierHeightAction lockTierAction = new LockTierHeightAction();
			
	public static LockTierHeightAction getAction(){
		return lockTierAction;
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(lockTierAction);
	}
	
	private LockTierHeightAction() {
		super(BUNDLE.getString("lockTierHeightAction"),  "16x16/actions/lock_track.png", "22x22/actions/lock_track.png");
	}

	@Override
	protected void setHeightFixed(DefaultTierGlyph dtg) {
		dtg.setHeightFixed(true);
	}
	
	
	@Override
	public boolean isEnabled(){
		return (!isAllButOneLocked() && isAnyLockable());
	}
}
