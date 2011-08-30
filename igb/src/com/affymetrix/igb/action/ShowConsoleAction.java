package com.affymetrix.igb.action;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.IGBAction;
import com.affymetrix.genometryImpl.util.ConsoleView;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ShowConsoleAction extends IGBAction {
	private static final long serialVersionUID = 1l;
	private static final ShowConsoleAction ACTION = new ShowConsoleAction();

	public ShowConsoleAction() {
		super();
	}

	public static ShowConsoleAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		ConsoleView.showConsole(IGBConstants.APP_NAME);
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("showConsole"));
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/development/Host16.gif";
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_C;
	}
}
