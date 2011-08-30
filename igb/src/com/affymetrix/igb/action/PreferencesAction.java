package com.affymetrix.igb.action;

import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.IGBAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class PreferencesAction extends IGBAction {
	private static final long serialVersionUID = 1l;
	private static final PreferencesAction ACTION = new PreferencesAction();

	public static PreferencesAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		PreferencesPanel.getSingleton().getFrame().setVisible(true);
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("preferences"));
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Preferences16.gif";
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_E;
	}
}
