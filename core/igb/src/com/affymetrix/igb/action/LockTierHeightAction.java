package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import com.affymetrix.igb.shared.StyledGlyph;
import com.affymetrix.igb.shared.TierGlyph.TierType;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;

import static com.affymetrix.igb.shared.Selections.*;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class LockTierHeightAction extends SeqMapViewActionA{
	private static final long serialVersionUID = 1L;
	
	public LockTierHeightAction(boolean lock) {
		super(BUNDLE.getString("lockTierHeightAction"), null, null);
		this.putValue(SELECTED_KEY, lock);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		for(StyledGlyph glyph : allGlyphs){
			if(glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph)glyph).getTierType() == TierType.ANNOTATION){
				DefaultTierGlyph dtg = (DefaultTierGlyph)glyph;
				dtg.setHeightFixed((Boolean)getValue(SELECTED_KEY));
			}
		}
		
		getTierMap().repackTheTiers(true, true);
	}
	
}
