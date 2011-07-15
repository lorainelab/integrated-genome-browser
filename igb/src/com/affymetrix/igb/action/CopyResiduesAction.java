package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import com.affymetrix.genoviz.swing.MenuUtil;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class CopyResiduesAction extends AbstractAction {

	private static final long serialVersionUID = 1l;

	public CopyResiduesAction() {
		this(BUNDLE.getString("copySelectedResiduesToClipboard"));
	}

	public CopyResiduesAction(String text) {
		this(text, MenuUtil.getIcon("toolbarButtonGraphics/general/Copy16.gif"));
	}

	public CopyResiduesAction(String text, Icon icon){
		super(text, icon);
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
	}

	public void actionPerformed(ActionEvent e) {
		IGB.getSingleton().getMapView().copySelectedResidues(false);

	}
}
