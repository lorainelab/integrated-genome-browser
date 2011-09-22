package com.affymetrix.igb.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;

public class IGBActionHolder {
	private static IGBActionHolder instance = new IGBActionHolder();
	private static final String DEFAULT_ICON_PATH = "toolbarButtonGraphics/general/TipOfTheDay16.gif";
	private final List<IGBActionListener> listeners = new ArrayList<IGBActionListener>();
	
	private IGBActionHolder() {
		super();
	}
	
	public static IGBActionHolder getInstance() {
		return instance;
	}

	private Map<String, IGBAction> igbActions = new HashMap<String, IGBAction>();

	public void addIGBAction(IGBAction igbAction) {
		igbActions.put(igbAction.getClass().getSimpleName(), igbAction);
		if (igbAction.getText() != null) {
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
	}

	public void removeIGBAction(IGBAction igbAction) {
		igbActions.remove(igbAction);
	}

	public IGBAction getIGBAction(String name) {
		return igbActions.get(name);
	}

	public void addIGBActionListener(IGBActionListener listener) {
		listeners.add(listener);
	}

	public void removeIGBActionListener(IGBActionListener listener) {
		listeners.remove(listener);
	}

	public void notifyActionPerformed(IGBAction action) {
		String simpleName = action.getClass().getSimpleName();
		for (IGBActionListener listener : listeners) {
			listener.notifyIGBAction(simpleName);
		}
	}
}
