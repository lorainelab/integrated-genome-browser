package com.affymetrix.igb.action;

import com.affymetrix.igb.shared.StyledGlyph;
import com.affymetrix.igb.shared.TierGlyph.TierType;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;

import static com.affymetrix.igb.shared.Selections.*;

/**
 *
 * @author hiralv
 */
public abstract class TierHeightAction extends SeqMapViewActionA{
		
	protected TierHeightAction(String name, String smallIcon, String largeIcon) {
		super(name, smallIcon, largeIcon);
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
				setHeightFixed((DefaultTierGlyph)glyph);
			}
		}
		
		getTierMap().repackTheTiers(true, true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
	
	protected abstract void setHeightFixed(DefaultTierGlyph dtg);
}
