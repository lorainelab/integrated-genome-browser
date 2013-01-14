package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ShowPlusStrandAction extends GenericAction {
   	private static final long serialVersionUID = 1L;
	private static final ShowPlusStrandAction ACTION = new ShowPlusStrandAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ShowPlusStrandAction getAction() {
		return ACTION;
	}

	private ShowPlusStrandAction() {
		super("Show All (+) Tiers", "16x16/actions/blank_placeholder.png", null);
		this.putValue(SELECTED_KEY, AffyTieredMap.isShowPlus());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap.setShowPlus(!AffyTieredMap.isShowPlus());
		AffyTieredMap map = ((IGB) IGB.getSingleton()).getMapView().getSeqMap();
		map.repackTheTiers(false, true);
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
