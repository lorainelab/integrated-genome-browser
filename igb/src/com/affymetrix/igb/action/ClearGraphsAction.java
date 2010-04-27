package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ClearGraphsAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ClearGraphsAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("clearGraphs")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_L);
	}

	public void actionPerformed(ActionEvent e) {
		if (IGB.confirmPanel("Really clear graphs?")) {
			IGB.getSingleton().getMapView().clearGraphs();
		}
	}
}
