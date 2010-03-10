package com.affymetrix.igb.action;

import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class RefreshDataAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	private static final RefreshDataAction singleton = new RefreshDataAction();

	private RefreshDataAction() {
		super(BUNDLE.getString("refreshDataButton"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_R);
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString("refreshDataTip"));
	}

	public static RefreshDataAction getAction() {
		return singleton;
	}

	public void actionPerformed(ActionEvent ae) {
		GeneralLoadView.getLoadView().loadVisibleData();
	}
}
