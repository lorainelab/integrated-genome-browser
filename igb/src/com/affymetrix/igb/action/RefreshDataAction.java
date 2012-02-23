package com.affymetrix.igb.action;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.MenuUtil;

import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class RefreshDataAction extends GenericAction {
	private static final long serialVersionUID = 1l;

	public RefreshDataAction(JComponent comp) {
		super();
		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("refreshDataButton"));
		if (ks != null) {
			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
		}
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString("refreshDataTip"));
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		super.actionPerformed(ae);
		GeneralLoadView.getLoadView().setShowLoadingConfirm(true);
		GeneralLoadView.getLoadView().loadVisibleFeatures();
	}

	@Override
	public String getText() {
		return BUNDLE.getString("refreshDataButton");
	}
}
