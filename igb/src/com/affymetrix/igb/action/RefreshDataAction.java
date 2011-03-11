package com.affymetrix.igb.action;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import com.affymetrix.genometryImpl.util.MenuUtil;

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
		super(BUNDLE.getString("refreshDataButton"), MenuUtil.getIcon("toolbarButtonGraphics/general/Refresh16.gif"));
		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("refreshDataButton"));
		if (ks != null) {
			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
		}
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString("refreshDataTip"));
	}

	public void actionPerformed(ActionEvent ae) {
		if(ae != null) 
			GeneralLoadView.getLoadView().setShowLoadingConfirm(true);

		GeneralLoadView.getLoadView().loadVisibleFeatures();
	}
}
