package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: PrintAction.java 11361 2012-05-02 14:46:42Z anuj4159 $
 */
public class PrintAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final PrintAction ACTION = new PrintAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static PrintAction getAction() {
		return ACTION;
	}

	private PrintAction() {
		super(BUNDLE.getString("print"), null, "16x16/devices/printer.png", "22x22/devices/printer.png", KeyEvent.VK_P, null, true);
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
}
