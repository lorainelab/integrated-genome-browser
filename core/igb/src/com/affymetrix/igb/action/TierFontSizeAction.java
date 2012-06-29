package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.ParameteredAction;
import com.affymetrix.igb.shared.TierGlyph;

/**
 *
 * @author hiralv
 */
public class TierFontSizeAction extends SeqMapViewActionA implements ParameteredAction {
	private static final long serialVersionUID = 1L;
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
		for (TierGlyph tier : getTierManager().getSelectedTiers()) {
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null) {
				style.setTrackNameSize(fontsize);
			}
		}
	}
	
	@Override
	public void performAction(Object parameter) {
		if(parameter.getClass() != Integer.class)
			return;
		
		setFontSize((Integer)parameter);
	}
}
