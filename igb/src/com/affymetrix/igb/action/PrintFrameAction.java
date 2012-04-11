package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
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
public class PrintFrameAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final PrintFrameAction ACTION = new PrintFrameAction();

	public static PrintFrameAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		ComponentPagePrinter cprinter = new ComponentPagePrinter(IGB.getSingleton().getFrame());

		try {
			cprinter.print();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem trying to print.", ex);
		}
	}

	@Override
	public String getText() {
		return BUNDLE.getString("printWhole");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Print16.gif";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_P;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
