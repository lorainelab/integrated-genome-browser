package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ExitAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ExitAction ACTION = new ExitAction();

	public static ExitAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
				new WindowEvent(
					IGB.getSingleton().getFrame(),
					WindowEvent.WINDOW_CLOSING));
	}

	@Override
	public String getText() {
		return BUNDLE.getString("exit");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_X;
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Stop16.gif";
	}
}
