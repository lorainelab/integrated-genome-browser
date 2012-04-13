package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class PrintAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final PrintAction ACTION = new PrintAction();

	public static PrintAction getAction() {
		return ACTION;
	}

	private PrintAction() {
		super(BUNDLE.getString("print"), null, "toolbarButtonGraphics/general/Print16.gif", KeyEvent.VK_P, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			IGB.getSingleton().getMapView().getSeqMap().print();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem trying to print.", ex);
		}
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
