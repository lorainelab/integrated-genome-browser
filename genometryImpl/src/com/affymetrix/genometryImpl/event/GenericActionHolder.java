package com.affymetrix.genometryImpl.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

public class GenericActionHolder {
	private static GenericActionHolder instance = new GenericActionHolder();
	private static final String DEFAULT_ICON_PATH = "toolbarButtonGraphics/general/TipOfTheDay16.gif";
	private final List<GenericActionListener> listeners = new ArrayList<GenericActionListener>();
	
	private GenericActionHolder() {
		super();
	}
	
	public static GenericActionHolder getInstance() {
		return instance;
	}

	private Map<String, GenericAction> igbActions = new HashMap<String, GenericAction>();

	public void addIGBAction(GenericAction igbAction) {
		igbActions.put(igbAction.getId(), igbAction);
		if (igbAction.getText() != null) {
			PreferenceUtils.getAccelerator(igbAction.getText());
			boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(igbAction.getText(), false);
			if (isToolbar) {
				String iconPath = igbAction.getIconPath();
				if (iconPath == null) {
					iconPath = DEFAULT_ICON_PATH;
				}
				ImageIcon icon = CommonUtils.getInstance().getIcon(iconPath);
				JButton button = new JButton(icon);
				button.addActionListener(igbAction);
				button.setToolTipText(igbAction.getText());
//				((IGB)Application.getSingleton()).addToolbarButton(button);
			}
		}
	}

	public void removeIGBAction(GenericAction igbAction) {
		igbActions.remove(igbAction);
	}

	public GenericAction getIGBAction(String name) {
		return igbActions.get(name);
	}

	public void addIGBActionListener(GenericActionListener listener) {
		listeners.add(listener);
	}

	public void removeIGBActionListener(GenericActionListener listener) {
		listeners.remove(listener);
	}

	public void notifyActionPerformed(GenericAction action) {
		String id = action.getId();
		for (GenericActionListener listener : listeners) {
			listener.notifyIGBAction(id);
		}
	}
}
