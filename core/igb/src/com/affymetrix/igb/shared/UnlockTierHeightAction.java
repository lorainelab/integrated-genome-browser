package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.action.TierHeightAction;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.shared.Selections.*;
import com.affymetrix.igb.shared.TierGlyph.TierType;

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
	
	protected Selections.RefreshSelectionListener enabler = new Selections.RefreshSelectionListener(){

		@Override
		public void selectionRefreshed() {
			if(isAnyLocked()){
				setEnabled(true);
			}else{
				setEnabled(false);
			}
		}
		
	};
		
	static{
		GenericActionHolder.getInstance().addGenericAction(unlockTierAction);
		Selections.addRefreshSelectionListener(unlockTierAction.enabler);
	}
	
	private UnlockTierHeightAction() {
		super(BUNDLE.getString("unlockTierHeightAction"), "16x16/actions/unlock_track.png", "22x22/actions/unlock_track.png");
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		for(StyledGlyph glyph : allGlyphs){
			if(glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph)glyph).getTierType() == TierType.ANNOTATION){
				setHeightFixed((DefaultTierGlyph)glyph);
			}
		}
		
		getTierMap().repackTheTiers(true, true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
	
	@Override
	protected void setHeightFixed(DefaultTierGlyph dtg) {
		dtg.setHeightFixed(false);
	}
}
