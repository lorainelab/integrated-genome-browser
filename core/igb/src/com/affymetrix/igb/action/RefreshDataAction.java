package com.affymetrix.igb.action;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.MenuUtil;

import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: RefreshDataAction.java 11329 2012-05-01 17:18:29Z anuj4159 $
 */
public class RefreshDataAction extends GenericAction {
	private static final long serialVersionUID = 1l;

	public RefreshDataAction(JComponent comp) {
		super(BUNDLE.getString("refreshDataButton"), BUNDLE.getString("refreshDataTip"), "toolbarButtonGraphics/general/Refresh16.gif", null, KeyEvent.VK_UNDEFINED);
		KeyStroke ks = MenuUtil.addAccelerator(comp, this, getId());
		if (ks != null) {
			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		super.actionPerformed(ae);
		GeneralLoadView.getLoadView().setShowLoadingConfirm(true);
		GeneralLoadView.getLoadView().loadVisibleFeatures();
	}
}
