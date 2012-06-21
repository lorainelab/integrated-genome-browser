package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.ParameteredAction;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class TierFontSizeAction extends SeqMapViewActionA implements ParameteredAction {
	private final static TierFontSizeAction ACTION = new TierFontSizeAction();
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static TierFontSizeAction getAction(){
		return ACTION;
	}
	
	protected TierFontSizeAction() {
		super("Label Size", null, null);
	}

	private void setFontSize(int fontsize){
		final List<TierLabelGlyph> tier_label_glyphs = getTierManager().getSelectedTierLabels();
		if (tier_label_glyphs.isEmpty()) {
			return;
		}
		
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null) {
				style.setTrackNameSize(fontsize);
			}
		}

		for (AbstractGraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
			gg.getGraphState().getTierStyle().setTrackNameSize(fontsize);
		}
	}
	
	@Override
	public void performAction(Object parameter) {
		if(parameter.getClass() != Integer.class)
			return;
		
		setFontSize((Integer)parameter);
	}
}
