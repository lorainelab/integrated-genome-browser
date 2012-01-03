package com.affymetrix.igb.action;

import com.affymetrix.igb.util.ExportDialogGUI;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import java.awt.event.ActionEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ExportWholeFrameAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ExportWholeFrameAction ACTION = new ExportWholeFrameAction();

	public static ExportWholeFrameAction getAction() {
		return ACTION;
	}

	private ExportWholeFrameAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
		//	ComponentWriter.showExportDialog(IGB.getSingleton().getFrame());
			ExportDialogGUI.getSingleton().showExportDialog(IGB.getSingleton().getFrame());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}

	@Override
	public String getText() {
		return BUNDLE.getString("wholeFrame");
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
