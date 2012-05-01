package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;

public class ChangeBackgroundColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static ChangeBackgroundColorAction ACTION = new ChangeBackgroundColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	public static ChangeBackgroundColorAction getAction() {
		return ACTION;
	}

	public ChangeBackgroundColorAction() {
		super(IGBConstants.BUNDLE.getString("changeBGColorAction"), "16x16/categories/applications-graphics.png", "22x22/categories/applications-graphics.png");
	}

	@Override
	public java.awt.Color getColor(){
		if(getTierManager().getSelectedTiers().size() == 1){
			getTierManager().getSelectedTiers().get(0).getAnnotStyle().getBackground();
		}
		return DEFAULT_COLOR;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor(getTierManager().getSelectedTierLabels(), false);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
