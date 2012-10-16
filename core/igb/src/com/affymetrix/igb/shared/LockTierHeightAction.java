package com.affymetrix.igb.shared;

import com.affymetrix.igb.action.SeqMapViewActionA;
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
	
	public LockTierHeightAction() {
		super(BUNDLE.getString("lockTierHeightAction"), null, null);
		this.putValue(SELECTED_KEY, Boolean.FALSE);
	}
	
	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		StyledGlyph[] glyphs = allGlyphs.toArray(new StyledGlyph[0]);
		int len = getTierManager().getVisibleTierGlyphs().size() - 1 == glyphs.length? glyphs.length - 1 : glyphs.length;
		StyledGlyph glyph;
		for(int i = 0; i < len ; i++){
			glyph = glyphs[i];
			if(glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph)glyph).getTierType() == TierType.ANNOTATION){
				DefaultTierGlyph dtg = (DefaultTierGlyph)glyph;
				dtg.setHeightFixed((Boolean)getValue(SELECTED_KEY));
			}
		}
		
		getTierMap().repackTheTiers(true, true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
	
}
