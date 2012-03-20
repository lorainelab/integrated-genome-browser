package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ShowMinusStrandAction extends GenericAction {
   	private static final long serialVersionUID = 1L;
	private static final ShowMinusStrandAction ACTION = new ShowMinusStrandAction();

	public static ShowMinusStrandAction getAction() {
		return ACTION;
	}

	private ShowMinusStrandAction() {
		super();
		this.putValue(SELECTED_KEY, AffyTieredMap.isShowMinus());
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap.setShowMinus(!AffyTieredMap.isShowMinus());
		AffyTieredMap map = ((IGB) IGB.getSingleton()).getMapView().getSeqMap();
		map.repackTheTiers(false, true);
	}

	@Override
	public String getText() {
		return "Show (-) tiers";
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
