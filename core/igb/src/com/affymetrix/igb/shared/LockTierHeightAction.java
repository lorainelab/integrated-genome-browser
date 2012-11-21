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
			
	private Selections.RefreshSelectionListener enabler = new Selections.RefreshSelectionListener(){

		@Override
		public void selectionRefreshed() {
			if((!isAllButOneLocked() && isAnyLockable())){
				setEnabled(true);
			}else{
				setEnabled(false);
			}
		}
		
	};
		
	public static LockTierHeightAction getAction(){
		return lockTierAction;
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(lockTierAction);
		Selections.addRefreshSelectionListener(lockTierAction.enabler);
	}
	
	private LockTierHeightAction() {
		super(BUNDLE.getString("lockTierHeightAction"),  "16x16/actions/lock_track.png", "22x22/actions/lock_track.png");
	}

	@Override
	protected void setHeightFixed(DefaultTierGlyph dtg) {
		dtg.setHeightFixed(true);
	}
}
