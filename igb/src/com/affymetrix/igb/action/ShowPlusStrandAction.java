package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ShowPlusStrandAction extends GenericAction {
   	private static final long serialVersionUID = 1L;
	private static final ShowPlusStrandAction ACTION = new ShowPlusStrandAction();

	public static ShowPlusStrandAction getAction() {
		return ACTION;
	}

	private ShowPlusStrandAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap.setShowPlus(!AffyTieredMap.isShowPlus());
		putValue(AffyTieredMap.SELECTED_KEY_, Boolean.valueOf(AffyTieredMap.isShowPlus()));
		AffyTieredMap map = ((IGB) IGB.getSingleton()).getMapView().getSeqMap();
		map.repackTheTiers(false, true, false);
	}

	@Override
	public String getText() {
		return "Show (+) tiers";
	}
}
