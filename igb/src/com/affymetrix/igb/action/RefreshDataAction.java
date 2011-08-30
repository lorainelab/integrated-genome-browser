package com.affymetrix.igb.action;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.affymetrix.genoviz.swing.MenuUtil;

import com.affymetrix.igb.shared.IGBAction;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class RefreshDataAction extends IGBAction {
	private static final long serialVersionUID = 1l;

	public RefreshDataAction(JComponent comp) {
		super();
		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("refreshDataButton"));
		if (ks != null) {
			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
		}
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString("refreshDataTip"));
	}

	public void actionPerformed(ActionEvent ae) {
		GeneralLoadView.getLoadView().loadVisibleFeatures();
	}

	@Override
	public String getText() {
		return BUNDLE.getString("refreshDataButton");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Refresh16.gif";
	}

}
