package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import com.affymetrix.genometryImpl.event.GenericAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
@SuppressWarnings("serial")
public abstract class CopyResiduesAction extends GenericAction {
	private static final CopyResiduesAction ACTION = new CopyResiduesAction() {
		@Override public String getText() { return BUNDLE.getString("copySelectedResiduesToClipboard"); }
	};
	private static final CopyResiduesAction ACTION_SHORT = new CopyResiduesAction() {
		@Override public String getText() { return "Copy"; }
	};

	public static CopyResiduesAction getAction() {
		return ACTION;
	}

	public static CopyResiduesAction getActionShort() {
		return ACTION_SHORT;
	}

	private CopyResiduesAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		IGB.getSingleton().getMapView().copySelectedResidues(false);
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Copy16.gif";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_C;
	}
}
