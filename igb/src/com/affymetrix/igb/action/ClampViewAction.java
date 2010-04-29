package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ClampViewAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ClampViewAction() {
		super(BUNDLE.getString("clampToView"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_V);
	}

	public void actionPerformed(ActionEvent e) {
		IGB.getSingleton().getMapView().clampToView();
	}

}
