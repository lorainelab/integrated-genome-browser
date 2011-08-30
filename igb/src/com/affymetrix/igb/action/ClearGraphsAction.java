package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
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
public class ClearGraphsAction extends IGBAction {
	private static final long serialVersionUID = 1l;
	private static final ClearGraphsAction ACTION = new ClearGraphsAction();

	public static ClearGraphsAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		if (IGB.confirmPanel("Really clear graphs?")) {
			IGB.getSingleton().getMapView().clearGraphs();
		}
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("clearGraphs"));
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_L;
	}
}
