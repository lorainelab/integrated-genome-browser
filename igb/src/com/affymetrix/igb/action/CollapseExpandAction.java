package com.affymetrix.igb.action;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ImageIcon;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class CollapseExpandAction extends CollapseExpandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static CollapseExpandAction ACTION;

	public static CollapseExpandAction getAction() {
		if (ACTION == null) {
			ACTION = new CollapseExpandAction();
		}
		return ACTION;
	}

	protected CollapseExpandAction() {
		super(IGBConstants.BUNDLE.getString("collapseAction"), "images/collapse.png");
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
		collapsedTracks = !hasCollapsed || hasExpanded;
		String text = collapsedTracks ? IGBConstants.BUNDLE.getString("collapseAction") : IGBConstants.BUNDLE.getString("expandAction") ;
		putValue(Action.NAME, text);
		putValue(SHORT_DESCRIPTION, text);
		String iconPath = collapsedTracks ? "images/collapse.png" : "images/expand.png";
		ImageIcon icon = CommonUtils.getInstance().getIcon(iconPath);
		if (icon == null) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "icon " + iconPath + " returned null");
		}
		else {
			putValue(Action.SMALL_ICON, icon);
		}
	}
}
