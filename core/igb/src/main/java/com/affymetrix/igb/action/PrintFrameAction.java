package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: PrintFrameAction.java 11361 2012-05-02 14:46:42Z anuj4159 $
 */
public class PrintFrameAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final PrintFrameAction ACTION = new PrintFrameAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static PrintFrameAction getAction() {
		return ACTION;
	}

	private PrintFrameAction() {
		super(BUNDLE.getString("printWhole"), null, "16x16/actions/print_whole_frame.png", "22x22/actions/print_whole_frame.png", KeyEvent.VK_P, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		ComponentPagePrinter cprinter = new ComponentPagePrinter(IGB.getSingleton().getFrame());

		try {
			cprinter.print();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem trying to print.", ex);
		}
	}
}
