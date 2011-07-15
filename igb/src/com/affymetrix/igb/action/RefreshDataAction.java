package com.affymetrix.igb.action;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.affymetrix.genometryImpl.event.TierMaintenanceListenerHolder;
import com.affymetrix.genoviz.swing.MenuUtil;

import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class RefreshDataAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public RefreshDataAction(JComponent comp) {
		super(BUNDLE.getString("refreshDataButton"));
		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("refreshDataButton"));
		if (ks != null) {
			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
		}
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString("refreshDataTip"));
	}

	public void actionPerformed(ActionEvent ae) {
		GeneralLoadView.getLoadView().loadVisibleFeatures();
		TierMaintenanceListenerHolder.getInstance().fireDataRefreshed();
	}
}
