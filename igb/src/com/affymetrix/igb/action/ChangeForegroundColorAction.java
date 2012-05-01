package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;

public class ChangeForegroundColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static ChangeForegroundColorAction ACTION = new ChangeForegroundColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ChangeForegroundColorAction getAction() {
		return ACTION;
	}

	public ChangeForegroundColorAction() {
		super(IGBConstants.BUNDLE.getString("changeColorAction"), "16x16/categories/applications-graphics.png", "22x22/categories/applications-graphics.png");
	}

	@Override
	public java.awt.Color getBackgroundColor(){
		return DEFAULT_COLOR;
	}
	
	@Override
	public java.awt.Color getForegroundColor(){
		if(getTierManager().getSelectedTiers().size() == 1){
			return getTierManager().getSelectedTiers().get(0).getForegroundColor();
		}
		return DEFAULT_COLOR.brighter();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor(getTierManager().getSelectedTierLabels(), true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
