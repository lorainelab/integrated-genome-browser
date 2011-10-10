package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.affymetrix.genometryImpl.event.GenericAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class UnclampViewAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final UnclampViewAction ACTION = new UnclampViewAction();

	public static UnclampViewAction getAction() {
		return ACTION;
	}

	private UnclampViewAction() {
		super();
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_U);
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		//IGB.getSingleton().getMapView().unclamp();
	}

	@Override
	public String getText() {
		return BUNDLE.getString("unclamp");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_U;
	}
}
