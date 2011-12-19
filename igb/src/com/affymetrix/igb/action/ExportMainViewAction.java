package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.ComponentWriter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ExportMainViewAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ExportMainViewAction ACTION = new ExportMainViewAction();

	public static ExportMainViewAction getAction() {
		return ACTION;
	}

	private ExportMainViewAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			ComponentWriter.showExportDialog(IGB.getSingleton().getMapView().getSeqMap().getNeoCanvas());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}

	@Override
	public String getText() {
		return BUNDLE.getString("mainView");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_M;
	}

	@Override
	public boolean usePrefixInMenu() {
		return true;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
