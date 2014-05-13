package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ShowMixedStrandAction extends GenericAction {
   	private static final long serialVersionUID = 1L;
	private static final ShowMixedStrandAction ACTION = new ShowMixedStrandAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ShowMixedStrandAction getAction() {
		return ACTION;
	}

	private ShowMixedStrandAction() {
		super("Show (+/-) Tiers", null, null);
		this.putValue(SELECTED_KEY, AffyTieredMap.isShowMixed());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap.setShowMixed(!AffyTieredMap.isShowMixed());
		AffyTieredMap map = ((IGB) IGB.getSingleton()).getMapView().getSeqMap();
		map.repackTheTiers(false, true);
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
