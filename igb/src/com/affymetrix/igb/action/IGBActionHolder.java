package com.affymetrix.igb.action;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.IGBAction;

public class IGBActionHolder {
	private static IGBActionHolder instance = new IGBActionHolder();
	private static final String DEFAULT_ICON_PATH = "toolbarButtonGraphics/general/TipOfTheDay16.gif";
	
	private IGBActionHolder() {
		super();
	}
	
	public static IGBActionHolder getInstance() {
		return instance;
	}

	private List<IGBAction> igbActions = new ArrayList<IGBAction>();

	public void addIGBAction(IGBAction igbAction) {
		igbActions.add(igbAction);
		PreferenceUtils.getAccelerator(igbAction.getText());
		boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(igbAction.getText(), false);
		if (isToolbar) {
			String iconPath = igbAction.getIconPath();
			if (iconPath == null) {
				iconPath = DEFAULT_ICON_PATH;
			}
			ImageIcon icon = MenuUtil.getIcon(iconPath);
			JButton button = new JButton(icon);
			button.addActionListener(igbAction);
			button.setToolTipText(igbAction.getText());
			((IGB)Application.getSingleton()).addToolbarButton(button);
		}
	}

	public void removeIGBAction(IGBAction igbAction) {
		igbActions.remove(igbAction);
	}

	public List<IGBAction> getIGBActions() {
		return igbActions;
	}
}
